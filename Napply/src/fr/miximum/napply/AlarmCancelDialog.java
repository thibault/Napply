
package fr.miximum.napply;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.PowerManager;

/**
 * This class has a simple purpose: displaying a dialog to dismiss or snooze the alarm
 * The dialog result is sent back to the alarm manager via an intent
 */
public class AlarmCancelDialog extends Activity {

    /** Dismiss or snooze dialog id */
    private static final int DIALOG_ID = 1;

    /** If we roll the handset past this angle, we consider it a snooze action */
    private static final int ROLL_ANGLE_BEFORE_SNOOZE = 170;

    /** Handler to unlock the screen */
    private KeyguardLock mKeyguardLock = null;

    /** Handler to turn the screen on and bright */
    private PowerManager.WakeLock mWakeLock = null;

    /** The id of the AppWidget which called us */
    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    /** Handler to the sensor system service */
    private SensorManager mSensorManager;

    /**
     * If the alarm stops itself, it should be able to tell us to finish
     */
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            int appWidgetId = NapplyWidget.getAppWidgetId(intent);

            // We only terminate if we are the dialog associated with the current alarm
            if (Napply.ALARM_TERMINATED.equals(intent.getAction()) && mAppWidgetId == appWidgetId) {
                finish();
            }
        }
    };

    /** Sensor listener, to detect handset rolling */
    private SensorEventListener mSensorListener = new SensorEventListener() {

        private float[] mGravity = null;
        private float[] mGeomagnetic = null;
        private double mPreviousRoll = ROLL_ANGLE_BEFORE_SNOOZE;

        /** If the handset faces toward the floor, snooze the alarm */
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                mGravity = event.values.clone();
            }
            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                mGeomagnetic = event.values.clone();
            }

            if (mGravity != null && mGeomagnetic != null) {
                float R[] = new float[9];
                float I[] = new float[9];
                if (SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic)) {
                    float orientation[] = new float[3];

                    SensorManager.getOrientation(R, orientation);
                    double roll = Math.abs(Math.toDegrees(orientation[2]));

                    // Don't snooze if the handset is already upside down
                    if (mPreviousRoll < ROLL_ANGLE_BEFORE_SNOOZE &&
                            roll > ROLL_ANGLE_BEFORE_SNOOZE) {
                        snoozeAlarm(getApplicationContext());
                        finish();
                    }
                    mPreviousRoll = roll;
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get AppWidget id from launching intent
        mAppWidgetId = NapplyWidget.getAppWidgetId(getIntent());

        // Register orientation sensor
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);

        // We cannot continue without a valid app widget id
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }

        // Register terminate message receiver
        IntentFilter filter = new IntentFilter(Napply.ALARM_TERMINATED);
        registerReceiver(mReceiver, filter);

        // Handler to the lock object
        KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        mKeyguardLock = km.newKeyguardLock(Napply.TAG);

        // Handler to the power manager, to turn the screen on
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, Napply.TAG);

        ringAlarm(getApplicationContext());
        showDialog(DIALOG_ID);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    @Override
    public void onResume() {
        // Disable the lock and turn the screen on
        mWakeLock.acquire();
        mKeyguardLock.disableKeyguard();

        mSensorManager.registerListener(mSensorListener,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(mSensorListener,
                mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_NORMAL);

        super.onResume();
    }

    @Override
    public void onPause() {
        // Reenable screen lock, and release the power lock
        mKeyguardLock.reenableKeyguard();
        mWakeLock.release();

        mSensorManager.unregisterListener(mSensorListener);

        super.onPause();
    }

    /**
     * Create the dialog
     */
    protected Dialog onCreateDialog(int id) {
        Dialog dialog;
        Context context = getApplicationContext();
        switch (id) {
            case DIALOG_ID:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(context.getString(R.string.wake_up))
                        .setCancelable(false)
                        .setPositiveButton(context.getString(R.string.snooze), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                snoozeAlarm(getApplicationContext());
                                finish();
                            }
                        })
                        .setNegativeButton(context.getString(R.string.dismiss), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dismissAlarm(getApplicationContext());
                                finish();
                            }
                        });
                AlertDialog alert = builder.create();
                dialog = alert;
                break;

            default:
                dialog = null;
        }
        return dialog;
    }

    /**
     * Ask the service to ring the alarm
     */
    private void ringAlarm(Context context) {
        Intent ring = new Intent(context, AlarmService.class);
        ring.setAction(Napply.ACTION_RING_ALARM);
        ring.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        startService(ring);
    }

    /**
     * Ask the service to snooze the alarm
     */
    private void snoozeAlarm(Context context) {
        Intent snooze = new Intent(context, AlarmService.class);
        snooze.setAction(Napply.ACTION_SNOOZE_ALARM);
        snooze.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        startService(snooze);
    }

    /**
     * Send a dismiss alarm intent to the alarm service
     */
    private void dismissAlarm(Context context) {
        Intent dismiss = new Intent(context, AlarmService.class);
        dismiss.setAction(Napply.ACTION_CANCEL_ALARM);
        dismiss.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        startService(dismiss);
    }
}

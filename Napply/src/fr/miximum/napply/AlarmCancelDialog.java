
package fr.miximum.napply;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

/**
 * This class has a simple purpose: displaying a dialog to dismiss or snooze the alarm
 * The dialog result is sent back to the alarm manager via an intent
 */
public class AlarmCancelDialog extends Activity {

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

        // Set activity layout
        setContentView(R.layout.alarm_cancel_dialog_layout);

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

        setClickHandlers();

        ringAlarm(getApplicationContext());
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    /** Acquire needed resources (keyguard, wakelock, sensors) */
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

    /** Release resources */
    @Override
    public void onPause() {
        // Reenable screen lock, and release the power lock
        mKeyguardLock.reenableKeyguard();
        mWakeLock.release();

        mSensorManager.unregisterListener(mSensorListener);

        super.onPause();
    }

    /**
     * Configure button event handlers
     */
    protected void setClickHandlers() {
        // Set snooze click handler
        Button snooze = (Button) findViewById(R.id.snooze_alarm);
        snooze.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                snoozeAlarm(getApplicationContext());
                finish();
            }
        });

        // Set dismiss click handler
        Button dismiss = (Button) findViewById(R.id.dismiss_alarm);
        dismiss.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dismissAlarm(getApplicationContext());
                finish();
            }
        });
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

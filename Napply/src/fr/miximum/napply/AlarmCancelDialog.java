
package fr.miximum.napply;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.PowerManager;

/**
 * This class has a simple purpose: displaying a dialog to dismiss or snooze the alarm
 * The dialog result is sent back to the alarm manager via an intent
 */
public class AlarmCancelDialog extends Activity {

    /** Dismiss or snooze dialog id */
    private static final int DIALOG_ID = 1;

    /** Handler to unlock the screen */
    private KeyguardLock mKeyguardLock = null;

    /** Handler to turn the screen on and bright */
    private PowerManager.WakeLock mWakeLock = null;

    /**
     * If the alarm stops itself, it should be able to tell us to finish
     */
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Napply.ACTION_DESTROY_DIALOG.equals(intent.getAction())) {
                finish();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Register terminate message receiver
        IntentFilter filter = new IntentFilter(Napply.ACTION_DESTROY_DIALOG);
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
    public void onResume() {
        // Disable the lock and turn the screen on
        mWakeLock.acquire();
        mKeyguardLock.disableKeyguard();

        super.onResume();
    }

    @Override
    public void onPause() {
        // Reenable screen lock, and release the power lock
        mKeyguardLock.reenableKeyguard();
        mWakeLock.release();

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
        startService(ring);
    }

    private void snoozeAlarm(Context context) {

    }

    /**
     * Send a dismiss alarm intent to the alarm service
     */
    private void dismissAlarm(Context context) {
        Intent dismiss = new Intent(context, AlarmService.class);
        dismiss.setAction(Napply.ACTION_CANCEL_ALARM);
        startService(dismiss);
    }
}


package fr.miximum.napply;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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

    private PowerManager.WakeLock mWakeLock = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Handler to the lock object
        KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        mKeyguardLock = km.newKeyguardLock(Napply.TAG);

        // Handler to the power manager, to turn the screen on
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, Napply.TAG);

        // Show the dialog
        showDialog(DIALOG_ID);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Disable the lock and turn the screen on
        mWakeLock.acquire();
        mKeyguardLock.disableKeyguard();
    }

    @Override
    public void onPause() {
        super.onPause();

        // Reenable screen lock, and release the power lock
        mKeyguardLock.reenableKeyguard();
        mWakeLock.release();
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
                                snoozeAlarm();
                                finish();
                            }
                        })
                        .setNegativeButton(context.getString(R.string.dismiss), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dismissAlarm();
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

    private void snoozeAlarm() {

    }

    /**
     * Send a dismiss alarm intent to the alarm service
     */
    private void dismissAlarm() {
        Context context = getApplicationContext();
        Intent intent = new Intent(context, NapplyAlarm.class);
        intent.setAction(Napply.ACTION_CANCEL_ALARM);
        startService(intent);
    }
}

package fr.miximum.napply;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.Vibrator;
import android.util.Log;

import java.io.IOException;

public class AlarmService extends Service {

    /** Pattern for the alarm vibration */
    private static final long[] sVibratePattern = new long[] { 500, 500 };

    /**
     * Delay before we auto-kill the alarm.
     * Thus it won't ring for hours if user is gone
     */
    private static final int AUTO_KILL_TIMEOUT = 60 * 1000;

    private static final String AUTOKILL_EXTRA = "autokill";

    private MediaPlayer mMediaPlayer = null;
    private Vibrator mVibrator = null;
    private boolean isAlarmRunning = false;

    public int onStartCommand(Intent intent, int flags, int startId) {

        if (Napply.ACTION_RING_ALARM.equals(intent.getAction())) {
            isAlarmRunning = true;
            setupAutokillAlarm();
            ring();
        }
        else if (Napply.ACTION_CANCEL_ALARM.equals(intent.getAction()) && isAlarmRunning)
        {
            isAlarmRunning = false;
            stop();

            // Was the alarm terminated by the autokill feature?
            Bundle extras = intent.getExtras();
            boolean isAutokill = extras != null && extras.getBoolean(AUTOKILL_EXTRA, false);
            if (isAutokill) {
                // If autokill, we need to destroy the dialog alert
                destroyDialog();
            }
            else {
                // Else, cancel the alarm
                cancelAutokillAlarm();
            }
            stopSelf();
        }

        return START_STICKY;
    }

    /**
     * Terminate the dialog activity
     */
    private void destroyDialog() {
        Intent intent = new Intent(Napply.ACTION_DESTROY_DIALOG);
        sendBroadcast(intent);
    }

    /**
     * Play alarm sound and vibrate handset
     */
    private void ring() {
        mVibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        mVibrator.vibrate(sVibratePattern, 0);

        Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        mMediaPlayer = new MediaPlayer();
        try {
            mMediaPlayer.setDataSource(this, alert);
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            mMediaPlayer.setLooping(true);
            mMediaPlayer.prepare();
            mMediaPlayer.start();
        } catch (IOException ioe) {
            Log.e(Napply.TAG, "Cannot play alarm");
        }
    }

    /**
     * Stop the alarm
     */
    private void stop() {
        mVibrator.cancel();
        mMediaPlayer.stop();
    }

    /**
     * We setup a timer to prevent the alarm to run for hours
     */
    private void setupAutokillAlarm() {

        Intent intent = new Intent(this, AlarmService.class);
        intent.setAction(Napply.ACTION_CANCEL_ALARM);
        intent.putExtra(AUTOKILL_EXTRA, true);
        PendingIntent pi = PendingIntent.getService(this, 0, intent, 0);

        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + AUTO_KILL_TIMEOUT, pi);
    }

    /**
     * Cancel the upcoming autokill alarm
     */
    private void cancelAutokillAlarm() {
        Intent intent = new Intent(this, AlarmService.class);
        intent.setAction(Napply.ACTION_CANCEL_ALARM);
        intent.putExtra(AUTOKILL_EXTRA, true);
        PendingIntent pi = PendingIntent.getService(this, 0, intent, 0);

        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        am.cancel(pi);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

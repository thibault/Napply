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
import android.os.IBinder;
import android.os.SystemClock;
import android.os.Vibrator;
import android.util.Log;

import java.io.IOException;

public class NapplyAlarm extends Service {

    /** Pattern for the alarm vibration */
    private static final long[] sVibratePattern = new long[] { 500, 500 };

    /**
     * Delay before we auto-kill the alarm.
     * Thus it won't ring for hours if user is gone
     */
    private static final int AUTO_KILL_TIMEOUT = 60 * 1000;

    private MediaPlayer mMediaPlayer = null;
    private Vibrator mVibrator = null;

    public int onStartCommand(Intent intent, int flags, int startId) {

        if (Napply.ACTION_RING_ALARM.equals(intent.getAction())) {
            setupAutokillAlarm();
            ring();
        }
        else if (Napply.ACTION_CANCEL_ALARM.equals(intent.getAction()))
        {
            cancelAutokillAlarm();
            stop();
        }

        return START_NOT_STICKY;
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
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(this, NapplyAlarm.class);
        intent.setAction(Napply.ACTION_CANCEL_ALARM);
        PendingIntent pi = PendingIntent.getService(this, 0, intent, 0);

        am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + AUTO_KILL_TIMEOUT, pi);
    }

    private void cancelAutokillAlarm() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

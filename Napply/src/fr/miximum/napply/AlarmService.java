package fr.miximum.napply;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
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

public class AlarmService extends Service {

    /** Pattern for the alarm vibration */
    private static final long[] sVibratePattern = new long[] { 500, 500 };

    /**
     * Delay before we auto-kill the alarm.
     * Thus it won't ring for hours if user is gone
     */
    private static final int AUTO_KILL_TIMEOUT = 60 * 1000;

    private static final String AUTOKILL_EXTRA = "autokill";

    /**
     * Delay to wait for the snooze feature
     */
    private static final int SNOOZE_DELAY = 7 * 60 * 1000;

    private MediaPlayer mMediaPlayer = null;
    private Vibrator mVibrator = null;
    private boolean isAlarmRunning = false;

    public int onStartCommand(Intent intent, int flags, int startId) {

        int appWidgetId = NapplyWidget.getAppWidgetId(intent);

        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {

            if (Napply.ACTION_RING_ALARM.equals(intent.getAction())) {
                setupAutokillAlarm(appWidgetId);
                ring();
            }
            else if (Napply.ACTION_CANCEL_ALARM.equals(intent.getAction()) && isAlarmRunning)
            {
                stop();
                sendAlarmTerminatedIntent(appWidgetId);
                cancelAutokillAlarm(appWidgetId);
                stopSelf();
            }
            else if (Napply.ACTION_SNOOZE_ALARM.equals(intent.getAction()) && isAlarmRunning)
            {
                stop();
                cancelAutokillAlarm(appWidgetId);
                scheduleSnooze(appWidgetId);
            }
        }

        return START_STICKY;
    }

    /**
     * Send broadcast intent to indicate that alarm is terminated
     * @param appWidgetId The id of the appWidget that initiated
     */
    private void sendAlarmTerminatedIntent(int appWidgetId) {
        Intent intent = new Intent(Napply.ALARM_TERMINATED);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        Log.e(Napply.TAG, "Send broadcast terminated intent");
        sendBroadcast(intent);
    }

    /**
     * Play alarm sound and vibrate handset
     */
    private void ring() {
        isAlarmRunning = true;
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
        isAlarmRunning = false;
        mVibrator.cancel();
        mMediaPlayer.stop();
    }

    /**
     * Schedule to relaunch alarm in a few minutes
     * @param appWidgetId The id of the appWidget that initiated the alarm
     */
    private void scheduleSnooze(int appWidgetId) {

        Intent intent = new Intent(this, AlarmCancelDialog.class);
        intent.setAction(Napply.ACTION_RING_ALARM);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent pi = PendingIntent.getActivity(this, appWidgetId, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager am = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + SNOOZE_DELAY, pi);
    }

    /**
     * We setup a timer to prevent the alarm to run for hours
     * @param appWidgetId The id of the appWidget that initiated the alarm
     */
    private void setupAutokillAlarm(int appWidgetId) {

        Intent intent = new Intent(this, AlarmService.class);
        intent.setAction(Napply.ACTION_CANCEL_ALARM);
        intent.putExtra(AUTOKILL_EXTRA, true);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent pi = PendingIntent.getService(this, appWidgetId, intent, 0);

        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + AUTO_KILL_TIMEOUT, pi);
    }

    /**
     * Cancel the upcoming autokill alarm
     * @param appWidgetId The id of the appWidget that initiated the alarm
     */
    private void cancelAutokillAlarm(int appWidgetId) {
        Intent intent = new Intent(this, AlarmService.class);
        intent.setAction(Napply.ACTION_CANCEL_ALARM);
        intent.putExtra(AUTOKILL_EXTRA, true);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent pi = PendingIntent.getService(this, appWidgetId, intent, 0);

        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        am.cancel(pi);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

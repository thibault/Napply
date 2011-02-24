package fr.miximum.napply;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;

import java.io.IOException;

public class NapplyAlarm extends Service {

    private static final long[] sVibratePattern = new long[] { 500, 500 };

    private MediaPlayer mMediaPlayer = null;
    private Vibrator mVibrator = null;

    public int onStartCommand(Intent intent, int flags, int startId) {

        if (Napply.ACTION_RING_ALARM.equals(intent.getAction())) {

            showCancelDialog();
            ring();
        }
        else if (Napply.ACTION_CANCEL_ALARM.equals(intent.getAction()))
        {
            Log.e(Napply.TAG, "Cancel alarm");
            cancel();
        }

        return START_NOT_STICKY;
    }

    private void showCancelDialog() {
        Intent intent = new Intent(this, AlarmCancelDialog.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

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

    private void cancel() {
        mVibrator.cancel();
        mMediaPlayer.stop();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}

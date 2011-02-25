package fr.miximum.napply;

import android.app.Activity;
import android.os.Bundle;

public class Napply extends Activity
{
    public static final String TAG = "Napply";

    public static final String ACTION_START_ALARM = "fr.miximum.napply.START_ALARM";

    public static final String ACTION_RING_ALARM = "fr.miximum.napply.RING_ALARM";

    public static final String ACTION_CANCEL_ALARM = "fr.miximum.napply.CANCEL_ALARM";

    public static final String ACTION_SNOOZE_ALARM = "fr.miximum.napply.SNOOZE_ALARM";

    public static final String ALARM_TERMINATED = "fr.miximum.napply.ALARM_TERMINATED";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }
}

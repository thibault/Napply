/*
 * © Copyright 2011 Thibault Jouannic <thibault@jouannic.fr>. All Rights Reserved.
 *
 *  This file is part of Napply.
 *
 *  Napply is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Napply is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Napply. If not, see <http://www.gnu.org/licenses/>.
 */

package fr.miximum.napply;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class NapplyWidget extends AppWidgetProvider {

    private static final String PREFS_NAME = "fr.miximum.napply";

    private static final String PREF_DURATION_PREFIX = "nap_duration_";

    private static final String PREF_IS_RUNNING_PREFIX = "is_nap_running_";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final int N = appWidgetIds.length;

        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i=0; i<N; i++) {
            int appWidgetId = appWidgetIds[i];
            updateAppWidget(context, appWidgetManager, appWidgetId, Napply.ACTION_START_ALARM);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        int appWidgetId = getAppWidgetId(intent);

        // Only if we have a valid appWidgetId
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {

            if (Napply.ACTION_START_ALARM.equals(intent.getAction())) {
                int duration = startAlarm(context, appWidgetId);
                showNewNapToast(context, duration);
                setAlarmRunning(context, true, appWidgetId);

                updateAppWidget(context, AppWidgetManager.getInstance(context), appWidgetId, formatAlarmTime(duration));
            }
            else if (Napply.ACTION_CANCEL_ALARM.equals(intent.getAction())) {
                stopAlarm(context, appWidgetId);
                showCanceledToast(context);
                setAlarmRunning(context, false, appWidgetId);

                updateAppWidget(context, AppWidgetManager.getInstance(context), appWidgetId);
            }
            else if (Napply.ALARM_TERMINATED.equals(intent.getAction())) {
                setAlarmRunning(context, false, appWidgetId);
                updateAppWidget(context, AppWidgetManager.getInstance(context), appWidgetId);
            }
        }
    }

    /**
     * Get AppWidget id from the intent extras
     * @param intent
     * @return The appWidgetId, or link AppWidgetManager.IINVALID_APPWIDGET_ID if none is provided
     */
    public static int getAppWidgetId(Intent intent) {
        // Get appWidget id
        int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
        Bundle extras = intent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        return appWidgetId;
    }

    /**
     * Start the alarm for the given widget
     * @param context
     * @param appWidgetId
     * @return the delay before alarm rings
     */
    private int startAlarm(Context context, int appWidgetId) {
        int napDuration = 60 * 1000 * getNapDuration(context, appWidgetId);

        Intent intent = new Intent(context, AlarmCancelDialog.class);
        intent.setAction(Napply.ACTION_RING_ALARM);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pi = PendingIntent.getActivity(context, appWidgetId, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pi);
        am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + napDuration, pi);

        return napDuration;
    }

    /**
     * Stop the alarm for given widget
     * @param context
     * @param appWidgetId
     */
    private void stopAlarm(Context context, int appWidgetId) {

        // Prepare the same intant as for launching alarm
        Intent intent = new Intent(context, AlarmCancelDialog.class);
        intent.setAction(Napply.ACTION_RING_ALARM);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent pi = PendingIntent.getActivity(context, appWidgetId, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        // Cancel the alarm
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pi);
    }

    /**
     * Display notification message when alarm started
     * @param context
     * @param napDuration duration in milliseconds
     */
    private void showNewNapToast(Context context, int napDuration) {

        CharSequence message = context.getString(R.string.toast_alarm_started, formatAlarmTime(napDuration));
        int duration = Toast.LENGTH_LONG;
        Toast toast = Toast.makeText(context, message, duration);
        toast.show();
    }

    /**
     * Display notification message when an alarm is canceled
     * @param context
     */
    private void showCanceledToast(Context context) {
        int duration = Toast.LENGTH_LONG;
        CharSequence message = context.getString(R.string.toast_alarm_canceled);
        Toast toast = Toast.makeText(context, message, duration);
        toast.show();
    }

    /**
     * Get the string of the alarm ring time
     * @param napDuration
     * @return
     */
    private String formatAlarmTime(int napDuration) {
        Calendar c = new GregorianCalendar();
        c.setTime(new Date());
        c.add(Calendar.MILLISECOND, napDuration);
        return DateFormat.getTimeInstance(DateFormat.SHORT).format(c.getTime());
    }

    /**
     * Save the configured nap duration in shared preferences
     * @param appWidgetId The widget id
     * @param napDuration The duration in minutes
     */
    static void setNapDuration(Context context, int appWidgetId, int napDuration) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putInt(PREF_DURATION_PREFIX + appWidgetId, napDuration);
        prefs.commit();
    }

    /**
     * get the saved nap duration from shared preferences
     * @param appWidgetId The widget id
     * @return The nap duration for the given appWidget id
     */
    static int getNapDuration(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        int duration = prefs.getInt(PREF_DURATION_PREFIX + appWidgetId, 0);

        return duration;
    }

    /**
     * Save a preference to remember if a widget has currently a running alarm
     * @param context
     * @param isRunning Is the alarm running?
     * @param appWidgetId For the given widget id
     */
    static void setAlarmRunning(Context context, boolean isRunning, int appWidgetId) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putBoolean(PREF_IS_RUNNING_PREFIX + appWidgetId, isRunning);
        prefs.commit();
    }

    /**
     * Tells if the alarm for given widget is currently pending
     * @param context
     * @param appWidgetId
     * @return A boolean, is an alarm is pending?
     */
    static boolean isAlarmRunning(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        boolean isRunning = prefs.getBoolean(PREF_IS_RUNNING_PREFIX + appWidgetId, false);

        return isRunning;
    }

    /**
     * Update the widget
     * @param context
     * @param appWidgetManager
     * @param appWidgetId
     */
    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, String napLabel) {

        // Prepare widget views
        int napDuration = getNapDuration(context, appWidgetId);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.napply_widget_layout);
        views.setTextViewText(R.id.nap_time, "" + napDuration);
        views.setTextViewText(R.id.nap_end, napLabel);

        // Prepare intent to launch alarm on widget click
        Intent intent = new Intent(context, NapplyWidget.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

        // The action on the widget click changes if an alarm is running
        if (isAlarmRunning(context, appWidgetId)) {
            intent.setAction(Napply.ACTION_CANCEL_ALARM);
        }
        else {
            intent.setAction(Napply.ACTION_START_ALARM);
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.napply_widget, pendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    /**
     * Update the widget. Use the default nap label
     * @param context
     * @param appWidgetManager
     * @param appWidgetId
     */
    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        String napLabel = context.getString(R.string.default_widget_label);
        updateAppWidget(context, appWidgetManager, appWidgetId, napLabel);
    }
}

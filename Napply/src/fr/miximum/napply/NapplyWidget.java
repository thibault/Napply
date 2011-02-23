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

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.RemoteViews;

public class NapplyWidget extends AppWidgetProvider {

    private static final String PREFS_NAME = "fr.miximum.napply";

    private static final String PREF_DURATION_PREFIX = "nap_duration_";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final int N = appWidgetIds.length;

        Log.e(Napply.TAG, "In widget:onUpdate");
        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i=0; i<N; i++) {
            int appWidgetId = appWidgetIds[i];
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
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
     * Update the widget
     * @param context
     * @param appWidgetManager
     * @param appWidgetId
     */
    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.napply_widget_layout);
        int napDuration = getNapDuration(context, appWidgetId);

        views.setTextViewText(R.id.nap_time, "" + napDuration);
        views.setTextViewText(R.id.nap_end, context.getString(R.string.default_widget_label));
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}

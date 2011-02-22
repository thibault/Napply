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

import fr.miximum.picker.NumberPicker;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;

public class WidgetConfigure extends Activity {

    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    /** The default number of hours for the nap duration */
    private static final int DEFAULT_NAP_HOURS = 0;

    /** The default number of minutes for the nap duration */
    private static final int DEFAULT_NAP_MINUTES = 25;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Set the result to CANCELED. This will cause the widget host to cancel
        // out of the widget placement if they press the back button.
        setResult(RESULT_CANCELED);

        setContentView(R.layout.widget_configure_layout);

        // Find widget id from intent
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // If they gave us an intent without the widget id, just bail.
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }

        // Configure picker widget
        fr.miximum.picker.NumberPicker hourPicker = (NumberPicker) findViewById(R.id.nap_hour);
        hourPicker.setRange(0, 23);
        hourPicker.setCurrent(DEFAULT_NAP_HOURS);

        NumberPicker minutePicker = (NumberPicker) findViewById(R.id.nap_minute);
        minutePicker.setRange(0, 59);
        minutePicker.setCurrent(DEFAULT_NAP_MINUTES);
    }
}

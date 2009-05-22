/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.linuxguy.HPCCMonWidget;

import org.linuxguy.HPCCMonWidget.R;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.text.format.Time;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.linuxguy.HPCCMonWidget.Helper.ApiException;
import org.linuxguy.HPCCMonWidget.Helper.ParseException;

/**
 * A simple widget that shows the status of runs on the high performance
 * computing cluster (HPCC) at Michigan State University.
 * 
 * For more information, please see <http://www.hpcc.msu.edu/>
 */
public class HPCCMonWidget extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {
        // To prevent any ANR timeouts, we perform the update in a service
        context.startService(new Intent(context, UpdateService.class));
    }
    
    public static class UpdateService extends Service {
    	//private Handler updateHandler;
        @Override
        public void onStart(Intent intent, int startId) {
            // Build the widget update for today
            RemoteViews updateViews = buildUpdate(this);
            
            // Push update for this widget to the home screen
            ComponentName thisWidget = new ComponentName(this, HPCCMonWidget.class);
            AppWidgetManager manager = AppWidgetManager.getInstance(this);
            manager.updateAppWidget(thisWidget, updateViews);
        }

        /**
         * Build a widget update to show the HPCC status.
         */
        public RemoteViews buildUpdate(Context context) {
            RemoteViews updateViews = null;
            String pageContent = "";
            
            try {
                // Try querying the HPCC
                Helper.prepareUserAgent(context);
                pageContent = Helper.getPageContent();
            } catch (ApiException e) {
                Log.e("WordWidget", "Couldn't contact API", e);
            } catch (ParseException e) {
                Log.e("WordWidget", "Couldn't parse API response", e);
            }
            
            // Use a regular expression to parse out the word and its definition
            Pattern pattern = Pattern.compile(Helper.HPCC_REGEX, Pattern.DOTALL);
            System.out.println(pageContent);
            Matcher matcher = pattern.matcher(pageContent);
            if (matcher.find()) {
                // Build an update that holds the updated widget contents
                updateViews = new RemoteViews(context.getPackageName(), R.layout.widget_word);
                
                String wordTitle = matcher.group(1) + " Running\n" + matcher.group(2);
                updateViews.setTextViewText(R.id.word_title, "HPCC Status");
                updateViews.setTextViewText(R.id.definition, wordTitle);
            } else {
                // Didn't find word of day, so show error message
                updateViews = new RemoteViews(context.getPackageName(), R.layout.widget_message);
                updateViews.setTextViewText(R.id.message, "Parse Error");
            }
            return updateViews;
        }
        
        @Override
        public IBinder onBind(Intent intent) {
            // We don't need to bind to this service
            return null;
        }
    }
}

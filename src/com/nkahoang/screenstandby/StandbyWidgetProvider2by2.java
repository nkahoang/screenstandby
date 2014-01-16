/*******************************************************************************
 * Copyright (c) 2014 Hoang Nguyen.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Hoang Nguyen - initial API and implementation
 ******************************************************************************/
package com.nkahoang.screenstandby;

import android.app.PendingIntent;
import android.appwidget.AppWidgetProvider;
import android.content.Intent;
import android.content.Context;
import android.widget.RemoteViews;
import android.appwidget.AppWidgetManager;

public class StandbyWidgetProvider2by2 extends AppWidgetProvider {
	@Override
	public void onUpdate(Context context, AppWidgetManager appwidgetmanager,
			int[] appWidgetIds) {
		 final int N = appWidgetIds.length;

	        // Perform this loop procedure for each App Widget that belongs to this provider
	        for (int i=0; i<N; i++) {
	            int appWidgetId = appWidgetIds[i];
	            
	            Intent serviceIntent = new Intent();
	            serviceIntent.setAction(StandbyService.ENABLE_INTENT);
	            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, serviceIntent, PendingIntent.FLAG_UPDATE_CURRENT);

	            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_2by2_layout);
	            views.setOnClickPendingIntent(R.id.imageButton1, pendingIntent);
	            try
	            {
	            	Intent turnonIntent = new Intent();
		            turnonIntent.setAction(StandbyService.TOGGLE_INTENT);
		            PendingIntent pendingIntentOn = PendingIntent.getBroadcast(context, 0, turnonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
	            	views.setOnClickPendingIntent(R.id.imageButton2, pendingIntentOn);
	            	
	            }
	            catch(Exception ex)
	            {
	            	
	            }

	            // Tell the AppWidgetManager to perform an update on the current app widget
	            appwidgetmanager.updateAppWidget(appWidgetId, views);
	        }
	}
}

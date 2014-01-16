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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.Handler;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

public class NotificationBar {
	private  TextView datetime;
	private  DateFormat dateFormat = new SimpleDateFormat("EEE, MMM d, y  HH:mm");
	private  TextView batteryLevel;
	private  WindowManager winMan;
	private  ViewGroup mainlayout;
	private  Handler h;
	 Timer timer = new Timer();
	
	 void detachNotification()
	{
		if (mainlayout!=null)
		{
			winMan.removeViewImmediate(mainlayout);
			mainlayout=null;
		}
	}
	
	 void attachNotification(Context context)
	{
		if (mainlayout == null)
		{
			h = new Handler();
	        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	        
	        //inflater.inflate(R.layout.notificationbar, this, true);
	        mainlayout = (ViewGroup)inflater.inflate(R.layout.notificationbar, mainlayout, false);
	        
	        Typeface typefaceLight = FontManager.getThinFont(context);
	        Typeface typeface = FontManager.getCondensedFont(context);
	        
	        batteryLevel = (TextView)mainlayout.findViewById(R.id.txtBattery);
	        batteryLevel.setTypeface(typeface);
	        setBatteryLevel(context);
	        
	    	datetime = (TextView)mainlayout.findViewById(R.id.txtDateTime);
	    	datetime.setTypeface(typefaceLight);
	    	Calendar cal = Calendar.getInstance();
	    	datetime.setText(dateFormat.format(cal.getTime()).toUpperCase());
	    	timer.schedule(new TimerTask() {
	
				@Override
				public void run() {
					// TODO Auto-generated method stub
					h.post(new Runnable() {
						@Override
						public void run() {
					    	Calendar cal = Calendar.getInstance();
					    	datetime.setText(dateFormat.format(cal.getTime()).toUpperCase());
						}});
				}}, 0, 10000);
	
			winMan = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
			WindowManager.LayoutParams 
			
			paramsfilter = new WindowManager.LayoutParams(
	                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
	                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR,
	                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
	                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
	                PixelFormat.RGBA_8888);
			
			Display display = winMan.getDefaultDisplay();
				paramsfilter.width = display.getWidth();
				paramsfilter.height = (int)Math.ceil(25 * context.getResources().getDisplayMetrics().density);
	        	paramsfilter.y -= paramsfilter.height; 
				paramsfilter.gravity = Gravity.TOP;
				paramsfilter.setTitle("Test");
				
			winMan.addView(mainlayout, paramsfilter);
		}
	}
	
	private void setBatteryLevel(Context context) {
        BroadcastReceiver batteryLevelReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                context.unregisterReceiver(this);
                int rawlevel = intent.getIntExtra("level", -1);
                int scale = intent.getIntExtra("scale", -1);
                int level = -1;
                if (rawlevel >= 0 && scale > 0) {
                    level = (rawlevel * 100) / scale;
                }
                batteryLevel.setText(level + "%");
            }
        };
        IntentFilter batteryLevelFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        context.registerReceiver(batteryLevelReceiver, batteryLevelFilter);
    }
}

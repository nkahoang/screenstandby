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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
/*
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Paint.Style;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnTouchListener;*/
import android.preference.PreferenceManager;

public class StandbyToggleReceiver extends BroadcastReceiver {
	/**
	 * @see android.content.BroadcastReceiver#onReceive(Context,Intent)
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		NotificationManager notificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		
		Boolean hdmi = intent.getBooleanExtra("autohdmi", false);
		if (prefs.getBoolean("twostageenable", false) && !hdmi)
		{
			Logger.Log(context, "Two stage triggered");
				Notification turnOffNotification = new Notification();
				turnOffNotification.icon = R.drawable.ic_launcher;
				turnOffNotification.tickerText = "Standby ready";
				Intent notificationIntent = new Intent(context,
						StandbyService.class);
				PendingIntent contentIntent = PendingIntent.getService(context, 0, 
							notificationIntent, 0);
				if (!prefs.getBoolean("twostagepersistence", false))
					turnOffNotification.flags |= Notification.FLAG_AUTO_CANCEL; 
				turnOffNotification.setLatestEventInfo(context, "Screen standby ready", "Do your stuffs, then click here to turn screen off", contentIntent);
				notificationManager.notify("SCREENSTANDBY_READY", 0, turnOffNotification);
		}
		else
		{
			Logger.Log(context, intent);
			context.startService(new Intent(context, StandbyService.class));
		}
	}
	/*
	class HalloweenView extends ViewGroup {
		int statusBarHeight;
		Boolean showblood = false;
		Boolean showbroken = false;
		Boolean showghost = false;
		Bitmap blood;
		Bitmap brokenscreen;
		Bitmap ghost;
	    public HalloweenView(final Context context) {
	        super(context);
	    	myPaint.setColor(Color.BLACK);
	    	myPaint.setStyle(Style.FILL_AND_STROKE);
    	    try {
    	    	blood = BitmapFactory.decodeResource(context.getResources(),
                        R.drawable.blood);
    	    	brokenscreen = BitmapFactory.decodeResource(context.getResources(),
                        R.drawable.brokenscreen);
    	    	ghost = BitmapFactory.decodeResource(context.getResources(),
                        R.drawable.ghost);
    	    	
				AssetFileDescriptor afd = context.getAssets().openFd("sound.mp3");
				AssetFileDescriptor afd2 = context.getAssets().openFd("sound2.mp3");
				AssetFileDescriptor afd3 = context.getAssets().openFd("glassbroken.mp3");
				final MediaPlayer player = new MediaPlayer();
				final MediaPlayer player2 = new MediaPlayer();
				final MediaPlayer player3 = new MediaPlayer();
				player.setDataSource(afd.getFileDescriptor(),afd.getStartOffset(),afd.getLength());
				player.prepare();
				player.start();
				player2.setDataSource(afd2.getFileDescriptor(),afd2.getStartOffset(),afd2.getLength());
				player2.prepare();
				player2.setOnCompletionListener(new OnCompletionListener() {

					@Override
					public void onCompletion(MediaPlayer mp) {
						WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
			    		wm.removeView(HalloweenView.this);
					}});
				player2.start();
				player3.setDataSource(afd2.getFileDescriptor(),afd3.getStartOffset(),afd3.getLength());
				player3.prepare();
	    	    Thread t = new Thread(new Runnable() {

					@Override
					public void run() {
						try {
							Thread.sleep((int)(Math.random() * 300) + 3000);
							for (int i = 0; i < 20; i++)
							{
								Thread.sleep((int)(Math.random() * 300));
								if (Math.random() > 0.5f)
								{
									showblood = true;
									showbroken = false;
									showghost = false;
									myPaint.setColor(Color.WHITE);
									HalloweenView.this.postInvalidate();
									Thread.sleep((int)(Math.random() * 800));
									continue;
								}
								myPaint.setColor(Color.TRANSPARENT);
								showblood = false;
								showbroken = true;
								showghost = false;
								HalloweenView.this.postInvalidate();
								Thread.sleep((int)(Math.random() * 500));
								myPaint.setColor(Color.BLACK);
								showbroken = false;
								showblood = (Math.random() > 0.5f);
								showghost = (Math.random() > 0.5f);
								HalloweenView.this.postInvalidate();
							}
							Thread.sleep((int)(Math.random() * 500));
							showbroken = true;
							showblood = true;
							showghost = false;
							myPaint.setColor(Color.TRANSPARENT);
							player3.start();
							HalloweenView.this.postInvalidate();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}});
	    	   t.start();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }

    	Paint myPaint = new Paint();

	    @Override
	    protected void onDraw(Canvas canvas) {
	    	canvas.drawRect(0, 0, 1500, 1500, myPaint);

	    	if (showblood)
	    		canvas.drawBitmap(blood, 0, -50, null);
	    	if (showbroken)
	    		canvas.drawBitmap(brokenscreen, 0, 100, null);
	    	if (showghost)
	    		canvas.drawBitmap(ghost, 0, 0, null);
	    }

		@Override
		protected void onLayout(boolean changed, int l, int t, int r, int b) {

		}
	    
	}*/
}

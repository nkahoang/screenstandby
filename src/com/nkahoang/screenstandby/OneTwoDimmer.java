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

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class OneTwoDimmer {
	public interface DimToggleEventHandler
	{
		public void OnDimToggleEventHandler();
	}
	
	int currentLimit = 0;
	Boolean isRunning = false;
	private ReschedulableTimer timer = null;
	private Runnable task = null;
	private DimToggleEventHandler eventhandler = null;
	private SharedPreferences prefs;
	private Context context;
	public OneTwoDimmer(Context context, DimToggleEventHandler handler)
	{
		eventhandler = handler;
		this.context = context;
	}
	
	public Boolean IsRunning()
	{
		return isRunning;
	}
	public void Start()
	{
		if (isRunning) return;
		if (prefs == null)
			prefs = PreferenceManager.getDefaultSharedPreferences(context);
		currentLimit = (prefs.getInt("onetwotimerh", 0) * 3600) +
					   (prefs.getInt("onetwotimerm", 0) * 60) +
					    prefs.getInt("onetwotimers", 15);
		Reset();
		timer = new ReschedulableTimer();
		task = new Runnable() {
	        @Override
	        public void run() {
        		if (eventhandler != null) eventhandler.OnDimToggleEventHandler();
        		isRunning = false;
	        }
	    };
	    timer.schedule(task, currentLimit * 1000);
	    isRunning = true;
	}
	
	public void Reset()
	{
		if (isRunning && (timer != null)) {
			timer.reschedule(currentLimit * 1000);
		}
	}
	
	public void Stop()
	{
		if (isRunning) {
			timer.cancel();
			isRunning = false;
		}
	}
	
	private class ReschedulableTimer extends Timer {
		  private Runnable task;
		  private TimerTask timerTask;

		  public void schedule(Runnable runnable, long delay) {
		    task = runnable;
		    timerTask = new TimerTask() { public void run() { task.run(); } };
		    timer.schedule(timerTask, delay);        
		  }

		  public void reschedule(long delay) {
		    timerTask.cancel();
		    timerTask = new TimerTask() { public void run() { task.run(); } };
		    timer.schedule(timerTask, delay);        
		  }
	}
}

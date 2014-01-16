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

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;

public class AppDetector extends Service {
    Process logcatProc;
    String logcatLine;
    Thread monitorThread;
    ActivityManager am;
    static Boolean isRunning = false;
    public AppDetector() {
    }
    
    private final static String LAUNCH_PARAM = "logcat -v raw ActivityManager:I *:S";
    final static String DETECT_SERVICE_CHANGE = "com.nkahoang.screenstandby.action.APPDETECTIONCHANGED";
    private SharedPreferences prefs;
    private Boolean isEnabled = false;
    private DataOutputStream os;
    private BufferedReader bufferedReader;
    private String package1, package2, package3, package4;
    
    public void onCreate() {
    	isRunning = true;
        super.onCreate();
        
    	prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	am = (ActivityManager) this.getSystemService( ACTIVITY_SERVICE );
    	monitorThread = new Thread(rMonitor);
    	
    	IntentFilter filter = new IntentFilter(DETECT_SERVICE_CHANGE);
    	registerReceiver(receiver, filter);
    	
    	isEnabled = prefs.getBoolean("appdetection", false);
        Initialize();
    }
    
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	if (intent.hasExtra("enabled"))
        	{
        		isEnabled = intent.getBooleanExtra("enabled", false);
        	}
        	Initialize();
        	Logger.Log(context, intent);
        }
    };
    
    public void Initialize()
    {
    	package1 = prefs.getString("appdetect1package","");
    	package2 = prefs.getString("appdetect2package","");
    	package3 = prefs.getString("appdetect3package","");
    	package4 = prefs.getString("appdetect4package","");
    	
    	if (isEnabled) {
    		if (!monitorThread.isAlive())
    			monitorThread.start();
    	}
    	else
    	{
    		try
    		{
    			if (monitorThread.isAlive()) {
    				monitorThread.stop();
		     		os.close();
		     		bufferedReader.close();
		     		logcatProc.destroy();
		     	}
    		}
    		catch(Exception ex){}
    		try
    		{
    			unregisterReceiver(receiver);
    		}
    		catch(Exception ex){}
     		isRunning = false;
    		this.stopSelf();
    	}
    }
    
    private Boolean checkForegroundActivity()
    {
    	return
    		((package1.length() > 0) &&
    			am.getRunningTasks(1).get(0).baseActivity.getPackageName().contains(package1)) ||
			((package2.length() > 0) &&
    			am.getRunningTasks(1).get(0).baseActivity.getPackageName().contains(package2)) ||
			((package3.length() > 0) &&
    			am.getRunningTasks(1).get(0).baseActivity.getPackageName().contains(package3)) ||
			((package4.length() > 0) &&
    			am.getRunningTasks(1).get(0).baseActivity.getPackageName().contains(package4));
    }
    
    private void startProcess()
    {
    	 try {
    		 logcatProc = Runtime.getRuntime().exec("su");
    		 os = new DataOutputStream(logcatProc.getOutputStream());
    		 os.writeBytes(LAUNCH_PARAM + "\n");
    		 bufferedReader = new BufferedReader(new InputStreamReader(logcatProc.getInputStream()));
		     while (((logcatLine = bufferedReader.readLine()) != null) && isEnabled) {
		    	 if (isRunningPackageStr(logcatLine))
		    	 {
		    		 if (checkForegroundActivity())
		    		 {
		    			 Intent serviceIntent = new Intent();
		 	             serviceIntent.setAction(StandbyService.ENABLE_INTENT);
		 	             AppDetector.this.sendBroadcast(serviceIntent);
		    		 }
		    		 else
		    		 {
		    			 Intent serviceIntent = new Intent();
		 	             serviceIntent.setAction(StandbyService.TOGGLE_INTENT);
		 	            AppDetector.this.sendBroadcast(serviceIntent);
		    		 }
		    	 }
		     }
		     os.close();
		     bufferedReader.close();
		     logcatProc.destroy();
		 } catch (IOException e) {}
    }

    public static boolean isRunningPackageStr(String paramString)
    {
    	return paramString.startsWith("Starting acti") ||
    		   (paramString.startsWith("Start") && paramString.contains("for activity")) ||
    		   paramString.startsWith("START");
    }
    
    Runnable rMonitor = new Runnable() {
		@Override
		public void run() {
			startProcess();
		}
	};
		
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

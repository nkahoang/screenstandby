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

import java.io.DataOutputStream;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class HeadsetReceiver extends BroadcastReceiver {

	
	static boolean useHeadset = false;
	static String launchPackage = "";
	private static Boolean killPackage = false;
	
	public HeadsetReceiver() {
	}

	private static void ScreenOn(Context context)
	{
		try
		{
			Intent serviceIntent = new Intent();
        	serviceIntent.setAction(StandbyService.TOGGLE_INTENT);
        	context.sendBroadcast(serviceIntent);
    		Logger.Log(context, "HEADSET UNPLUGGED");
		}
		catch(Exception ex)
		{
			Logger.Log(context, ex);
		}
        try
        {
	        if (killPackage && launchPackage.length() > 0)
	        {
        		Process proc;
        		DataOutputStream os;
        			proc = Runtime.getRuntime().exec("su");
        		    os = new DataOutputStream(proc.getOutputStream());
        		os.writeBytes("kill $(pgrep " + launchPackage + ")\n");
	        }
        }catch(Exception ex)
        {
        	Logger.Log(context, ex.getMessage() + " " + ex.getStackTrace().toString());
        }
        try
        {
	        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
	        if (prefs.getBoolean("headset_close_returnhome", false))
	        {
	        	Logger.Log(context, "HEADSET RETURN HOME");
				Intent startMain = new Intent(Intent.ACTION_MAIN);
				startMain.addCategory(Intent.CATEGORY_HOME);
				startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(startMain);	
	        }
        }
        catch(Exception ex)
        {
        	Logger.Log(context, ex.getMessage() + " " + ex.getStackTrace().toString());
        }
	}
	
	private static void ScreenOff(Context context)
	{
		Logger.Log(context, "HEADSET PLUGGED");
		Intent serviceIntent = new Intent();
        serviceIntent.setAction(StandbyService.ENABLE_INTENT);
        serviceIntent.putExtra("autohdmi", true);
        context.sendBroadcast(serviceIntent);
        if (launchPackage.length() > 0)
        {
        	Intent LaunchIntent = context.getPackageManager().getLaunchIntentForPackage(launchPackage);
        	context.startActivity(LaunchIntent);
        }
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG))
		{
	    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
	    	useHeadset = prefs.getBoolean("useheadset", false);
	    	launchPackage = prefs.getString("headsetrunpackage", "");
	    	killPackage = prefs.getBoolean("heatsetrun_close", false);
	    	if (useHeadset)
	    	{
	    		if (intent.getIntExtra("state", 0) == 0)
	    		{
	    			ScreenOn(context);
	    		}
	    		else if (intent.getIntExtra("state", 0) == 1)
	    		{
	    			ScreenOff(context);
	    		}
	    	}
		}
	}
}

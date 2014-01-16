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

import com.nkahoang.kernelswitchobserver.GenericHardwareObserver;
import com.nkahoang.kernelswitchobserver.UEventStateChangeHandler;
import java.io.DataOutputStream;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class HDMIReceiver extends BroadcastReceiver {
    public HDMIReceiver() {    	
    }
    
    private static String MOTOROLA_HDMIINTENT = "com.motorola.intent.action.externaldisplaystate";
	private static String launchPackage = "";
	private static Boolean killPackage = false;
	private GenericHardwareObserver hdmiobserver;
	
	private static void ScreenOn(Context context)
	{
		try
		{
			Intent serviceIntent = new Intent();
        	serviceIntent.setAction(StandbyService.TOGGLE_INTENT);
        	context.sendBroadcast(serviceIntent);
    		Logger.Log(context, "HDMI UNPLUGGED");
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
	        if (prefs.getBoolean("hdmirun_close_returnhome", false))
	        {
	        	Logger.Log(context, "HDMI RETURN HOME");
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
		Logger.Log(context, "HDMI PLUGGED");
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
    public void onReceive(final Context context, Intent intent) {
    	Logger.Log(context, intent);
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    	launchPackage = prefs.getString("hdmirunpackage", "");
    	killPackage = prefs.getBoolean("hdmirun_close", false);

    	if (intent.hasExtra("state"))
    	{
    		Boolean iState = intent.getBooleanExtra("state", true);
    		String intentToBroadcast = "";
    		if (iState && prefs.getBoolean("hdmi_broadcast_connect", false))
    			intentToBroadcast = prefs.getString("hdmi_broadcast_connect_action", "");
    		else if (!iState && prefs.getBoolean("hdmi_broadcast_disconnect", false))
    			intentToBroadcast = prefs.getString("hdmi_broadcast_disconnect_action", "");
    		if (intentToBroadcast.trim().length() > 0) context.sendBroadcast(new Intent(intentToBroadcast));
    	}
    	
    	if (prefs.getBoolean("hdmidetection", false))
    	{
    		if (intent.getAction().equals(MOTOROLA_HDMIINTENT))
    		{
    			int HDMI = intent.getIntExtra("hdmi",1);
    			int HDCP = intent.getIntExtra("hdcp",1);
    			if (HDMI == HDCP)
    			{
    				if (HDMI == 1)
    					ScreenOff(context);
    				else
    					ScreenOn(context);
    			}
    			else
    			{
    				if (HDCP == 1)
    					ScreenOff(context);
    				else
    					ScreenOn(context);
    			}
    		}
    		else
    		{
		    	Boolean state = true;
		    	if (intent.hasExtra("state"))
		    		state = intent.getBooleanExtra("state", state);
		    	
		    	if (state) {
		    		try
		    		{
		    			hdmiobserver = new GenericHardwareObserver("hdmi");
		    			hdmiobserver.setOnUEventChangeHandler(new UEventStateChangeHandler() {
							@Override
							public void OnUEventStateChange(String NewState) {
								if (NewState.trim().equals("0")) {
									try {
										hdmiobserver.stop();
									}
									catch(Exception ex) {}
									ScreenOn(context);
								}
							}});
		    			hdmiobserver.start();
		    		}
		    		catch(Exception ex)
		    		{
		    			Logger.Log(context, ex);
		    		}
		    		ScreenOff(context);
		    	}
		    	else
		    		ScreenOn(context);
    		}
    	}
    }
}

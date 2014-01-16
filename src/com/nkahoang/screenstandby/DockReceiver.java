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

import com.nkahoang.kernelswitchobserver.GenericHardwareObserver;
import com.nkahoang.kernelswitchobserver.HardwareNotFoundException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class DockReceiver extends BroadcastReceiver {
	private static String launchPackage = "";
	private static Boolean killPackage = false;
    public DockReceiver() {
    }
    private static void ScreenOn(Context context)
	{
		Log.w("standby", "DOCK UNPLUGGED");
		Intent serviceIntent = new Intent();
        serviceIntent.setAction(StandbyService.TOGGLE_INTENT);
        context.sendBroadcast(serviceIntent);
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
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.getBoolean("dockrun_close_returnhome", false))
        {
			Intent startMain = new Intent(Intent.ACTION_MAIN);
			startMain.addCategory(Intent.CATEGORY_HOME);
			startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(startMain);	
        }
	}
	
	private static void ScreenOff(Context context)
	{
		Log.w("standby", "DOCK PLUGGED");
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
    
	private static String getDeviceState(String device)
	{
		String status = "N/A";
		try {
			GenericHardwareObserver ob = new GenericHardwareObserver(device);
			status = ob.getCurrentHardwareState();
		} catch (HardwareNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return status;
	}
	@Override
    public void onReceive(Context context, Intent intent) {
		Logger.Log(context, intent);
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    	Boolean useDock = prefs.getBoolean("dockdetection", false);
    	launchPackage = prefs.getString("dockrunpackage", "");
    	killPackage = prefs.getBoolean("dockrun_close", false);
    	Boolean screenOff = false;
    	
    	if (useDock)
    	{
    		int state = intent.getIntExtra(Intent.EXTRA_DOCK_STATE, Intent.EXTRA_DOCK_STATE_UNDOCKED);
    		//if (state != Intent.EXTRA_DOCK_STATE_UNDOCKED)
    		//{
	    		if (prefs.getBoolean("dockatrix", false))
	    		{
	    			if (getDeviceState("whisper_hid").equals("1") && 
	    				getDeviceState("smartdock").equals("1"))
	    			{
	    				String docktype = getDeviceState("extdock");
	    				if (docktype.equals("3")) //LAPDOCK
	    				{
	    					if (prefs.getBoolean("dockatrix_lapdock", true)) {
	    						ScreenOff(context);
	    						Toast.makeText(context, "Atrix Lapdock detected", Toast.LENGTH_SHORT).show();
	    					}
	    				}
	    				else if (docktype.equals("4")) //HD DOCK
	    				{
	    					if (prefs.getBoolean("dockatrix_hddock", true)) {
	    						ScreenOff(context);
	    						Toast.makeText(context, "Atrix HD dock detected", Toast.LENGTH_SHORT).show();
	    					}
	    				}
	    			}
	    			else
	    				ScreenOn(context);
	    			return;
	    		}
    		//}
    		
    		switch (state)
    		{
    			case Intent.EXTRA_DOCK_STATE_CAR:
    				screenOff = prefs.getBoolean("dockcar", true);
    				break;
    			case Intent.EXTRA_DOCK_STATE_DESK:
    				screenOff = prefs.getBoolean("dockdesk", true);
    				break;
    			case Intent.EXTRA_DOCK_STATE_HE_DESK:
    				screenOff = prefs.getBoolean("dockanalog", true);
    				break;
    			case Intent.EXTRA_DOCK_STATE_LE_DESK:
    				screenOff = prefs.getBoolean("dockdigital", true);
    				break;
    			case Intent.EXTRA_DOCK_STATE_UNDOCKED:
    				if (prefs.getBoolean("dockremoval", true)) ScreenOn(context);
    				return;
    		}
    		if (screenOff)
    			ScreenOff(context);
    	}
    }
}

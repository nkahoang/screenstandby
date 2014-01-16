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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class BootTimeReceiver extends BroadcastReceiver {
	static HeadsetReceiver headsetreceiver = new HeadsetReceiver();
    public BootTimeReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    	Boolean mOneTwoDimmer = prefs.getBoolean("onetwodimenabling", false);
    	Boolean mOneTwoBoot = prefs.getBoolean("onetwodimboot", false);
    	if (mOneTwoDimmer && mOneTwoBoot) {
    		try
    		{
    			Intent serviceIntent = new Intent();
            	serviceIntent.setAction(StandbyService.ENABLE_INTENT);
            	context.sendBroadcast(serviceIntent);
        		Logger.Log(context, "Boot time activation");
    		}
    		catch(Exception ex)
    		{
    			Logger.Log(context, ex);
    		}
    	}
    	if (prefs.getBoolean("appdetection", false))
    	{
    		context.startService(new Intent(context, AppDetector.class));
    	}
    	
    	try
    	{
    	if (prefs.getBoolean("useheadset", false))
    		context.registerReceiver(headsetreceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
    	}
    	catch(Exception ex)
    	{
    	}
    }
}

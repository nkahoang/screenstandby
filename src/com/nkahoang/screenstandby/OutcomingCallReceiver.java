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

import java.util.HashMap;

import com.nkahoang.screenstandby.remotecontrol.ClientService;
import com.nkahoang.screenstandby.remotecontrol.ServerService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class OutcomingCallReceiver extends BroadcastReceiver {
	public OutcomingCallReceiver() {
	}

	@Override
	public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.getBoolean("useautodialer", false))
        {
            String dialedNumber = intent.getStringExtra(
                    Intent.EXTRA_PHONE_NUMBER).trim();
            HashMap<String,SettingActivity.CallAction> actions = SettingActivity.CallAction.GetActions(prefs);
            if (actions.containsKey(dialedNumber))
            {
            	SettingActivity.CallAction act = actions.get(dialedNumber);
            	switch (act.GetAction())
            	{
            		case 0:
            			Intent intentActivity = new Intent(context, Main.class);
            			intentActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            			context.startActivity(intentActivity);
            			break;
            		case 1:
            			Intent intentToggle = new Intent("com.nkahoang.screenstandby.action.TOGGLE");
            			context.sendBroadcast(intentToggle);
            			break;
            		case 2:
            			Intent intentRemote = new Intent("com.nkahoang.screenstandby.action.REMOTE_CONTROL");
            			context.sendBroadcast(intentRemote);
            			break;
            		case 3:
            		case 4:
                    	Intent LaunchIntent = context.getPackageManager().getLaunchIntentForPackage(act.GetAppPackage());
                    	context.startActivity(LaunchIntent);
            			break;
            	}
                setResultData(null);
            }
        }
	}
}

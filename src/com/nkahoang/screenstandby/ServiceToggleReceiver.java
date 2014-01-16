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

import com.nkahoang.screenstandby.Main.MainMenuPageFragment;
import com.nkahoang.screenstandby.remotecontrol.ClientService;
import com.nkahoang.screenstandby.remotecontrol.ServerService;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

public class ServiceToggleReceiver extends BroadcastReceiver {
	public ServiceToggleReceiver() {
	}

	@Override
	public void onReceive(final Context context, Intent intent) {
		if (intent.getAction().equals("com.nkahoang.screenstandby.action.TOGGLE"))
		{
			if (StandbyService.isRunning)
			{
				Intent serviceIntent = new Intent("com.nkahoang.screenstandby.action.DISABLE");
	            context.sendBroadcast(serviceIntent);	
			}
			else
			{
				Intent serviceIntent = new Intent("com.nkahoang.screenstandby.action.ENABLE");
	            context.sendBroadcast(serviceIntent);
			}	
		}
		else if (intent.getAction().equals("com.nkahoang.screenstandby.action.REMOTE_CONTROL"))
		{
			ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

			boolean mWifiConnected = mWifi.isConnected();
			if (!mWifiConnected)
			{
				WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
				java.lang.reflect.Method[] wmMethods = wifi.getClass().getDeclaredMethods();
				
				for(java.lang.reflect.Method method: wmMethods){
					if(method.getName().equals("isWifiApEnabled")) {
						try {
							mWifiConnected = (Boolean)method.invoke(wifi);
						}
						catch(Exception ex)
						{
							Logger.Log(context, ex);
							mWifiConnected = false;
						}
						break;
					}
				}
			}
			
			if (!mWifiConnected) {
				Builder builder = new AlertDialog.Builder(context);
		    	builder
		    	.setTitle("Wifi needed!")
		    	.setMessage("Remote control currently only works with Wifi connection / Wifi tethering.\n\nDo you want to open wifi settings and connect to a wifi network?\n\n (Note: Receiver & controller must be on a same network / tethering hotspot)")
		    	.setIcon(android.R.drawable.ic_dialog_alert)
		    	.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		    	    public void onClick(DialogInterface dialog, int which) {
		    	    	context.startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
		    	    }
		    	})
		    	.setNeutralButton("Proceed anyway", new DialogInterface.OnClickListener() {
		    	    public void onClick(DialogInterface dialog, int which) {
		    	    	Intent intent = new Intent(context,
	    						ServerService.isRunning?RemoteServerActivity.class:
	    						(ClientService.isRunning?RemoteControllerActivity.class:
	    												RemoteControlSelectActivity.class));
		    	    	intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		    	    	context.startActivity(intent);
		    	    }
		    	})
		    	.setNegativeButton("No", null)
		    	.show();	
				return;
			}

			Intent intent2 = new Intent(context,
					ServerService.isRunning?RemoteServerActivity.class:
					(ClientService.isRunning?RemoteControllerActivity.class:
											RemoteControlSelectActivity.class));
			intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(intent2);
		}
	}
}

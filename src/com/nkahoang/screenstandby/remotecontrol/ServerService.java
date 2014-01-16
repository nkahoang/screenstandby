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
package com.nkahoang.screenstandby.remotecontrol;

import com.nkahoang.screenstandby.R;
import com.nkahoang.screenstandby.RemoteServerActivity;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

public class ServerService extends Service {
	public ServerService() {
	}

	final String ACTION_START_SERVER = "com.nkahoang.screenstandby.action.START_SERVER";
	final String ACTION_START_STOP = "com.nkahoang.screenstandby.action.STOP_SERVER";
	private IntentFilter filter;
	private final IBinder mBinder = new ServiceBinder(this);
	public static boolean isRunning = false;
	private OnStateChangedHandler serverStateChangedHandler;
	private OnClientStateChangedHandler clientStateChangedHandler;
	
	static Core remoteCore = null;
	
	@Override
	public void onCreate()
	{
		filter = new IntentFilter(ACTION_START_STOP);
		this.registerReceiver(bReceiver, filter);
		super.onCreate();	
	}
	
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		stopServer();
	}
	
	public void stopServer()
	{
		if (isRunning)
		{
			isRunning = false;
			remoteCore.stopServer();
			remoteCore = null;
			serverStateChangedHandler = null;
			clientStateChangedHandler = null;
	
			try
			{
				if (filter!= null) this.unregisterReceiver(bReceiver);
				this.stopForeground(true);
			}
			catch(Exception ex){}
		}
	}
	
	public Core getRemotingCore()
	{
		return remoteCore;
	}
	
	public void setOnServerStateChangedListener(OnStateChangedHandler handler)
	{
		serverStateChangedHandler = handler;
		if (remoteCore != null)
			remoteCore.setOnStateChangedListener(handler);
	}
	public void setOnClientStateChangedListener(OnClientStateChangedHandler handler)
	{
		clientStateChangedHandler = handler;
		if (remoteCore != null)
			remoteCore.setOnClientStateChangedListener(handler);
	}
	public int getServerState()
	{
		return (remoteCore != null)?remoteCore.getServerState():Core.STATE_NOTHING;
	}
	
	public void startServer()
	{
		Intent remoteActivityIntent = new Intent(ServerService.this, RemoteServerActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, remoteActivityIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        Notification notify = new Notification();
        notify.icon = R.drawable.receiverico;
        notify.setLatestEventInfo(this, "Remote receiver is active", "Click here to open receiver interface", pendingIntent);
        this.startForeground(R.string.service_remote_receiver, notify);
        remoteCore = new Core(ServerService.this);
        remoteCore.startServer();
        if (serverStateChangedHandler != null) remoteCore.setOnStateChangedListener(serverStateChangedHandler);
        if (clientStateChangedHandler != null) remoteCore.setOnClientStateChangedListener(clientStateChangedHandler);
        isRunning = true;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		return Service.START_NOT_STICKY;
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
    
	private BroadcastReceiver bReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(ACTION_START_STOP))
			{
				ServerService.this.stopSelf();
			}
		}
	};
}

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

import java.net.InetAddress;

import javax.jmdns.ServiceInfo;

import com.nkahoang.screenstandby.R;
import com.nkahoang.screenstandby.RemoteControllerActivity;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

public class ClientService extends Service {
	public ClientService() {
	}

	final String ACTION_START_SERVER = "com.nkahoang.screenstandby.action.START_CLIENT";
	final String ACTION_START_STOP = "com.nkahoang.screenstandby.action.STOP_CLIENT";
	private IntentFilter filter;
	private final IBinder mBinder = new ServiceBinder(this);
	public static boolean isRunning = false;
	public static boolean isDiscovering = false;
	private OnStateChangedHandler serverStateChangedHandler;
	private OnClientStateChangedHandler clientStateChangedHandler;
	private OnServiceAddressResolvedHandler serviceAddressResolvedHandler;
	
	static Core remoteCore = null;
	
	@Override
	public void onCreate()
	{
		super.onCreate();	
	}
	
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		stopClient();
	}
	
	public void stopClient()
	{
		if (isRunning)
		{
			isDiscovering = false;
			isRunning = false;
			remoteCore.stopClient();
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
	public void setOnServiceAddressResolvedHandlerListener(OnServiceAddressResolvedHandler handler)
	{
		serviceAddressResolvedHandler = handler;
		if (remoteCore != null)
			remoteCore.setOnServiceAddressResolvedHandler(handler);
	}

	public int getClientState()
	{
		return (remoteCore != null)?remoteCore.getClientState():Core.STATE_NOTHING;
	}
	
	public void stopClientDiscovery()
	{
        isDiscovering = false;
        
		if (remoteCore != null)
	        new Thread(new Runnable() {
				@Override
				public void run() {
						remoteCore.stopClientServiceDiscoverable();
				}}).start();
	}
	public void startClientDiscovery()
	{
        remoteCore.startClientServiceDiscovery();
        if (serverStateChangedHandler != null)
        	remoteCore.setOnStateChangedListener(serverStateChangedHandler);
        if (clientStateChangedHandler != null)
        	remoteCore.setOnClientStateChangedListener(clientStateChangedHandler);
        if (serviceAddressResolvedHandler != null)
        	remoteCore.setOnServiceAddressResolvedHandler(serviceAddressResolvedHandler);
        isDiscovering = true;
	}
	
	public void startClient(ServiceInfo serviceInfo)
	{
		if (!isRunning) {
			Intent remoteActivityIntent = new Intent(ClientService.this, RemoteControllerActivity.class);
			PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, remoteActivityIntent, PendingIntent.FLAG_CANCEL_CURRENT);
	        Notification notify = new Notification();
	        notify.icon = R.drawable.senderico;
	        notify.setLatestEventInfo(this, "Remote controller is active", "Click here to open controller interface", pendingIntent);
	        this.startForeground(R.string.service_remote_receiver, notify);
			filter = new IntentFilter(ACTION_START_STOP);
			this.registerReceiver(bReceiver, filter);
	        remoteCore.startClient(serviceInfo);
	        isRunning = true;	
		}
	}
	
	public void startClient(InetAddress addr, int Port)
	{
		if (!isRunning) {
			Intent remoteActivityIntent = new Intent(ClientService.this, RemoteControllerActivity.class);
			PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, remoteActivityIntent, PendingIntent.FLAG_CANCEL_CURRENT);
	        Notification notify = new Notification();
	        notify.icon = R.drawable.senderico;
	        notify.setLatestEventInfo(this, "Remote controller is active", "Click here to open controller interface", pendingIntent);
	        this.startForeground(R.string.service_remote_receiver, notify);
			filter = new IntentFilter(ACTION_START_STOP);
			this.registerReceiver(bReceiver, filter);
	        remoteCore.startClient(new InetAddress[] {addr}, Port);
	        isRunning = true;
		}
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
        remoteCore = new Core(ClientService.this);
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
				ClientService.this.stopSelf();
			}
		}
	};
}

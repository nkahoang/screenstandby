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

import com.nkahoang.screenstandby.remotecontrol.Core;
import com.nkahoang.screenstandby.remotecontrol.OnClientStateChangedHandler;
import com.nkahoang.screenstandby.remotecontrol.OnStateChangedHandler;
import com.nkahoang.screenstandby.remotecontrol.ServerService;
import com.nkahoang.screenstandby.remotecontrol.ServiceBinder;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.text.Html;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class RemoteServerActivity extends BaseActivity {
	private TextView txtStatus, txtClient, txtServerName, txtServerIP, txtServerWifi,txtConnected;
	private Button btnStopServer;
	private ServerService mService;
	private Core mCore;
	private ViewGroup pnServerInfo, pnNotConnected, pnConnected;
	private ProgressBar progRunning;
	private boolean mBound = false;
	private ProgressDialog progressDialog;

	private final byte PANEL_NOTCONNECTED = 0;
	private final byte PANEL_CONNECTED = 1;
	
	private void switchToPanel(final byte i)
	{
		this.runOnUiThread(new Runnable(){
			@Override
			public void run() {
				pnNotConnected.setVisibility(i == PANEL_NOTCONNECTED ? View.VISIBLE:View.GONE);
				pnConnected.setVisibility(i == PANEL_CONNECTED ? View.VISIBLE:View.GONE);
			}
		});
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_remote_server);
		pnNotConnected =(ViewGroup)this.findViewById(R.id.pnNotConnect);
		pnConnected =(ViewGroup)this.findViewById(R.id.pnConnected);
		switchToPanel(PANEL_NOTCONNECTED);
		TextView txtTitle = (TextView)this.findViewById(R.id.txtTitle);
		txtTitle.setTypeface(typefaceLight);
		TextView txtConnected = (TextView)this.findViewById(R.id.txtConnected);
		txtConnected.setTypeface(typefaceLight);
		txtStatus = (TextView)this.findViewById(R.id.txtStatus);
		txtStatus.setTypeface(typeface);
		txtClient = (TextView)this.findViewById(R.id.txtClient);
		txtClient.setTypeface(typeface);
		txtServerName = (TextView)this.findViewById(R.id.txtServerName);
		txtServerName.setTypeface(typeface);
		txtServerIP= (TextView)this.findViewById(R.id.txtServerIP);
		txtServerIP.setTypeface(typeface);
		txtServerWifi= (TextView)this.findViewById(R.id.txtServerWifi);
		txtServerWifi.setTypeface(typeface);
		pnServerInfo = (ViewGroup)this.findViewById(R.id.pnServerInfo);
		btnStopServer = (Button)this.findViewById(R.id.btnStop);
		progRunning = (ProgressBar) this.findViewById(R.id.progRunning);
		ImageButton btnHome = (ImageButton)this.findViewById(R.id.btnhome);
		btnHome.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				goHomeActivity();
			}
		});
		btnStopServer.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				
		        if (mBound) {
		            unbindService(mConnection);
		            mBound = false;
		        }
		        progressDialog = new ProgressDialog(RemoteServerActivity.this);    
		        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER | ProgressDialog.THEME_HOLO_DARK);
		        progressDialog.setTitle("Stopping receiver");  
		        progressDialog.setMessage("Closing network and cleaning stuffs.\n Please wait...\n");
		        progressDialog.setCancelable(false);
		        progressDialog.setIndeterminate(true);
		        progressDialog.setIcon(android.R.drawable.ic_dialog_info);
		        progressDialog.show();
		        new LoadViewTask().execute();
			}
		});
	}

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        if (!ServerService.isRunning)
        {
        	Intent intent = new Intent(this, ServerService.class);
        	this.startService(intent);
        }
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	if (!mBound) {
    		Intent intent = new Intent(this, ServerService.class);
    		bindService(intent, mConnection, Context.BIND_IMPORTANT);
    	}
    	if (mService!=null)
    		switchToPanel(mService.getServerState() == Core.STATE_SERVER_CONNECTED?PANEL_CONNECTED:PANEL_NOTCONNECTED);
    }

	private String message = "";
	private String submessage = "";
	private int infoVisibility;
	private int progBarVisibility;
    private void changeServerStatus(int state)
    {
    	switch (state)
    	{
    		case Core.STATE_NOTHING:
    			message = "Receiver is configuring";
    			submessage = "Please wait...";
    			infoVisibility = View.GONE;
    			progBarVisibility = View.VISIBLE;
    			break;
			case Core.STATE_SERVER_RUNNING:
				message = "Making device discoverable...";
				submessage = "Receiver is ready";
				mCore = mService.getRemotingCore();
				infoVisibility = View.VISIBLE;
				progBarVisibility = View.VISIBLE;
				break;
			case Core.STATE_SERVER_BROADCAST_SEVICE_CONFIGURED:
				message = "Receiver is now discoverable";
				submessage = "Waiting for clients..."; 
				mCore = mService.getRemotingCore();
				infoVisibility = View.VISIBLE;
				progBarVisibility = View.GONE;
				break;
			case Core.STATE_SERVER_BROADCAST_SEVICE_ERROR:
				message = "Warning: Receiver can be connected to using manual method only";
				submessage = "Please connect manually using below IP address";
				mCore = mService.getRemotingCore();
				infoVisibility = View.VISIBLE;
				progBarVisibility = View.GONE;
				break;
			case Core.STATE_SERVER_ERROR:
				message = "Receiver has an error";
				switchToPanel(PANEL_NOTCONNECTED);
				infoVisibility = View.GONE;
				progBarVisibility = View.GONE;
				break;
			case Core.STATE_SERVER_CONNECTED:
				switchToPanel(PANEL_CONNECTED);
				break;
			case Core.STATE_SERVER_DISCONNECTED:
				switchToPanel(PANEL_NOTCONNECTED);
				break;
    	}
    	this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				pnServerInfo.setVisibility(infoVisibility);
				progRunning.setVisibility(progBarVisibility);
				txtStatus.setText(message);
				if (infoVisibility == View.VISIBLE)
				{
					txtServerName.setText(Html.fromHtml("device name: <b>" + mCore.getDeviceName() + "</b>"));
					txtServerIP.setText(Html.fromHtml("device IP: <b>" + mCore.getDeviceIP() + "</b> port: <b>"+ mCore.getServicePort() + "</b>"));
					txtServerWifi.setText(Html.fromHtml("wi-fi network: <b>" + mCore.getWifiConnectionName() + "</b>"));
				}
			}
    	});
    }
    
    private OnClientStateChangedHandler clientStateHandler = new OnClientStateChangedHandler()
    {
		@Override
		public void OnStateChanged(int state) {
			
		}
    };
    
    private OnStateChangedHandler serverStateHandler = new OnStateChangedHandler()
    {
		@Override
		public void OnStateChanged(int oldState, int newState) {
			changeServerStatus(newState);
		}
    };

    @Override
    protected void onStop() {
        super.onStop();
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    private class LoadViewTask extends AsyncTask<Void, Integer, Void>  
    {
		@Override
		protected Void doInBackground(Void... arg0) {
			try {
				mService.stopServer();
			} catch (Exception e) {
			}

            mService.stopServer();
	        try
	        {
	    		Intent intent = new Intent(RemoteServerActivity.this, ServerService.class);
		        stopService(intent);
	        }
	        catch(Exception ex)
	        {
	        }
			return null;
		}
		

        //after executing the code in the thread  
        @Override  
        protected void onPostExecute(Void result)  
        {  
            //close the progress dialog  
            progressDialog.dismiss();
            goHomeActivity();
            finish();
        }  	
    }
    private void goHomeActivity()
    {
    	Intent intent = new Intent(RemoteServerActivity.this,
				Main.class);
		startActivity(intent);
		finish();
    }
    
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            ServiceBinder binder = (ServiceBinder) service;
            mService = (ServerService)binder.getService();
            mBound = true;
            mService.setOnServerStateChangedListener(serverStateHandler);
            mService.setOnClientStateChangedListener(clientStateHandler);
            if (!ServerService.isRunning)
            	mService.startServer();
            mCore = mService.getRemotingCore();
            changeServerStatus(mService.getServerState());
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
}

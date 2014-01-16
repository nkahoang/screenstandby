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

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.List;

import javax.jmdns.ServiceInfo;

import com.nkahoang.screenstandby.remotecontrol.ClientService;
import com.nkahoang.screenstandby.remotecontrol.Core;
import com.nkahoang.screenstandby.remotecontrol.OnClientStateChangedHandler;
import com.nkahoang.screenstandby.remotecontrol.OnServiceAddressResolvedHandler;
import com.nkahoang.screenstandby.remotecontrol.OnStateChangedHandler;
import com.nkahoang.screenstandby.remotecontrol.RemoteAppPackages;
import com.nkahoang.screenstandby.remotecontrol.RemotePackageInfo;
import com.nkahoang.screenstandby.remotecontrol.ServiceBinder;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.wifi.WifiInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class RemoteControllerActivity extends BaseActivity {
	private Button btnManualConnect, btnRestart;
	private TextView txtStatus, txtTitle;
	private ViewGroup pnReceiverLists, pnMainSelector, pnMainRemote;
	private ClientService mService;
	private Core mCore;
	private boolean mBound = false;
	private final byte PANEL_SELECTOR = 0;
	private final byte PANEL_REMOTE = 1;
	private ProgressDialog progressDialog;
	
	private void switchToPanel(byte i)
	{
		pnMainSelector.setVisibility(i == PANEL_SELECTOR ? View.VISIBLE:View.GONE);
		txtTitle.setVisibility(pnMainSelector.getVisibility());
		pnMainRemote.setVisibility(i == PANEL_REMOTE ? View.VISIBLE:View.GONE);
	}
	
	private OnClickListener sendVerbButtonOnClick = new OnClickListener() {

		@Override
		public void onClick(View v) {
			try {
				mCore.sendAction((Character)v.getTag());
			} catch (IOException e) {
				Logger.Log(RemoteControllerActivity.this, e);
				Log.e("ScreenStandby", e.getMessage());
			}
		}
		
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_remote_controller);

		pnMainSelector = (ViewGroup) this.findViewById(R.id.pnReceiverSelector);
		pnMainRemote = (ViewGroup) this.findViewById(R.id.pnRemoteControl);
		
		txtTitle = (TextView)this.findViewById(R.id.txtTitle);
		txtTitle.setTypeface(typefaceLight);

		LinearLayout pnHeader = (LinearLayout)this.findViewById(R.id.pnHeader);
		
		txtStatus = (TextView)pnHeader.findViewById(R.id.txtStatus);
		txtStatus.setTypeface(typeface);
		
		ImageButton btnStart = (ImageButton)this.findViewById(R.id.btnMediaPlay);
		btnStart.setTag(Core.VERB_PLAY);
		btnStart.setOnClickListener(sendVerbButtonOnClick);
		
		ImageButton btnStop = (ImageButton)this.findViewById(R.id.btnMediaStop);
		btnStop.setTag(Core.VERB_STOP);
		btnStop.setOnClickListener(sendVerbButtonOnClick);
		
		ImageButton btnNext = (ImageButton)this.findViewById(R.id.btnMediaNext);
		btnNext.setTag(Core.VERB_NEXT);
		btnNext.setOnClickListener(sendVerbButtonOnClick);
		
		ImageButton btnBack = (ImageButton)this.findViewById(R.id.btnMediaBack);
		btnBack.setTag(Core.VERB_PREVIOUS);
		btnBack.setOnClickListener(sendVerbButtonOnClick);
		
		ImageButton btnPause = (ImageButton)this.findViewById(R.id.btnMediaPause);
		btnPause.setTag(Core.VERB_PAUSE);
		btnPause.setOnClickListener(sendVerbButtonOnClick);
		
		ImageButton btnFF = (ImageButton)this.findViewById(R.id.btnMediaFF);
		btnFF.setTag(Core.VERB_FAST_FORWARD);
		btnFF.setOnClickListener(sendVerbButtonOnClick);
		
		ImageButton btnRewind = (ImageButton)this.findViewById(R.id.btnMediaRewind);
		btnRewind.setTag(Core.VERB_REWIND);
		btnRewind.setOnClickListener(sendVerbButtonOnClick);
		
		ImageButton btnVolUp = (ImageButton)this.findViewById(R.id.btnMediaVolUp);
		btnVolUp.setTag(Core.VERB_VOLUMEUP);
		btnVolUp.setOnClickListener(sendVerbButtonOnClick);
		
		ImageButton btnVolDown = (ImageButton)this.findViewById(R.id.btnMediaVolDown);
		btnVolDown.setTag(Core.VERB_VOLUMEDOWN);
		btnVolDown.setOnClickListener(sendVerbButtonOnClick);
		
		ImageButton btnVolMute = (ImageButton)this.findViewById(R.id.btnMediaVolMute);
		btnVolMute.setTag(Core.VERB_VOLUMEMUTE);
		btnVolMute.setOnClickListener(sendVerbButtonOnClick);
		
		ImageButton btnDpadUp = (ImageButton)this.findViewById(R.id.btnDpadUp);
		btnDpadUp.setTag(Core.VERB_DPAD_UP);
		btnDpadUp.setOnClickListener(sendVerbButtonOnClick);
		
		ImageButton btnDpadDown = (ImageButton)this.findViewById(R.id.btnDpadDown);
		btnDpadDown.setTag(Core.VERB_DPAD_DOWN);
		btnDpadDown.setOnClickListener(sendVerbButtonOnClick);
		
		ImageButton btnDpadLeft = (ImageButton)this.findViewById(R.id.btnDpadLeft);
		btnDpadLeft.setTag(Core.VERB_DPAD_LEFT);
		btnDpadLeft.setOnClickListener(sendVerbButtonOnClick);
		
		ImageButton btnDpadRight = (ImageButton)this.findViewById(R.id.btnDpadRight);
		btnDpadRight.setTag(Core.VERB_DPAD_RIGHT);
		btnDpadRight.setOnClickListener(sendVerbButtonOnClick);

		ImageButton btnDpadCenter = (ImageButton)this.findViewById(R.id.btnDpadCenter);
		btnDpadCenter.setTag(Core.VERB_DPAD_CENTER);
		btnDpadCenter.setOnClickListener(sendVerbButtonOnClick);

		ImageButton btnKeyBack = (ImageButton)this.findViewById(R.id.btnKeyBack);
		btnKeyBack.setTag(Core.VERB_KEY_BACK);
		btnKeyBack.setOnClickListener(sendVerbButtonOnClick);
		
		ImageButton btnKeyHome = (ImageButton)this.findViewById(R.id.btnKeyHome);
		btnKeyHome.setTag(Core.VERB_KEY_HOME);
		btnKeyHome.setOnClickListener(sendVerbButtonOnClick);
		
		ImageButton btnKeyRecent = (ImageButton)this.findViewById(R.id.btnKeyRecent);
		btnKeyRecent.setTag(Core.VERB_KEY_APPSWITCH);
		btnKeyRecent.setOnClickListener(sendVerbButtonOnClick);
		
		ImageButton btnKeyMenu = (ImageButton)this.findViewById(R.id.btnKeyMenu);
		btnKeyMenu.setTag(Core.VERB_KEY_MENU);
		btnKeyMenu.setOnClickListener(sendVerbButtonOnClick);
		
		ImageButton btnKeySearch = (ImageButton)this.findViewById(R.id.btnKeySearch);
		btnKeySearch.setTag(Core.VERB_KEY_SEARCH);
		btnKeySearch.setOnClickListener(sendVerbButtonOnClick);
		
		ImageButton btnScreenOff = (ImageButton)this.findViewById(R.id.btnScreenOff);
		btnScreenOff.setTag(Core.VERB_SCREENOFF);
		btnScreenOff.setOnClickListener(sendVerbButtonOnClick);
		
		ImageButton btnScreenOn = (ImageButton)this.findViewById(R.id.btnScreenOn);
		btnScreenOn.setTag(Core.VERB_SCREENON);
		btnScreenOn.setOnClickListener(sendVerbButtonOnClick);
		
		Button btnLaunchApp = (Button)this.findViewById(R.id.btnLaunchApp);
		btnLaunchApp.setTag(Core.VERB_REQUEST_PACKAGE_LIST);
		btnLaunchApp.setOnClickListener(btnLaunchAppOnClick);
		
		Button btnDisconnect = (Button)this.findViewById(R.id.btnDisconnect);
		btnDisconnect.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(RemoteControllerActivity.this);
		    	builder
		    	.setTitle("Stop remote controller?")
		    	.setMessage("Are you sure you want to disconnect remote controller?")
		    	.setIcon(R.drawable.disconnectico)
		    	.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		    	    public void onClick(DialogInterface dialog, int which) {
				        progressDialog = new ProgressDialog(RemoteControllerActivity.this);    
				        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER | ProgressDialog.THEME_HOLO_DARK);
				        progressDialog.setTitle("Disconnecting");  
				        progressDialog.setMessage("Closing network and cleaning stuffs.\n Please wait...\n");
				        progressDialog.setCancelable(false);
				        progressDialog.setIndeterminate(true);
				        progressDialog.setIcon(R.drawable.disconnectico);
				        progressDialog.show();
				        new LoadViewTask().execute();
		    	    }
		    	})
		    	.setNegativeButton("No", null)
		    	.show();	
				return;
			}
		});
		
		ImageButton btnKeyInput = (ImageButton)this.findViewById(R.id.btnKeyInput);
		btnKeyInput.setOnClickListener(sendInputButtonOnClick);
		
		btnManualConnect = (Button)this.findViewById(R.id.btnManualConnect);
		btnManualConnect.setTypeface(typeface);
		btnManualConnect.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showManualIPConnect();
			}
		});

		ImageButton btnHome = (ImageButton)this.findViewById(R.id.btnhome);
		btnHome.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				goHomeActivity();
			}
		});
		btnRestart = (Button)pnHeader.findViewById(R.id.btnRestart);
		btnRestart.setOnClickListener(new OnClickListener() {
			@SuppressLint("NewApi")
			@Override
			public void onClick(View v) {
				RemoteControllerActivity.this.recreate();
			}
		});
		pnReceiverLists = (ViewGroup)this.findViewById(R.id.pnClients);
	}

	private String message = "";
    private void changeClientStatus(final int state)
    {
		this.runOnUiThread(new Runnable() {
		@Override
		public void run() {
	    	switch (state)
	    	{
	    		case Core.STATE_NOTHING:
	    			message = "Discovering...";
	    			break;
				case Core.STATE_CLIENT_DISCOVERY_STARTED:
					message = "Discovering...";
					mCore = mService.getRemotingCore();
					break;
				case Core.STATE_CLIENT_DISCOVERY_ERROR:
					message = "Cannot discover receivers... Please try again or connect manually";
					break;
				case Core.STATE_CLIENT_ERROR_CANNOT_CONNECT:
					
				case Core.STATE_CLIENT_ERROR:
					message = "Error occurred. Retry connecting perhap?";
					if (mService != null) {
						mService.stopClient();
					}
	    				switchToPanel(PANEL_SELECTOR);
					break;
				case Core.STATE_CLIENT_DISCONNECTED:
					AlertDialog.Builder inputBuilder = new AlertDialog.Builder(RemoteControllerActivity.this);
					inputBuilder.setTitle("Remote controller disconnected");
					inputBuilder.setMessage("Receiver requested a disconnection");
					inputBuilder.setIcon(R.drawable.disconnectico);
					inputBuilder.setNegativeButton("Dismiss", new AlertDialog.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							progressDialog = new ProgressDialog(RemoteControllerActivity.this);    
					        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER | ProgressDialog.THEME_HOLO_DARK);
					        progressDialog.setTitle("Disconnecting");  
					        progressDialog.setMessage("Closing network and cleaning stuffs.\n Please wait...\n");
					        progressDialog.setCancelable(false);
					        progressDialog.setIndeterminate(true);
					        progressDialog.setIcon(R.drawable.disconnectico);
					        progressDialog.show();
					        new LoadViewTask().execute();
					        dialog.dismiss();
						}
					});
				case Core.STATE_CLIENT_CONNECTED:
					switchToPanel(PANEL_REMOTE);
					return;
	    	}
			txtStatus.setText(message);}
		});
    }
    
	@Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        if (!(ClientService.isRunning || ClientService.isDiscovering))
        {
        	Intent intent = new Intent(this, ClientService.class);
        	this.startService(intent);
        }
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	if (!mBound) {
    		Intent intent = new Intent(this, ClientService.class);
    		bindService(intent, mConnection, Context.BIND_IMPORTANT);
    	}
    }
    
    @Override
    protected void onPause()
    {
    	super.onPause();
    	IPDialog = null;
    }
    
	@Override
    protected void onStop() {
        super.onStop();
        try
        {
        	IPDialog = null;
        	mService.stopClientDiscovery();
        }
        catch(Exception ex) {};
        if (mBound)
        {
        	unbindService(mConnection);
        	mBound = false;
        }
	}
	
    private OnClientStateChangedHandler clientStateHandler = new OnClientStateChangedHandler()
    {
		@Override
		public void OnStateChanged(int state) {			
			changeClientStatus(state);
		}
    };
    
    private OnStateChangedHandler serverStateHandler = new OnStateChangedHandler()
    {
		@Override
		public void OnStateChanged(int oldState, int newState) {
		}
    };
    
    private OnServiceAddressResolvedHandler serviceResolvedHandler = new OnServiceAddressResolvedHandler()
    {
		@Override
		public void OnServiceAddressResolved(final String name,final javax.jmdns.ServiceInfo sinfo, final boolean isAdd) {
			RemoteControllerActivity.this.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (isAdd)
					{
						Button b = new Button(RemoteControllerActivity.this);
						LinearLayout.LayoutParams lParams = new LinearLayout.LayoutParams(
								LinearLayout.LayoutParams.MATCH_PARENT,
								LinearLayout.LayoutParams.WRAP_CONTENT);
						lParams.setMargins(0, 5, 0, 1);
						b.setLayoutParams(lParams);
						boolean isTablet = false;
						try
						{
							isTablet = sinfo.getPropertyString("bTab").equals("true");
						}
						catch(Exception ex) {}
						b.setText(sinfo.getName());
						b.setTextColor(Color.WHITE);
						
						b.setBackgroundResource(R.drawable.metrobuttongrey);
						//b.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(isTablet? R.drawable.tabletico:R.drawable.phoneico),null,null,null);
						b.setCompoundDrawablesWithIntrinsicBounds(isTablet? R.drawable.tabletico:R.drawable.phoneico, 0, 0, 0);
						b.setPadding(30, 2, 2, 3);
						b.setTag(sinfo);
						b.setOnClickListener(btnSelectReceiver);
						if (pnReceiverLists.getChildCount() == 0)
						{
							TextView tv = new Button(RemoteControllerActivity.this);
							tv.setText("Select one of the receivers below: ");
							tv.setClickable(false);
							tv.setTypeface(typeface,Typeface.BOLD);
							tv.setBackgroundColor(Color.TRANSPARENT);
							LinearLayout.LayoutParams lParamsT = new LinearLayout.LayoutParams(
									LinearLayout.LayoutParams.MATCH_PARENT,
									LinearLayout.LayoutParams.WRAP_CONTENT);
							lParamsT.setMargins(0, 2, 0, 1);
							pnReceiverLists.addView(tv);
						}
						pnReceiverLists.addView(b);
					}
					else
					{
						for (int i = 1; i < pnReceiverLists.getChildCount(); i++)
						{
							Button b = (Button)pnReceiverLists.getChildAt(i);
							if (b.getTag().equals(sinfo))
							{
								pnReceiverLists.removeViewAt(i);
								break;
							}
						}
						if (pnReceiverLists.getChildCount() == 1)
							pnReceiverLists.removeAllViews();
					}
				}});
		}
    };
    
    private OnClickListener btnSelectReceiver = new OnClickListener() {
		@Override
		public void onClick(View v) {
			ServiceInfo si = (ServiceInfo)v.getTag();
			if (si != null)
			{
				startController(si);
				mService.stopClientDiscovery();
			}
		}
    };
    
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            ServiceBinder binder = (ServiceBinder) service;
            mService = (ClientService)binder.getService();
            mBound = true;
            mService.setOnServerStateChangedListener(serverStateHandler);
            mService.setOnClientStateChangedListener(clientStateHandler);
            mService.setOnServiceAddressResolvedHandlerListener(serviceResolvedHandler);
            if (!ClientService.isDiscovering) mService.startClientDiscovery();
            	changeClientStatus(mService.getClientState());
    			mCore = mService.getRemotingCore();
        	if (mCore.getIsClientMode()) {
    			runOnUiThread(new Runnable() {
					@Override
					public void run() {
	            		switchToPanel(PANEL_REMOTE);
					}
    			});
        	}
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    AlertDialog.Builder IPDialog;
    TextView ip1, ip2, ip3, ip4, ipPort;
    android.net.wifi.WifiManager wifi;
    private void getAssignIPAddressToTextbox()
    {
    	if (wifi == null) wifi = (android.net.wifi.WifiManager) getSystemService(android.content.Context.WIFI_SERVICE);
    	WifiInfo info = wifi.getConnectionInfo();
    	int ip = info.getIpAddress();
    	if (ip > 0)
    	{
        	if (ip1 != null) ip1.setText((ip & 0xFF) + "");
        	if (ip2 != null) ip2.setText(((ip >> 8) & 0xFF) + "");
        	if (ip3 != null) ip3.setText(((ip >> 16) & 0xFF) + "");
        	if (ip4 != null) ip4.setText("");	
    	}
    	else
    	{
            try {
                for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                    NetworkInterface intf = en.nextElement();
                    if (intf.getName().contains("wlan"))
                        for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                        	InetAddress addr = enumIpAddr.nextElement();
                        	if (Inet4Address.class.isInstance(addr))
                        	{
								byte[] b = addr.getAddress();
								if (ip1 != null) ip1.setText((b[0] & 0xFF) + "");
								if (ip2 != null) ip2.setText((b[1] & 0xFF) + "");
								if (ip3 != null) ip3.setText((b[2] & 0xFF) + "");
								break;
                        	}
                        }
                }
            } catch (Exception ex) {
                Logger.Log(getBaseContext(), ex);
            }
    	}
    	if (ipPort != null) ipPort.setText(Core.SERVICE_PORT+"");
    }
    private void showManualIPConnect()
    {
		IPDialog = new AlertDialog.Builder(this);
		LayoutInflater inflater = getLayoutInflater();
		ViewGroup root = new FrameLayout(this);
		inflater.inflate(R.layout.dialog_manual_connect, root);
		IPDialog.setView(root);
		IPDialog.setTitle("Manually connect");
		ip1 = (TextView)root.findViewById(R.id.txtIP1);
		ip2 = (TextView)root.findViewById(R.id.txtIP2);
		ip3 = (TextView)root.findViewById(R.id.txtIP3);
		ip4 = (TextView)root.findViewById(R.id.txtIP4);
		ipPort = (TextView)root.findViewById(R.id.txtPort);
		IPDialog.setPositiveButton("Connect", btnManualConnectClick);
		IPDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
	    });
    	getAssignIPAddressToTextbox();
    	IPDialog.show();
    }
    
    private DialogInterface.OnClickListener btnManualConnectClick = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {

			/*
			try
			{
				address = new byte[] {
						Byte.parseByte(ip1.getText().toString().trim()),
						Byte.parseByte(ip2.getText().toString().trim()),
						Byte.parseByte(ip3.getText().toString().trim()),
						Byte.parseByte(ip4.getText().toString().trim())};
			}
			catch(Exception ex)
			{
				try
				{
					address = new byte[] {
							Byte.parseByte(ip4.getText().toString().trim()),
							Byte.parseByte(ip3.getText().toString().trim()),
							Byte.parseByte(ip2.getText().toString().trim()),
							Byte.parseByte(ip1.getText().toString().trim())};
				}
				catch(Exception ex2)
				{
					Logger.Log(getBaseContext(), ex);
					Toast.makeText(RemoteControllerActivity.this, "Invalid IP address", Toast.LENGTH_SHORT).show();
					return;
				}
				Logger.Log(getBaseContext(), ex);
				Toast.makeText(RemoteControllerActivity.this, "Invalid IP address", Toast.LENGTH_SHORT).show();
			}
			*/
			dialog.dismiss();
			Thread t = new Thread(new Runnable() {
				@Override
				public void run() {
					boolean OK = false;
					java.net.InetAddress addr= null;
					int port = Core.SERVICE_PORT;
					try
					{
						addr = java.net.Inet4Address.getByName(
								ip1.getText().toString().trim() + "." +
								ip2.getText().toString().trim() + "." +
								ip3.getText().toString().trim() + "." +
								ip4.getText().toString().trim());
						port = Integer.parseInt(ipPort.getText().toString().trim());
					}
					catch(Exception ex)
					{
						Logger.Log(getBaseContext(), ex);
						Toast.makeText(RemoteControllerActivity.this, "Invalid IP address", Toast.LENGTH_SHORT).show();
						return;
					}
					
					try
					{
						startController(addr,port);
						OK = true;
					}
					catch(Exception ex)
					{
						Logger.Log(getBaseContext(), ex);
					}
					if (OK) mService.stopClientDiscovery();
					else Toast.makeText(RemoteControllerActivity.this, "An error occured trying to connect manually", Toast.LENGTH_SHORT).show();
				}});
			t.start();
		}
    };
    
    private void startController(javax.jmdns.ServiceInfo serviceInfo)
    {
    	mService.startClient(serviceInfo);
    }
    private void startController(InetAddress addr, int Port)
    {
    	mService.startClient(addr, Port);
    }
    
    private class LoadViewTask extends AsyncTask<Void, Integer, Void>  
    {
		@Override
		protected Void doInBackground(Void... arg0) {
			try {
				mService.stopClient();
		        if (mBound) {
		            unbindService(mConnection);
		            mBound = false;
		        }
			} catch (Exception e) {
			}
			mService.stopClient();
	        try
	        {
	    		Intent intent = new Intent(RemoteControllerActivity.this, ClientService.class);
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
        }  	
    }

    private OnClickListener sendInputButtonOnClick = new OnClickListener() {
		public void onClick(View arg0) {
			AlertDialog.Builder inputBuilder = new AlertDialog.Builder(RemoteControllerActivity.this);
			final EditText input = new EditText(RemoteControllerActivity.this);
			input.setInputType(InputType.TYPE_CLASS_TEXT);
			input.setLayoutParams(new ViewGroup.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT));
			inputBuilder.setTitle("Send input");
			inputBuilder.setMessage("Enter text to send to remote device: ");
			inputBuilder.setView(input);
			inputBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
		         public void onClick(DialogInterface dialog, int whichButton) {
		             String text = input.getText().toString();
		             if (mCore != null)
		             {
		            	 try {
							mCore.sendTextInput(text);
						} catch (IOException e) {
							Logger.Log(RemoteControllerActivity.this, e);
							Toast.makeText(RemoteControllerActivity.this, "An error occurred while trying to send text to remote device", Toast.LENGTH_SHORT).show();
						}
		             }
	            	dialog.dismiss();
		         }
		    });
		    inputBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		         public void onClick(DialogInterface dialog, int whichButton) {
		                dialog.dismiss();
		         }
		    }).show();
		}
    };
    
    private OnClickListener btnLaunchAppOnClick = new OnClickListener()
    {
		@Override
		public void onClick(View v) {
			RemoteAppPackages rap = mCore.getRemoteAppPackages();
			if (rap != null)
			{
				showAppDialog(rap);
			}
			else
			{
		    	progressDialog = new ProgressDialog(RemoteControllerActivity.this);    
		        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER | ProgressDialog.THEME_HOLO_DARK);
		        progressDialog.setTitle("Fetching app list");  
		        progressDialog.setMessage("Requesting list of applications from remote device...");
		        progressDialog.setCancelable(false);
		        progressDialog.setIndeterminate(true);
		        progressDialog.setIcon(R.drawable.launchappico);
		        progressDialog.show();
		        Runnable rCallback = new Runnable(){
					@Override
					public void run() {
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									RemoteAppPackages rap2 = mCore.getRemoteAppPackages();
									progressDialog.dismiss();
									if (rap2 != null)
										showAppDialog(rap2);
									else
										Toast.makeText(RemoteControllerActivity.this, "An error occurred during fetching app list", Toast.LENGTH_SHORT).show();
								}});
					}};
				mCore.requestRemoteAppPackages(rCallback);
			}
		}
    };
    
    AlertDialog.Builder appList;
    AlertDialog appListDialog;
    private void showAppDialog(RemoteAppPackages pkgs)
    {
    	appList = new AlertDialog.Builder(this);
		LayoutInflater inflater = getLayoutInflater();
		ViewGroup root = new FrameLayout(this);
		inflater.inflate(R.layout.app_launcher_dialog, root);
		appList.setView(root);
		appList.setTitle("Remote app launcher");
		
		TableLayout fAppList = (TableLayout)root.findViewById(R.id.pnAppList);
		List<RemotePackageInfo> info = pkgs.getPackages();
		TableRow currentRow = null;
		int i = 0;
		final int ItemPerRow = (this.getResources().getBoolean(R.bool.isTablet) && !mCore.canDeviceMakeCall())?6:3;
		for(RemotePackageInfo p : info)
		{
			if (i % ItemPerRow == 0)
			{
				currentRow = new TableRow(this);
				currentRow.setGravity(Gravity.CENTER);
				fAppList.addView(currentRow);
			}
			Button b = new Button(this);
			b.setBackgroundResource(R.drawable.metromediabtnbg);
			Drawable d = p.getDrawable();
			d.setBounds(0, 0, 65, 65);
			b.setCompoundDrawables(null, d, null, null);
			b.setText(p.getLabel());
			b.setTag(p.getPackageName());
			b.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					try {
						mCore.requestLaunchApp(v.getTag().toString());
						appListDialog.dismiss();
					} catch (IOException e) {
						Logger.Log(RemoteControllerActivity.this, e);
					}
				} });
			b.setWidth(192);
			b.setHeight(192);
			currentRow.addView(b);
			i++;
		}
		
		appList.setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
	    });
		appListDialog = appList.show();
    }
    
    private void goHomeActivity()
    {
    	Intent intent = new Intent(RemoteControllerActivity.this,
				Main.class);
		startActivity(intent);
		finish();
    }
}

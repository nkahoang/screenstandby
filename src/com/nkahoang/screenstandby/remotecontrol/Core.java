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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;
import javax.jmdns.ServiceInfo;

import com.nkahoang.screenstandby.Logger;
import com.nkahoang.screenstandby.R;
import com.nkahoang.screenstandby.StandbyService;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.AudioManager;
import android.net.wifi.WifiInfo;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;

public class Core {
    protected Context context;
    public Core(Context context)
    {
    	this.context = context;
    }

    public static final int STATE_SERVER_ERROR = 0;
    public static final int STATE_NOTHING = 1;
    public static final int STATE_SERVER_BROADCAST_SEVICE_CONFIGURED = 2;
    public static final int STATE_SERVER_BROADCAST_SEVICE_ERROR = 32;
    public static final int STATE_SERVER_RUNNING = 4;
    public static final int STATE_SERVER_STOPPED = 8;
    public static final int STATE_SERVER_CONNECTED = 16;
    public static final int STATE_SERVER_DISCONNECTED = 64;
    
    public static final int STATE_CLIENT_RUNNING = 16;
    public static final int STATE_CLIENT_ERROR = 0;
    public static final int STATE_CLIENT_ERROR_CANNOT_CONNECT = 64;
    public static final int STATE_CLIENT_NOTCONNECTED = 1;
    public static final int STATE_CLIENT_CONNECTED = 2;
    public static final int STATE_CLIENT_DISCONNECTED = 4;
    public static final int STATE_CLIENT_DISCOVERY_STARTED = 8;
    public static final int STATE_CLIENT_DISCOVERY_STOPPED = 16;
    public static final int STATE_CLIENT_DISCOVERY_ERROR = 32;
    
    public static final int[] SERVICE_PORTS_LIST = new int[] {12609,12610,31092,12992};
    public static int SERVICE_PORT = 12609;
    
    private int status = STATE_NOTHING;
    private int clientstatus = STATE_CLIENT_NOTCONNECTED;
    
    private android.net.wifi.WifiManager.MulticastLock lock;
    private String type = "_rmctss._tcp.local.";
    private ServiceInfo serviceInfo;
    
    private final char STATUS = 0x01;
    private final char VERB = 0x02;
    private final char END_CHAR = 0x0F;
    private final char EXTRA = 0x03;
    private final char STATUS_CONNECTED = 0x10;
    private final char STATUS_DISCONNECTED = 0x11;
    public static final char VERB_DISCONNECT = 0x12; //0x02 first: ACTION
    public static final char VERB_SCREENOFF = 0x13;
    public static final char VERB_SCREENON = 0x14;
    public static final char VERB_PLAY = 0x15;
    public static final char VERB_PAUSE = 0x16;
    public static final char VERB_PREVIOUS = 0x17;
    public static final char VERB_NEXT = 0x18;
    public static final char VERB_STOP = 0x19;
    public static final char VERB_VOLUMEUP = 0x1A;
    public static final char VERB_VOLUMEDOWN = 0x1B;
    public static final char VERB_VOLUMEMUTE= 0x29;
    public static final char VERB_PLAYPAUSE = 0x1C;
    public static final char VERB_REWIND = 0x1D;
    public static final char VERB_FAST_FORWARD = 0x1E;
    public static final char VERB_KEY_HOME = 0x1F;
    public static final char VERB_DPAD_UP = 0x20;
    public static final char VERB_DPAD_DOWN = 0x21;
    public static final char VERB_DPAD_LEFT = 0x22;
    public static final char VERB_DPAD_RIGHT = 0x23;
    public static final char VERB_DPAD_CENTER = 0x24;
    public static final char VERB_KEY_BACK = 0x25;
    public static final char VERB_KEY_APPSWITCH = 0x26;
    public static final char VERB_KEY_MENU = 0x27;
    public static final char VERB_KEY_SEARCH = 0x28;
    public static final char VERB_REQUEST_PACKAGE_LIST = 0x40;
    public static final char EXTRA_REQUEST_LAUNCH_APP_PACKAGE = 0x30;
    public static final char EXTRA_INPUT_TEXT = 0x31;
    public static final char OBJECT_JSON_PACKAGE_LIST= 0x20;
    public static final char OBJECT_BYTE_PACKAGE_LIST= 0x22;
    
    private BufferedReader in;
    private BufferedWriter out;
    
    private Process proc;
	private DataOutputStream os;
    
    private ArrayList<OnStateChangedHandler> _statechangedhandlers = new ArrayList<OnStateChangedHandler>();
    private ArrayList<OnClientStateChangedHandler> _clientstatechangedhandlers = new ArrayList<OnClientStateChangedHandler>();
    private ArrayList<OnServiceAddressResolvedHandler> _serviceaddressresolvedhandlers = new ArrayList<OnServiceAddressResolvedHandler>();
	private Hashtable<String, ServiceInfo> addressTable;
	private Hashtable<Character, Object> receivedObjects = new Hashtable<Character, Object>();
    private JmDNS jmdns = null;
    private Thread currentThread = null;
    private Socket currentSocket = null;
	private ServerSocket ss = null;
    private String _IP = "";
    private String _WifiName = "";
    private boolean isClientMode = false;
    private boolean isServerMode = false;
    private Runnable objectReceivedCallback;
    
    public boolean getIsClientMode()
    {
    	return isClientMode;
    }
    public boolean getIsServerMode()
    {
    	return isServerMode;
    }
    
    public RemoteAppPackages getRemoteAppPackages()
    {
		return (receivedObjects.containsKey(OBJECT_BYTE_PACKAGE_LIST))?(RemoteAppPackages)receivedObjects.get(OBJECT_BYTE_PACKAGE_LIST):null;
    }
    
    public void requestRemoteAppPackages(Runnable callback)
    {
		try {
			objectReceivedCallback = callback;
			this.sendAction(VERB_REQUEST_PACKAGE_LIST);
		} catch (IOException e) {
			Logger.Log(context, e);
		}
    }
    
    public int getServicePort()
    {
    	return SERVICE_PORT;
    }
    
    public String getDeviceIP()
    {
    	return _IP;
    }
    
    public String getDeviceName()
    {
    	return android.os.Build.BRAND + " " + android.os.Build.MODEL;
    }
    
    public int getServerState()
    {
    	return status;
    }
    
    public int getClientState()
    {
    	return clientstatus;
    }
    
    public boolean canDeviceMakeCall()
    {
        TelephonyManager manager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        return !(manager.getPhoneType() == TelephonyManager.PHONE_TYPE_NONE);
    }
    
    public void setOnStateChangedListener(OnStateChangedHandler handler)
    {
    	if (!_statechangedhandlers.contains(handler))
    		_statechangedhandlers.add(handler);
    }
    
    public void removeOnStateChangedListener(OnStateChangedHandler handler)
    {
    	if (_statechangedhandlers.contains(handler))
    		_statechangedhandlers.remove(handler);
    }
    
    public void removeAllOnStateChangedListener()
    {
		_statechangedhandlers.clear();
    }
    
    public void setOnClientStateChangedListener(OnClientStateChangedHandler handler)
    {
    	if (!_clientstatechangedhandlers.contains(handler))
    		_clientstatechangedhandlers.add(handler);
    }
    
    public void removeOnClientStateChangedListener(OnClientStateChangedHandler handler)
    {
    	if (_clientstatechangedhandlers.contains(handler))
    		_clientstatechangedhandlers.remove(handler);
    }
    
    public void removeAllOnClientStateChangedListener()
    {
    	_clientstatechangedhandlers.clear();
    }
    
    public void setOnServiceAddressResolvedHandler(OnServiceAddressResolvedHandler handler)
    {
    	if (!_serviceaddressresolvedhandlers.contains(handler))
    		_serviceaddressresolvedhandlers.add(handler);
    }
    
    public void removeOnServiceAddressResolvedHandler(OnServiceAddressResolvedHandler handler)
    {
    	if (_serviceaddressresolvedhandlers.contains(handler))
    		_serviceaddressresolvedhandlers.remove(handler);
    }
    
    public void removeAllOnServiceAddressResolvedHandler()
    {
		_serviceaddressresolvedhandlers.clear();
    }
    
	public Thread startClientServiceDiscovery()
	{
		if (!isServerMode)
		try
		{
			Thread t = new Thread(new Runnable() {
				@Override
				public void run() {
					jmdns = setUpClient();
					if (jmdns == null)
						Core.this.changeClientState(STATE_CLIENT_DISCOVERY_ERROR);
					else
						Core.this.changeClientState(STATE_CLIENT_DISCOVERY_STARTED);
				}
			});
			t.start();
			return t;
		}
		catch(Exception ex) {
			Core.this.changeClientState(STATE_CLIENT_DISCOVERY_ERROR);
			return null; 
		}
		return null;
	}
	
	public void stopClientServiceDiscoverable()
	{
		try
		{
			if(jmdns != null && clientListener != null)
				jmdns.removeServiceListener(type, clientListener);
			try
			{
				jmdns.close();
				jmdns = null;
			}
			catch(Exception ex){};
			Core.this.changeClientState(STATE_CLIENT_DISCOVERY_STOPPED);
			lock.release();
		}
		catch(Exception ex)
		{
			Logger.Log(context, ex);
		}
	}
	
	public void stopClient()
	{
		if (isClientMode)
		{
			this.isClientMode = false;
			stopClientServiceDiscoverable();
			this.removeAllOnClientStateChangedListener();
			this.removeAllOnStateChangedListener();
			this.removeAllOnStateChangedListener();
			try {
				this.sendAction(VERB_DISCONNECT);
			} catch (IOException e) {Logger.Log(context, e);}
			
			try {
				this.currentSocket.close(); }
			catch (Exception ex) {Logger.Log(context, ex);}
			
			try {
				this.currentThread.stop();
			}
			catch (Exception ex) {Logger.Log(context, ex);}
		}
	}
	
	public void stopServer()
	{
		if (isServerMode)
		{
			this.isServerMode = false;
			this.removeAllOnClientStateChangedListener();
			this.removeAllOnStateChangedListener();
			closeCmd();
			try {
				this.sendAction(VERB_DISCONNECT);
			} catch (IOException e) {
				Logger.Log(context, e);
			}
			try
			{
				jmdns.unregisterService(serviceInfo);
			}
			catch(Exception ex){
				Logger.Log(context, ex);
			}
			try
			{
				jmdns.close();
			}
			catch(Exception ex){
				Logger.Log(context, ex);
			};
			
			try
			{
				this.currentSocket.close();
			}
			catch (Exception ex) {Logger.Log(context, ex);}
			try
			{
				this.currentThread.stop();
			}
			catch (Exception ex) {Logger.Log(context, ex);}
			try
			{
				lock.release();
			}
			catch (Exception ex) {Logger.Log(context, ex);}
			try
			{
				this.ss.close();
			}
			catch (Exception ex) {Logger.Log(context, ex);}
			Log.w("ScreenStandby", "Remote receiver stopped");
			Logger.Log(context, "Remote receiver stopped");
		}
	}
	public Thread startServer()
	{
		if (!isClientMode)
		try
		{
			isServerMode = true;
			Thread serverThread = new Thread(new Runnable() {
				@Override
				public void run() {
					//initializing jmdns service for autoconf
					new Thread(new Runnable()
					{
						@Override
						public void run() {
							startCmds();
						}
					}).start();
					
					while (true)
					{
						try
						{
							Socket client = null;
							try
							{
									for(int port: SERVICE_PORTS_LIST)
									{
										try
										{
											ss = new ServerSocket(port);
											SERVICE_PORT = port; //record the port if port is available
											break;
										}
										catch(Exception e)
										{
											ss = null;
											continue; //cannot find a port, continue
										}	
									}
									
								if (ss == null) {

									Logger.Log(context, "Server error");
									changeState(STATE_SERVER_ERROR);
									return;
								}
								new Thread(new Runnable()
								{
									@Override
									public void run() {
										// Broadcast discovery services async
										jmdns = setUpServer();
										if (jmdns != null) changeState(STATE_SERVER_BROADCAST_SEVICE_CONFIGURED);
										else changeState(STATE_SERVER_BROADCAST_SEVICE_ERROR);
									}
								}).start();
								changeState(STATE_SERVER_RUNNING);
								client = ss.accept();
								try
								{
									if (jmdns != null) jmdns.unregisterService(serviceInfo);
								}
								catch(Exception ex) { }
							}
							catch(Exception ex){
								break;
							}
							if (client != null)
							{
								currentSocket = client;
								in = new BufferedReader(new InputStreamReader(client.getInputStream()));
								out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
	
								out.write(createStatus(STATUS_CONNECTED));
								changeClientState(STATE_CLIENT_CONNECTED);
								changeState(STATE_SERVER_CONNECTED);
								
								if (processConnection())
								{}
								
								client.close();
								ss.close();
								changeState(STATE_SERVER_DISCONNECTED);
								try
								{
									jmdns.registerService(serviceInfo);
								}
								catch(Exception ex) { 
									Logger.Log(context, ex);
									Log.e("Screen standby", ex.getMessage());
								}
							}
							continue;
						}
						catch (IOException e)
						{
							changeClientState(STATE_CLIENT_ERROR);
							continue;
						}
					}
					changeState(STATE_SERVER_STOPPED);
					isServerMode = false;
				}});
			serverThread.start();
			currentThread = serverThread;
			return serverThread;
		}
		catch(Exception ex)
		{
			return null;
		}
		return null;
	}

	public void startClient(final ServiceInfo serviceInfo) //Connect To Server from a service Info
	{
		startClient(serviceInfo.getInetAddresses(), serviceInfo.getPort());
	}
	
	public void startClient(final InetAddress[] addresses, final int port)
	{	
		if (!(this.isServerMode || this.isClientMode)) {
			Thread t = new Thread(new Runnable(){
			@Override
			public void run() {
				for(InetAddress address: addresses)
				{
					if (startClientInternal(address, port)) {
						Core.this.changeClientState(STATE_CLIENT_CONNECTED);
						return;
					}
				}
				Core.this.changeClientState(STATE_CLIENT_ERROR);
			}
			});
			t.start();
		}
	}
	
	private boolean startClientInternal(InetAddress address, int Port)
	{
		try
		{	
			Socket client = new Socket(address, Port);
			currentSocket = client;
			in = new BufferedReader(new InputStreamReader(client.getInputStream()));
			out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
			Thread clientThread = new Thread(new Runnable()
			{
				@Override
				public void run() {
					processConnection();
				}
			});
			clientThread.start();
			currentThread = clientThread;
			this.isClientMode = true;
			return true;
		}
		catch(Exception ex)
		{
			Logger.Log(context, ex);
			Log.e("Screen standby", ex.getMessage());
		}
		return false;
	}

	public void sendTextInput (String text) throws IOException
	{
		this.sendExtra(EXTRA_INPUT_TEXT, text.toCharArray());
	}
	public void requestLaunchApp (String packageName) throws IOException
	{
		this.sendExtra(EXTRA_REQUEST_LAUNCH_APP_PACKAGE, packageName.toCharArray());
	}
	
	public void sendAction (char verb) throws IOException
	{
		if (socketReady()) {
			out.write(createVerb(verb));
			out.flush();
		}
	}
	public void sendStatus (char status) throws IOException
	{
		if (socketReady()) {
			out.write(createStatus(status));
			out.flush();
		}
	}
	/*
	public void sendJsonObject (char object_type, Object object) throws IOException
	{
		String sJson = gson.toJson(object);
		char[] stringCharArray = sJson.toCharArray();
		sendExtra(object_type, stringCharArray);
	}
	*/
	
	public void sendObject (char object_type, Object object) throws IOException
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput oOut = null;
		try {
		  oOut = new ObjectOutputStream(bos);   
		  oOut.writeObject(object);
		  oOut.flush();
		  oOut.close();
		  byte[] bytes = bos.toByteArray();
		  writeToFile(bytes);
		  bos.close();
		  Log.w("ScreenStandby", "Finished serializing object, size="+bytes.length);
		  sendExtra(object_type, bytes);
		}
		catch(Exception ex)
		{
			Logger.Log(context, ex);
			Log.e("ScreenStandby", ex.getMessage());
		}
	}
	
	public void sendExtra (char extra, char[] rawValue) throws IOException
	{
		if (socketReady())
		{
			out.write(createExtra(extra));
			char[] lengthByte = intToChars(rawValue.length);
			out.write(lengthByte);
			out.write(rawValue);
			out.flush();
		}
	}
	public void sendExtra (char extra, byte[] rawBytesValue) throws IOException
	{
		if (socketReady())
		{
			char[] rawChars = bytesToChars(rawBytesValue);
			char[] lengthByte = intToChars(rawChars.length);
			Logger.Log(context, "Length: " + (int)lengthByte[0] + " " + (int)lengthByte[1] + " " + (int)lengthByte[2] + " " + (int)lengthByte[3]);
			Logger.Log(context, "Finished sending object, size="+rawChars.length);
			sendExtra(extra, rawChars);
		}
	}

    private void changeClientState(int newstate)
    {
    	if (clientstatus != newstate)
    	{
    		clientstatus = newstate;
    		for(OnClientStateChangedHandler handler : _clientstatechangedhandlers)
    			if (handler != null)
    				handler.OnStateChanged(newstate);
    	}
    }
    private void changeState(int newstate)
    {
    	if (status != newstate)
    	{
    		int oldstate = status;
    		status = newstate;
    		for(OnStateChangedHandler handler : _statechangedhandlers)
    			if (handler != null)
    				handler.OnStateChanged(oldstate, newstate);
    	}
    }
	
	private ServiceListener clientListener = new ServiceListener() {
        public void serviceResolved(ServiceEvent ev) {
        	ServiceInfo sInfo = ev.getInfo();
            if (!sInfo.getPropertyString("sHost").equals(android.os.Build.HOST))
            {
		        Log.w("Standby","Service resolved: "
		                 + sInfo.getQualifiedName()
		                 + " port:" + sInfo.getPort());
		    	if (!addressTable.containsKey(ev.getName()))
		    	{
		    		addressTable.put(ev.getName(), sInfo);
		    		for(OnServiceAddressResolvedHandler handler: _serviceaddressresolvedhandlers)
		    			if (handler != null) handler.OnServiceAddressResolved(ev.getName(), sInfo,true);
		    	}
            }
        }
        public void serviceRemoved(ServiceEvent ev) {
        	Log.w("Standby", "Service removed: " + ev.getName());
        	for(OnServiceAddressResolvedHandler handler: _serviceaddressresolvedhandlers)
        	{
	        	if (handler != null) handler.OnServiceAddressResolved(ev.getName(), ev.getInfo(), false);
	        	if (addressTable.containsKey(ev.getName()))
	        		addressTable.remove(ev.getName());
        	}
        }
        public void serviceAdded(ServiceEvent event) {
            jmdns.requestServiceInfo(event.getType(), event.getName(), 1);
        }
    };
	
	private boolean socketReady()
	{
		return ((currentSocket != null) && (in != null) && (out != null));
	}

    private char[] createStatus(char status)
    {
    	return new char[] {STATUS,status};
    }
    
    private char[] createVerb(char verb)
    {
    	return new char[] {VERB,verb};
    }
    
    private char[] createExtra(char extra)
    {
    	return new char[] {EXTRA,extra};
    }
    
    private static String intToIp(int i) {
 	   		return 
	               ( i & 0xFF) + "." +
 	               ((i >> 8 ) & 0xFF) + "." +
 	               ((i >> 16 ) & 0xFF) + "." +
 	               ((i >> 24 ) & 0xFF );
	}
    
    public String getWifiConnectionName()
    {
    	return _WifiName;
    }
    
    private JmDNS setUpServer() {
        JmDNS jmdns = null;
        android.net.wifi.WifiManager wifi = (android.net.wifi.WifiManager) context.getSystemService(android.content.Context.WIFI_SERVICE);
        lock = wifi.createMulticastLock("mylock");
        WifiInfo info = wifi.getConnectionInfo();
        _WifiName = info.getSSID();
        lock.setReferenceCounted(true);
        lock.acquire();
        try {
        	int intaddr = info.getIpAddress();
        	byte[] byteaddr = new byte[] { (byte) (intaddr & 0xff), (byte) (intaddr >> 8 & 0xff), (byte) (intaddr >> 16 & 0xff), (byte) (intaddr >> 24 & 0xff) };
        	InetAddress addr=InetAddress.getByAddress(byteaddr); //Need to process UnknownHostException
        	_IP = intToIp(intaddr);
        	jmdns=JmDNS.create(addr);
    		serviceInfo = ServiceInfo.create(type, getDeviceName(), SERVICE_PORT, "SS Remote");
			java.util.HashMap<String,String> mProp = new java.util.HashMap<String,String>();
			
			//if device has a big screen and cannot make call, it is a tablet
			mProp.put("bTab", (this.context.getResources().getBoolean(R.bool.isTablet) && !canDeviceMakeCall())+"");
			mProp.put("sHost", android.os.Build.HOST);
			serviceInfo.setText(mProp);
        	if (isServerMode) jmdns.registerService(serviceInfo);
        	else jmdns.close();
        } catch (IOException e) {
            Logger.Log(context, e);
        }
        return jmdns;
    }
    
    private JmDNS setUpClient() {
        android.net.wifi.WifiManager wifi = (android.net.wifi.WifiManager) context.getSystemService(android.content.Context.WIFI_SERVICE);
        lock = wifi.createMulticastLock("clientlock");
        WifiInfo info = wifi.getConnectionInfo();
        lock.setReferenceCounted(false);
        lock.acquire();
        try {
        	InetAddress addr = null;
        	int intaddr = info.getIpAddress();
        	if (intaddr == 0)
        	{
                try {
                    for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                        NetworkInterface intf = en.nextElement();
                        if (intf.getName().contains("wlan"))
	                        for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
	                        	InetAddress address = enumIpAddr.nextElement();
	                        	if (Inet4Address.class.isInstance(address))
	                        	{
		                        	addr = address;
									byte[] b = addr.getAddress();
									intaddr = ((b[0] & 0xFF) << 0) |
									                 ((b[1] & 0xFF) << 8) |
									                 ((b[2] & 0xFF) << 16)  |
									                 ((b[3] & 0xFF) << 24);
									break;
	                        	}
	                        }
                    }
                } catch (Exception ex) {
                    Logger.Log(context, ex);
                }
        	}
        	

        	_IP = intToIp(intaddr);
    		addressTable = new Hashtable<String, ServiceInfo>();

			byte[] byteaddr = new byte[] { (byte) (intaddr & 0xff), (byte) (intaddr >> 8 & 0xff), (byte) (intaddr >> 16 & 0xff), (byte) (intaddr >> 24 & 0xff) };
			addr=InetAddress.getByAddress(byteaddr); //Need to process UnknownHostException
    		
    		jmdns=JmDNS.create(addr);
            jmdns.addServiceListener(type, clientListener);
        } catch (IOException ex) {
            Logger.Log(context, ex);
        }
        return jmdns;
    }
    
    private void sendKey(String keycode)
    {
    	try
    	{
    		this.doCmds("input keyevent " + keycode);
    	}
    	catch(Exception ex)
    	{
    	}
    }
    private void sendMediaButton(int keycode)
    {
    	long eventtime = SystemClock.uptimeMillis();

	  	Intent downIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null); 
	  	KeyEvent downEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN, keycode, 0); 
	  	downIntent.putExtra(Intent.EXTRA_KEY_EVENT, downEvent); 
	  	context.sendOrderedBroadcast(downIntent, null);

	  	Intent upIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null); 
	  	KeyEvent upEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_UP, keycode, 0); 
	  	upIntent.putExtra(Intent.EXTRA_KEY_EVENT, upEvent); 
	  	context.sendOrderedBroadcast(upIntent, null); 
    }
    
    private boolean processConnection()
    {
    	try
    	{
	    	boolean connected = true;
			while (connected)
			{
				char[] message = new char[2];
				for (int i = 0; i < 2; i ++)
					message[i] = (char)in.read();
				
				switch (message[0])
				{
					case VERB:
						switch(message[1])
						{
							case VERB_KEY_HOME:
								Intent startMain = new Intent(Intent.ACTION_MAIN);
				    			startMain.addCategory(Intent.CATEGORY_HOME);
				    			startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				    			context.startActivity(startMain);
								break;
							case VERB_KEY_BACK:
								sendKey("KEYCODE_BACK");
								break;
							case VERB_KEY_APPSWITCH:
								sendKey("KEYCODE_APP_SWITCH");
								break;
							case VERB_KEY_MENU:
								sendKey("KEYCODE_MENU");
								break;
							case VERB_KEY_SEARCH:
								sendKey("KEYCODE_SEARCH");
								break;
							case VERB_DPAD_UP:
								sendKey("KEYCODE_DPAD_UP");
								break;
							case VERB_DPAD_DOWN:
								sendKey("KEYCODE_DPAD_DOWN");
								break;
							case VERB_DPAD_LEFT:
								sendKey("KEYCODE_DPAD_LEFT");
								break;
							case VERB_DPAD_RIGHT:
								sendKey("KEYCODE_DPAD_RIGHT");
								break;
							case VERB_DPAD_CENTER:
								sendKey("KEYCODE_ENTER");
								break;
							case VERB_DISCONNECT: //Client asking to disconnect
								out.write(createStatus(STATUS_DISCONNECTED));
								changeClientState(STATE_CLIENT_DISCONNECTED);
								connected = false;
								return true;
							case VERB_SCREENOFF:
								Intent screenOnIntent = new Intent();
								screenOnIntent.setAction(StandbyService.ENABLE_INTENT);
								screenOnIntent.putExtra("remotecontrol", true);
			    	            context.sendBroadcast(screenOnIntent);
								break;
							case VERB_SCREENON:
								Intent screenOffIntent = new Intent();
								screenOffIntent.setAction(StandbyService.TOGGLE_INTENT);
								screenOffIntent.putExtra("remotecontrol", true);
			    	            context.sendBroadcast(screenOffIntent);
								break;
							case VERB_PLAY:
								sendMediaButton(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
								break;
							case VERB_PAUSE:
								sendMediaButton(KeyEvent.KEYCODE_MEDIA_PAUSE);
								break;
							case VERB_PLAYPAUSE:
								sendMediaButton(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
								break;
							case VERB_STOP:
								sendMediaButton(KeyEvent.KEYCODE_MEDIA_STOP);
								break;
							case VERB_PREVIOUS:
								sendMediaButton(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
								break;
							case VERB_NEXT:
								sendMediaButton(KeyEvent.KEYCODE_MEDIA_NEXT);
								break;
							case VERB_REWIND:
								sendMediaButton(KeyEvent.KEYCODE_MEDIA_REWIND);
								break;
							case VERB_FAST_FORWARD:
								sendMediaButton(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD);
								break;
							case VERB_VOLUMEUP:
								increaseVolume();
								break;
							case VERB_VOLUMEDOWN:
								decreaseVolume();
								break;
							case VERB_VOLUMEMUTE:
								toggleVolumeMute();
								break;
							case END_CHAR:
								break;
							case VERB_REQUEST_PACKAGE_LIST:
								final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
								mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
								final PackageManager pm = context.getPackageManager();
								final List<ResolveInfo> pkgAppsList = pm.queryIntentActivities( mainIntent, 0);
								RemoteAppPackages rap = new RemoteAppPackages();
								ResolveInfo r;
								String label;
								int i;
								for (int j = 1; j < pkgAppsList.size() - 1; j++) //sorting
								{
									r= pkgAppsList.get(j);
									label = r.loadLabel(pm).toString().toLowerCase().trim();
									for(i = j - 1; (i >= 0) && (pkgAppsList.get(i).loadLabel(pm).toString().toLowerCase().trim().compareTo(label) > 0); i--) {
										pkgAppsList.set(i+1, pkgAppsList.get(i));
							        }
									pkgAppsList.set(i+1, r);
								}
								for(ResolveInfo ri : pkgAppsList)
									rap.put(ri.loadIcon(pm),
											ri.activityInfo.packageName,
											ri.loadLabel(pm).toString());
								
								this.sendObject(Core.OBJECT_BYTE_PACKAGE_LIST, rap);
								//this.sendJsonObject(Core.OBJECT_BYTE_PACKAGE_LIST, rap);
								break;
						}
					break;
					case STATUS: break; //TODO: process Status and Extra messages
					case EXTRA:
						char[] lengthByte = new char[4];
						in.read(lengthByte);
						Log.w("ScreenStandby", "Length: " + lengthByte[0] + " " + lengthByte[1] + " " + lengthByte[2] + " " + lengthByte[3]);
						int length = charsToInt(lengthByte);
						char[] rawMessage = new char[length];
						for (int i = 0; i < length; i++)
							rawMessage[i] = (char)in.read();
						switch (message[1])
						{
							case Core.EXTRA_INPUT_TEXT:
						    	try
						    	{
						    		String inputText = new String(rawMessage);
						    		inputText = inputText.replace("\n", "");
						    		inputText = inputText.replace("\"", "\\\"");
						    		String[] words = inputText.split(" ");
						    		for (int i = 0; i < words.length; i++)
						    		{
							    		this.doCmds("input text " + words[i]);
							    		if (i != (words.length - 1)) this.doCmds("input keyevent KEYCODE_SPACE");
						    		}
						    	}
						    	catch(Exception ex)
						    	{
						    		Logger.Log(context, ex);
						    	}
								break;
							case Core.EXTRA_REQUEST_LAUNCH_APP_PACKAGE:
								String packageName = new String(rawMessage);
								Intent LaunchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
					        	context.startActivity(LaunchIntent);
								break;
							case Core.OBJECT_BYTE_PACKAGE_LIST:
								
								//int value = in.read(rawMessage, 0, length);
								byte[] b = charsToBytes(rawMessage);
								writeToFile(b);
								ByteArrayInputStream bis = new ByteArrayInputStream(b);
								ObjectInputStream oin = null;
								try {
								  oin = new ObjectInputStream(bis);//new GZIPInputStream(bis));
								  RemoteAppPackages o = (RemoteAppPackages)oin.readObject();
								  receivedObjects.put(Core.OBJECT_BYTE_PACKAGE_LIST, o);  
								} 
								catch(Exception ex)
								{
									Logger.Log(context, ex);
									Log.w("ScreenStandby", ex.getMessage());
								}
								finally {
								  bis.close();
								  if (oin != null) oin.close();
								}
								if (objectReceivedCallback != null)
									objectReceivedCallback.run();
								break;
						}
						break;
					case END_CHAR: break; //also use as padding / safe end string
				}
			}
			return true;
    	}
    	catch(Exception ex)
    	{
			changeClientState(STATE_CLIENT_ERROR);
			return false;
    	}
    }
    
    private AudioManager audioManager;
    private void increaseVolume()
    {
    	if (audioManager == null)
    		audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
    	audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND);
    }
    private boolean volumeMute = false; 
    private void toggleVolumeMute()
    {
    	if (audioManager == null)
    		audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
    	volumeMute = !volumeMute;
    	audioManager.setStreamMute(AudioManager.STREAM_MUSIC, volumeMute);
    }
    private void decreaseVolume()
    {
    	if (audioManager == null)
    		audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
    	audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND);
    }

    private boolean _rootFeatures = true;
	
    private void startCmds(){
    	try
    	{
    		proc = Runtime.getRuntime().exec("su");
    		os = new DataOutputStream(proc.getOutputStream());
    		//is = new DataInputStream(proc.getInputStream());
    		_rootFeatures = true;
    	}
    	catch(Exception ex)
    	{
    		_rootFeatures = false;
    	}
    }
    
	private void doCmds(String cmds) throws Exception {
		if (proc == null)
		{
			startCmds();
		}
		if (_rootFeatures)
		{
			os.writeBytes(cmds+"\n");
			os.flush();
		}
	}

	private static byte[] charsToBytes(char[] chars)
	{
		byte[] bytes = new byte[chars.length];
		for(int i=0;i<chars.length;i++) {
		   bytes[i] = (byte)chars[i];
		}
		return bytes;
	}
	
	private static char[] bytesToChars(byte[] bytes)
	{
		char[] chars = new char[bytes.length];
		for(int i=0;i<chars.length;i++) chars[i] = (char) bytes[i];
		return chars;
	}
	
	private static int charsToInt(char[] bytes)
	{
		return((bytes[0] & 0xFF) << 24) | ((bytes[1] & 0xFF) << 16)
		        | ((bytes[2] & 0xFF) << 8) | (bytes[3] & 0xFF);
	}
	
	private static char[] intToChars(int value)
	{
		char[] chars = new char[4];
		chars[0] = (char) ((value >> 24) & 0xFF);
		chars[1] = (char) ((value >> 16) & 0xFF);
		chars[2] = (char) ((value >> 8) & 0xFF);
		chars[3] = (char) (value & 0xFF);
		return chars;
	}
	private void writeToFile(byte[] b)
	{
		/* DEBUGGING PURPOSE ONLY
		String logfile = Environment.getExternalStorageDirectory().getPath() + "/object.bin";
		File file = new File(logfile);
		try {
			file.createNewFile();
			FileOutputStream fOut = new FileOutputStream(file);
			fOut.write(b);
			fOut.close();
		} catch (IOException e) {
		}*/
	}
	private void closeCmd()
	{
		try
		{
			if (proc != null && _rootFeatures)
			{
				doCmds("exit");
				proc.destroy();
				proc = null;
				os = null;
			}
		}
		catch(Exception ex)
		{
		}
	}
}

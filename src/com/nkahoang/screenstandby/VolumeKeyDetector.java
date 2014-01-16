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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class VolumeKeyDetector {
	public static final char VOLUME_UP = 73;
	public static final char VOLUME_DOWN = 72;
	//private final char executable_permission = 777;
	private Boolean compatibilityMode = false;
	private Process geteventProc;
    private String geteventLine;
    private Thread monitorThread;
    private OnVolumeKeyDetectedHandler vkeyhandler = null;
    boolean isEnabled = false;
	private String keypadDeviceName;
	private String keypadDeviceEventName;
    private DataOutputStream os;
    private BufferedReader bufferedReader;
    private Context context;
	
    public void setCompatibilityMode(Boolean value)
    {
    	compatibilityMode = value;
    }
	@SuppressLint("DefaultLocale")
	public VolumeKeyDetector(Context context)
	{
		this.context = context;
		File fParent = new File("/sys/class/input");
		if (fParent.exists() && fParent.isDirectory())
		{
			File[] eventFiles = fParent.listFiles(new FileFilter() {
				@Override
				public boolean accept(File pathname) {
					return pathname.isDirectory() && pathname.getAbsolutePath().contains("event");
				}});
			String deviceName = "";
			for (File event : eventFiles)
			{
				String deviceNamePath = event.getAbsolutePath() + "/device/name";
				try {
					
					FileInputStream fr = new FileInputStream(deviceNamePath);
					DataInputStream is = new DataInputStream(fr);
					deviceName = is.readLine().trim().toLowerCase();
					is.close();
					
					if (deviceName.contains("keypad") || deviceName.contains("sii9234_rcp"))
					{
						keypadDeviceName = deviceName;
						keypadDeviceEventName = event.getName();
				    	monitorThread = new Thread(rMonitor);
						break;
					}
				} catch (IOException e) {
				} 
			}
			if (monitorThread == null)
				monitorThread = new Thread(rMonitor);
		}
	}
	public String getKeypadDeviceName()
	{
		return keypadDeviceName;
	}
	public String getKeypadEventDeviceName()
	{
		return keypadDeviceEventName;
	}
	private String generateLaunchParam()
	{
		return "getevent /dev/input/" + keypadDeviceEventName;
	}
    Runnable rMonitor = new Runnable() {
		@Override
		public void run() {
			startProcess();
		}
	};
	
	private String createScriptFile()
	{
		try
		{
    	String newFileName = "/data/data/" + context.getPackageName() + "/volscript";
		File myFile = new File(newFileName);
		myFile.createNewFile();
		FileOutputStream fOut = new FileOutputStream(myFile);
		OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
		myOutWriter.append(generateLaunchParam() + " \n");
		myOutWriter.flush();
		myOutWriter.close();
		return newFileName;
		}
		catch(Exception e)
		{
			return null;
		}
	}
	
	@SuppressLint("NewApi")
	private static String copyFile(String filename, Context context) {
	    AssetManager assetManager = context.getAssets();
	
	    InputStream in = null;
	    OutputStream out = null;
	    try {
	        in = assetManager.open(filename);
	        String newFileName = "/data/data/" + context.getPackageName() + "/" + filename;
	        out = new FileOutputStream(newFileName);
	
	        byte[] buffer = new byte[1024];
	        int read;
	        while ((read = in.read(buffer)) != -1) {
	            out.write(buffer, 0, read);
	        }
	        in.close();
	        in = null;
	        out.flush();
	        out.close();
	        out = null;

	    	try
	    	{
	    		if (android.os.Build.VERSION.SDK_INT >= 9)
	    		{
	    			File f = new File (newFileName);
	    			f.setExecutable(true, true);
	    		}
	    	}
	    	catch(Exception ex)
	    	{
	    		
	    	}
	        return newFileName;
	    } catch (Exception e) {
	        Logger.Log(context, e);
	        return null;
	    }
	}
    private void startProcess()
    {
    	if (this.compatibilityMode)
    	{
    		try {
    			String[] cmd = new String[] {"su","-c","getevent","-q"};
		    	geteventProc = Runtime.getRuntime().exec(cmd);
		    	os = new DataOutputStream(geteventProc.getOutputStream());
		    	bufferedReader = new BufferedReader(new InputStreamReader(geteventProc.getInputStream()));
		    	while (((geteventLine = bufferedReader.readLine()) != null) && isEnabled) {
		    		//if ((this.keypadDeviceEventName == null) || geteventLine.contains("/input/" + this.keypadDeviceEventName))
		    		//{
			    	 if (geteventLine.contains("0001 0073 00000001")) //VOLUME UP PRESSED
			    		 keyDetected(VOLUME_UP);
			    	 else if (geteventLine.contains("0001 0072 00000001")) //VOLUME DOWN PRESSED
			    		 keyDetected(VOLUME_DOWN);
		    		//}
			    }
			    os.close();
			    bufferedReader.close();
			    geteventProc.destroy();
			 } catch (IOException e) {}
    	}
    	else
    	{
	    	try {
	    		//first, copy volkey detector native image file
		    	String nativePath = copyFile("volkey", context);
		    	//execute volkey file
		    	//String[] cmd = {"su","-c",nativePath};
		    	geteventProc = Runtime.getRuntime().exec("su");
		    	os = new DataOutputStream(geteventProc.getOutputStream());
				os.writeBytes("chmod 755 " + nativePath + "\n");
				os.writeBytes(nativePath + "\n");
				os.flush();
		    	bufferedReader = new BufferedReader(new InputStreamReader(geteventProc.getInputStream()));
		    	while (((geteventLine = bufferedReader.readLine()) != null) && isEnabled) {
			    	 if (geteventLine.contains("0001 0073 00000001")) //VOLUME UP PRESSED
			    		 keyDetected(VOLUME_UP);
			    	 else if (geteventLine.contains("0001 0072 00000001")) //VOLUME DOWN PRESSED
			    		 keyDetected(VOLUME_DOWN);
			    }
			    os.close();
			    bufferedReader.close();
			    geteventProc.destroy();
			 } catch (IOException e) {}
    	}
    }

    void setOnVolumeKeyDetectedHandler(OnVolumeKeyDetectedHandler handler)
    {
    	vkeyhandler = handler;   	
    }
    private void keyDetected(char key)
    {
    	if (vkeyhandler != null && isEnabled)
    	{
    		vkeyhandler.OnVolumeKeyDetected(key);
    	}
    }
    interface OnVolumeKeyDetectedHandler {
    	void OnVolumeKeyDetected(char key);
    }
    
    public void start()
    {
    	isEnabled = true;
		if (!monitorThread.isAlive())
    			monitorThread.start();
    }
    public void stop()
    {
    	isEnabled = false;
    	if (monitorThread.isAlive()) {
     		try {
     			os.writeBytes("e\n");
     			if (this.compatibilityMode)
     				geteventProc.destroy();
			} catch (Exception e) {
			}
			monitorThread.stop();
     	}
    }
}

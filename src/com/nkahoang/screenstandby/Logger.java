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

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.zip.GZIPOutputStream;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

import android.app.ProgressDialog;
public class Logger {
	static SharedPreferences prefs = null;  
    private static ProgressDialog progressDialog = null;
    private static PackageInfo pInfo;
    
	public static void Log(Context context, Intent i)
	{
		 if (prefs == null)
			 prefs = PreferenceManager.getDefaultSharedPreferences(context);

		 if (prefs.getBoolean("enableextensivelog", false))
		 {
			 String Message = i.getAction();
			 if (i.getExtras() != null)
			 {
				 Message += "  -Extra: " + i.getExtras().size() + " |";
				 for(String key : i.getExtras().keySet())
					 Message += "[" + key + "]:" + i.getExtras().get(key).toString() + "\n";
			 }
			 Log(context,Message);
		 }
	}
	public static void Log(Context context, Throwable ee)
	{
		 if (prefs == null)
			 prefs = PreferenceManager.getDefaultSharedPreferences(context);

		 if (prefs.getBoolean("enableextensivelog", false))
		 {
			 try
			 {
				String Message = "";
				Message += ee.getLocalizedMessage() + " ";
				Message += ee.getMessage() + "\n";
				for(StackTraceElement e: ee.getStackTrace())
				{
					Message += e.getClassName() + " ";
					Message += e.getFileName() + " ";
					Message += e.getMethodName() + "():";
					Message += e.getLineNumber() + "\n";
				}
				Log(context,Message);
			 }
			 catch(Exception ex) {}
			 try
			 {
				 java.io.StringWriter errMessage = new StringWriter();
			     PrintWriter pw = new PrintWriter(errMessage);
		         ee.printStackTrace(pw);
		         Log(context,"--------");
		         Log(context,errMessage.toString());
			 }
			 catch(Exception ex) {}
		 }
	}
	public static void Log(Context context, String message)
	{
		 if (prefs == null)
			 prefs = PreferenceManager.getDefaultSharedPreferences(context);
		 
		 if (prefs.getBoolean("enableextensivelog", false))
		 {
			 java.util.Date d = Calendar.getInstance().getTime();
			 message = "[" + d.getHours() + ":" + 
					 d.getMinutes() + ":" + 
					 d.getSeconds() + "]:" + message;
			 String log = prefs.getString("extendedlog", "");
			 log += "\n" + message;
			 prefs.edit().putString("extendedlog", log).commit();
		 }
	}
	static int GetLogLength(Context context)
	{
		 if (prefs == null)
			 prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String log = prefs.getString("extendedlog", "");
		return log.getBytes().length;
	}

	public static void ShowLog(Context context)
	{
        //Display the progress dialog
		try
		{
			pInfo = context.getPackageManager().getPackageInfo( context.getPackageName(), 0);
		}
		catch(Exception ex) {
			Log(context, ex);
		}
        progressDialog = new ProgressDialog(context);    
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER | ProgressDialog.THEME_HOLO_DARK);
        progressDialog.setTitle("Saving debug info");  
        progressDialog.setMessage("Collecting data. Please wait...\n(no private info will be collected)");
        progressDialog.setCancelable(false);
        progressDialog.setIcon(R.drawable.debuggingico);
        progressDialog.setIndeterminate(true);
        progressDialog.show();
        new LoadViewTask(context).execute();
	}
	

    private static class LoadViewTask extends AsyncTask<Void, Integer, Void>  
    {
    	private Context context;
    	public LoadViewTask(Context context)
    	{
    		this.context = context;
    	}
    	private Boolean Success = false;
    	private String fileName = "";
		@Override
		protected Void doInBackground(Void... params) {
			try {
				fileName = logDebug(context);
				Success = true;
			} catch (Exception e) {
			}
			return null;
		}
		  
        //after executing the code in the thread  
        @Override  
        protected void onPostExecute(Void result)  
        {  
            //close the progress dialog  
            progressDialog.dismiss();
            if (Success) {
            	AlertDialog.Builder builder = new AlertDialog.Builder(context);
				builder.setTitle("Log saved");
				builder.setIcon(android.R.drawable.ic_menu_send);
				builder.setMessage("Debug log saved at\n\""+fileName+"\"\n\nDo you want to send log to developer email?")
				.setNegativeButton("No thank", dialogClickListener).setPositiveButton("Send", dialogClickListener).show();
            }
            else
				Toast.makeText(context, "Error saving debug log (did you allow root access?)", Toast.LENGTH_SHORT).show();
        }  	

    	DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
    	    @Override
    	    public void onClick(DialogInterface dialog, int which) {
    	        switch (which){
    	        case DialogInterface.BUTTON_POSITIVE:
    	            Intent intent = new Intent(Intent.ACTION_SEND);
    	            intent.setType("text/plain");
    	            intent.putExtra(Intent.EXTRA_EMAIL, new String[] {"android@nkahnt.com"});
    	            intent.putExtra(Intent.EXTRA_SUBJECT, "Screen Standby # Root (" + pInfo.versionName + ") Log Submission");
    	            intent.putExtra(Intent.EXTRA_TEXT, android.os.Build.MODEL + " " + android.os.Build.PRODUCT + " " + android.os.Build.ID + "\n" +
    	            android.os.Build.HOST + android.os.Build.TIME);
    	            File file = new File(fileName);
    	            if (!file.exists() || !file.canRead()) {
    	                Toast.makeText(context, "Attachment Error. Cannot attach file", Toast.LENGTH_SHORT).show();
    	            }
    	            Uri uri = Uri.parse("file://" + fileName);
    	            intent.putExtra(Intent.EXTRA_STREAM, uri);
    	            context.startActivity(Intent.createChooser(intent, "Send log using"));
    	            break;

    	        case DialogInterface.BUTTON_NEGATIVE:
    	            dialog.dismiss();
    	            break;
    	        }
    	    }
    	};
    }

	@SuppressLint("SimpleDateFormat")
	private final static String getDateTime()  
	{  
	    SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd-hhmmss");
	    return df.format(Calendar.getInstance().getTime());  
	}
	

	private static Process proc;
	private static DataOutputStream os;
	private static DataInputStream is;
	@SuppressLint("NewApi")
	public static String logDebug(Context context) throws Exception
	{
		String logfile = Environment.getExternalStorageDirectory().getPath() + "/screenstandby"+getDateTime()+".log.gz";
		if (proc == null)
		{
			proc = Runtime.getRuntime().exec("sh");
		    os = new DataOutputStream(proc.getOutputStream());
		    is = new DataInputStream(proc.getInputStream());
		}
		os.writeBytes("find /sys/devices\n");
		os.writeBytes("find /sys/class\n");
		os.writeBytes("ls /sys/class/backlight\n");
		os.writeBytes("ls /dev/graphics\n");
		os.writeBytes("ls /sys/class/backlight/*/brightness\n");
		os.writeBytes("ls /sys/class/fb0\n");
		os.writeBytes("ps\n");
		os.writeBytes("exit \n");
		InputStreamReader isr = new InputStreamReader(is);
	    BufferedReader br = new BufferedReader(isr);
		String line = null;

		File myFile = new File(logfile);
		myFile.createNewFile();
		FileOutputStream fOut = new FileOutputStream(myFile);
		GZIPOutputStream gos = new GZIPOutputStream(fOut);
		OutputStreamWriter myOutWriter = 
								new OutputStreamWriter(gos);
		myOutWriter.append("MODEL: " + android.os.Build.MODEL + "\n");
		myOutWriter.append("MANUFACTURER: " + android.os.Build.MANUFACTURER + "\n");
		myOutWriter.append("BRAND: " + android.os.Build.BRAND + "\n");
		myOutWriter.append("DISPLAY: " + android.os.Build.DISPLAY + "\n");
		myOutWriter.append("#Display information: \n");
		Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		myOutWriter.append("-W:" + display.getWidth());
		myOutWriter.append("-H:" + display.getHeight());
		myOutWriter.append("-RRa:" + display.getRefreshRate());
		myOutWriter.append("-PF:" + display.getPixelFormat());
		myOutWriter.append("-O:" + display.getOrientation() + "\n");
		try
		{
			myOutWriter.append("HARDWARE: " + android.os.Build.HARDWARE + "\n");
		}
		catch(Exception ex){}
		myOutWriter.append("CPU_ABI: " + android.os.Build.CPU_ABI + "\n");
		myOutWriter.append("PRODUCT: " + android.os.Build.PRODUCT + "\n\n");
		myOutWriter.append("VERSION: APP " + pInfo.versionName + " (Full) on DEVICE " + android.os.Build.DEVICE + "\n");

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		Map<String,?> prefsMap = prefs.getAll();
		SensorManager mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		myOutWriter.append("\n#SENSORS: \n");
		try
		{
			Sensor proximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
			if (proximity == null)
				myOutWriter.append("PROXIMITY SENSORS: null\n");
			else
			{
				myOutWriter.append("PROXIMITY SENSORS: found " + proximity.getName() + "\n");
				myOutWriter.append("           ranges: " + proximity.getMaximumRange() + "\n");
				myOutWriter.append("            power: " + proximity.getPower() + "\n");
				myOutWriter.append("        esolution: " + proximity.getResolution() + "\n");
			}
		}
		catch(Exception ex)
		{
			myOutWriter.append("PROXIMITY SENSORS: null\n");
		}
		try
		{
			Sensor proximity = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
			if (proximity == null)
				myOutWriter.append("PROXIMITY SENSORS: null\n");
			else
			{
				myOutWriter.append("PROXIMITY SENSORS: found " + proximity.getName() + "\n");
				myOutWriter.append("           ranges: " + proximity.getMaximumRange() + "\n");
				myOutWriter.append("            power: " + proximity.getPower() + "\n");
				myOutWriter.append("        esolution: " + proximity.getResolution() + "\n");
			}
		}
		catch(Exception ex)
		{
			myOutWriter.append("PROXIMITY SENSORS: null\n");
		}

        //for volume key detector
        VolumeKeyDetector vkd = new VolumeKeyDetector(context);
        myOutWriter.append("VOLUME KEY DETECTOR\n");
        myOutWriter.append("Keypad device: " + vkd.getKeypadDeviceName() + "\n");
        myOutWriter.append("Keypad event device: " + vkd.getKeypadEventDeviceName() + "\n");
        
		myOutWriter.append("\n#SELECTED PREFERENCES: \n");
        for(Map.Entry<String,?> entry : prefsMap.entrySet())
        {
            myOutWriter.append(entry.getKey() + ": " + entry.getValue().toString() + "\n");            
        }

		myOutWriter.append("\nDEVICE LISTS");
		myOutWriter.append("---- /sys/devices list:\n /*this should not be blank. If it is, try to check if you enabled root)*/");
		while ((line = br.readLine()) != null)
		{
			myOutWriter.append(line+"\n");
		}
		
		myOutWriter.close();
		gos.close();
		fOut.close();
		proc.destroy();
		proc = null;
		return logfile;
	}
}

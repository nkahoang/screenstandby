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
import java.io.IOException;
import java.io.InputStreamReader;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.CheckBox;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Typeface;

public class ManualBrightnessChangerActivity extends BaseActivity {
	private ProgressDialog progressDialog;
	private Process proc;
	private DataOutputStream os;
	private DataInputStream is;
	private String brightnessPath = "";
	private SeekBar brightnessSlider;
	private ImageButton btnDarker;
	private ImageButton btnLighter;
	private CheckBox chkAutoBrightness;
	private Boolean mFastBrightnessmethod;
	private Button btnHomescreenShortcut;
	private int mOldBrightness_Sys;
	
	@Override
	protected void onNewIntent(Intent intent) {
		if (getIntent().getBooleanExtra("FromShortcut", false)) {
        	try {
            	brightnessPath = getIntent().getStringExtra("Devices");
            	mOldBrightness_Sys = getIntent().getIntExtra("Brightness", mOldBrightness_Sys);
				setBrightness(mOldBrightness_Sys);
			} catch (Exception e) {
				Logger.Log(this, e);
			}
        	this.finish();
        }
		else
			super.onNewIntent(intent);
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
     
        if (getIntent().getBooleanExtra("FromShortcut", false)) {
        	try {
            	brightnessPath = getIntent().getStringExtra("Devices");
            	mOldBrightness_Sys = getIntent().getIntExtra("Brightness", mOldBrightness_Sys);
				setBrightness(mOldBrightness_Sys);
			} catch (Exception e) {
				Logger.Log(this, e);
			}
        	this.finish();
        }
        else
        {
        setContentView(R.layout.activity_manual_brightness_changer);
    	//STATUSBAR
        Typeface typefaceLight = FontManager.getThinFont(this);
        Typeface typeface = FontManager.getCondensedFont(this);
        
    	((TextView)this.findViewById(R.id.txtManualBrightness)).setTypeface(typefaceLight);
        mFastBrightnessmethod = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("brightnessmethod_fast", false);
        LoadDevices();
        
        brightnessSlider = (SeekBar) this.findViewById(R.id.seekBrightness);
        btnDarker = (ImageButton) this.findViewById(R.id.btnDarker);
        btnLighter = (ImageButton) this.findViewById(R.id.btnLighter);
        btnHomescreenShortcut = (Button) this.findViewById(R.id.btnHomescreenShortcut);
        chkAutoBrightness = (CheckBox) this.findViewById(R.id.chkAutoBrightness);
        chkAutoBrightness.setChecked(
        		Settings.System.getInt(this.getContentResolver(), 
				android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE,
				android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) == 
				android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
        
        chkAutoBrightness.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				Settings.System.putInt(ManualBrightnessChangerActivity.this.getContentResolver(), "screen_brightness_mode", chkAutoBrightness.isChecked()?
						android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC:
						android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
				WindowManager.LayoutParams localLayoutParams = ManualBrightnessChangerActivity.this.getWindow().getAttributes();
		        localLayoutParams.screenBrightness = (brightnessSlider.getProgress() / 255.0F);
		        ManualBrightnessChangerActivity.this.getWindow().setAttributes(localLayoutParams);
			      /*
				Settings.System.putInt(ManualBrightnessChangerActivity.this.getContentResolver(), 
						android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE, 
						chkAutoBrightness.isChecked()?
								android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC:
								android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
				*/
			}});
        btnHomescreenShortcut.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent serviceIntent = new Intent(Intent.ACTION_MAIN);
		        ComponentName name = new ComponentName(getPackageName(), ".ManualBrightnessChangerActivity");
		        serviceIntent.setComponent(name);
		        serviceIntent.putExtra("FromShortcut", true);
		        serviceIntent.putExtra("Devices", brightnessPath);
		        serviceIntent.putExtra("Brightness", brightnessSlider.getProgress());
		        Intent shortcutintent = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
		        shortcutintent.putExtra("duplicate", true);
		        shortcutintent.putExtra(Intent.EXTRA_SHORTCUT_NAME, (brightnessSlider.getProgress() * 100 / 255) + "% Brightness");
		        Parcelable icon = Intent.ShortcutIconResource.fromContext(getApplicationContext(), R.drawable.brightnessico);
		        shortcutintent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon);
		        shortcutintent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, serviceIntent);
		        sendBroadcast(shortcutintent);
			}});
        btnDarker.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mOldBrightness_Sys -= 20;
				if (mOldBrightness_Sys <= 0) mOldBrightness_Sys = 0;
				brightnessSlider.setProgress(mOldBrightness_Sys);
				try {
					setBrightness(mOldBrightness_Sys);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}});
        btnLighter.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mOldBrightness_Sys += 20;
				if (mOldBrightness_Sys > 255) mOldBrightness_Sys = 255;
				brightnessSlider.setProgress(mOldBrightness_Sys);
				try {
					setBrightness(mOldBrightness_Sys);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}});
        brightnessSlider.setProgress(mOldBrightness_Sys);
        brightnessSlider.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				try {
					if (seekBar.getProgress() != mOldBrightness_Sys)
						mOldBrightness_Sys = seekBar.getProgress();
					setBrightness(mOldBrightness_Sys);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}});
        }
    }
    
    private int getCurrentBrightness() throws IOException
    {
    	int currentBrightness = 0;
    	if (proc == null)
		{
			proc = Runtime.getRuntime().exec("su");
		    os = new DataOutputStream(proc.getOutputStream());
		    is = new DataInputStream(proc.getInputStream());
		}	
    	
    	InputStreamReader isr = new InputStreamReader(is);
	    BufferedReader br = new BufferedReader(isr);
		String line = null;
		
		for(String s: brightnessPath.split("\\r?\\n"))
		{
			if (s.trim().length() > 0) 
				os.writeBytes("cat " + s.trim() + "\n");
		}
		
		os.writeBytes("echo end\n");
		try
		{
		while ((line = br.readLine()) != null)
		{ 
			if (line.trim().length() == 0 || line.trim().equals("end")) break;
			int b = Integer.parseInt(line.trim());
			if (b > currentBrightness)
				currentBrightness = b;
		}
		}
		catch(Exception ex){}
		
		return currentBrightness;
    }
    private void LoadDevices()
    {
    	progressDialog = new ProgressDialog(this);    
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER | ProgressDialog.THEME_HOLO_DARK);
        progressDialog.setTitle("Loading devices");  
        progressDialog.setMessage("Loading brightness devices. Please wait");
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);
        try {
	        //Display the progress dialog
	        progressDialog.show();
	        new LoadViewTask().execute();
		} catch (Exception e) {
		}
    }
    
    private class LoadViewTask extends AsyncTask<Void, Integer, Void>  
    {
		@Override
		protected Void doInBackground(Void... params) {
			try
			{
				brightnessPath = getBrightnessPath();
			}
			catch(Exception ex){}
			return null;
		}
		  
        //after executing the code in the thread  
        @Override  
        protected void onPostExecute(Void result)  
        {  
            try {
    			mOldBrightness_Sys = getCurrentBrightness();
    		} catch (IOException e1) {
    			// TODO Auto-generated catch block
    			mOldBrightness_Sys = android.provider.Settings.System.getInt(getContentResolver(),android.provider.Settings.System.SCREEN_BRIGHTNESS, 75);
    		}
    		if (mOldBrightness_Sys > 255) mOldBrightness_Sys = 255;
    		if (mOldBrightness_Sys <= 0) mOldBrightness_Sys = 0;
    		brightnessSlider.setProgress(mOldBrightness_Sys);
            progressDialog.dismiss();
        }
    }
    
    private String getBrightnessPath() throws Exception
	{
		String value = "";
		if (value.trim().length() > 0) return value;
		if (proc == null)
		{
			proc = Runtime.getRuntime().exec("su");
		    os = new DataOutputStream(proc.getOutputStream());
		    is = new DataInputStream(proc.getInputStream());
		}
		
		if (mFastBrightnessmethod)
			os.writeBytes("ls /sys/class/backlight/*/brightness\n"); //fast, use LS
		else
			os.writeBytes("find /sys/devices -name 'brightness' \n"); //slow, use BUSYBOX
		
		os.writeBytes("echo end\n");
			
		InputStreamReader isr = new InputStreamReader(is);
	    BufferedReader br = new BufferedReader(isr);
		String line = null;
		while ((line = br.readLine()) != null)
		{ 
			if (line.trim().length() == 0 || line.trim().equals("end")) break;
			if (line.toLowerCase().contains("backlight") && (!line.toLowerCase().contains("button")))
				value = value + line + "\n";
		}
		return value;
	}
    
    private void setBrightness(int Value) throws Exception {
		for(String s: brightnessPath.split("\\r?\\n"))
		{
			if (s.trim().length() > 0) doCmds("echo " + Value + " > " + s.trim());
		}
    }
    
    private void doCmds(String cmds) throws Exception {
		if (proc == null)
		{
			proc = Runtime.getRuntime().exec("su");
		    os = new DataOutputStream(proc.getOutputStream());
		    is = new DataInputStream(proc.getInputStream());
		}
		os.writeBytes(cmds+"\n");
	}
}

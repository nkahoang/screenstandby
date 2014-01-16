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
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

@SuppressLint("NewApi")
public class StandbyService extends Service{
	private static final Boolean VERBOSE = false;
    public static final String ENABLE_INTENT = "com.nkahoang.screenstandby.action.ENABLE";
    public static final String TOGGLE_INTENT = "com.nkahoang.screenstandby.action.DISABLE";
    
    private WindowManager winman;
    private WindowManager.LayoutParams params, paramsHUD, paramsIdle, paramsfilter;
	private HUDView myview;
	private MouseView mouseview;
	private IdleView idleview;
	private FilterView filterview;
    private SharedPreferences prefs;
    
    static Boolean isRunning = false;
    private int mProximityBehaviour = 0;
    private int mScreenOnBrightnessValue = 70;
    private int mInitialAutoBrightnessMode = 1;
    private int mRestoreBrightness = 72;
    private int mOrientation = 0;
	private int old_phonebrightness;
    private float mTouchpadSpeed = 1f;
    private PowerManager pm;
    private WifiManager wm;
    private PowerManager.WakeLock mWakeLock;
    private WifiLock mWifiLock;
    private Display display;
    private Bitmap bmpCursor;
    private Bitmap bmpScrollH;
    private Bitmap bmpScrollV;
    private ContentResolver resolver;
    private Semaphore mutex = new Semaphore(1);
    private IntentFilter filter;
	private Timer timer = null;
	private TimerTask task = null;
	private Process proc;
	private DataOutputStream os;
	private DataInputStream is;
	private SensorManager mSensorManager;
	private Sensor lightSensor;
	private BroadcastReceiver mReceiver;
	private PhoneReceiver mPhoneReceiver;
	private AudioManager mAudioManager;
    private ComponentName mRemoteControlResponder;
    private VolumeKeyDetector mVolKeyDetector;
    private MediaControlReceiver mediaButtonReceiver;
	String devicePath = "";
	private float mAccel; // acceleration apart from gravity
	private float mAccelCurrent; // current acceleration including gravity
	private float mAccelLast; // last acceleration including gravity
	private float mShakeCount = 0;
	private float mShakeSensitivity = 1.0f;
	private float mZSensitivity = -3f;
	private float mYSensitivity;
	private int MaxWidth, MaxHeight;
	private Date mlast, mlastFace, mlastHold; //last time since successful shake
	private Boolean faceup = true;
    private Handler handler = new Handler();
    private boolean mDetectPhone = true;
	private boolean mDisableTouchScreen = false;
	private boolean mEnableShake = false;
	private boolean mEnableUpsideDown = false;
	private boolean mFBMethod = false;
	private boolean mBrightnessMethod = true;
	private boolean mFastBrightnessmethod = false;
	private boolean mBlMethod = false;
	private boolean mWakeLockEnable = false;
	private boolean mWifiLockEnable = false;
	private boolean mHandleAutoBrightness = true;
	private boolean mEnableFaceDown = false;
	private boolean mEnableLCDPower = false;
	private boolean mEnableLightSensor = false;
	private boolean mEnableButtonBrightness = false;
	private boolean mEnableTouchpad = false;	
	private boolean mEnableTouchpadSwipe = false;
	private boolean mEnableTouchpadSoftkey = true;
	private boolean mSGS3US = false;
	private boolean mOneTwoDimmer = false;
	private boolean mNonRootMethod = false;
	private boolean mSurviveLock = true;
	private boolean mMediaButtonControl = false;
	private boolean mHeadSetMediaButtonControl = true;
	private boolean mHardwareMediaButtonControl = true;
	private boolean mVolumeControl = false;
    private boolean isRemoteControlMode = false;
	private int mVolumeBehaviour = 0;
	//private boolean mOneTwoFullscreen = false;
	private int brightnessvalue = 0;
	private static final int dpaddirection[][] = {{0, 1, 2, 3}, 
												  {3, 2, 0, 1}, 
												  {1, 0, 3, 2}, 
												  {2, 3, 0, 1}};
	private float touchpadwidth;
	private int lastLCDPower = 1;
    private float touchpadheight;
    private int touchpadwidthbound;
    private int touchpadheightbound;
    private float swipearea = 0.1f;
    private float swipesensitivity = 10;
	private OneTwoDimmer autodimmer = null;
	private String fb0path = "/sys/class/graphics/fb0/blank";
	private String lcdpath = "/sys/class/lcd/panel/lcd_power";

	private static final String SCREEN_BRIGHTNESS_MODE = "screen_brightness_mode";
	private static final int SCREEN_BRIGHTNESS_MODE_MANUAL = 0;
	private static final int SCREEN_BRIGHTNESS_MODE_AUTOMATIC = 1;  
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override   
	public void onDestroy() {
		try
		{
			if (mMediaButtonControl)
				mAudioManager.unregisterMediaButtonEventReceiver(mRemoteControlResponder);
			task.cancel();
			timer.cancel();
			mShakeCount = 0;
			try
			{
				if (mReceiver != null)
				this.unregisterReceiver(mReceiver);
			}
			catch(IllegalArgumentException ex) {}
			catch(Exception ex) {if (VERBOSE) Logger.Log(this, ex);}
			
			try
			{
				if (receiver != null)
				this.unregisterReceiver(receiver);
			}
			catch(IllegalArgumentException ex) {}
			catch(Exception ex) {Logger.Log(this, ex);}
			
			try
			{
		    	if (myview != null)
		    		winman.removeView(myview);
		    	if (mouseview != null)
		    		winman.removeView(mouseview);
				}
			catch(Exception ex) {}
			
			if (mEnableShake || mEnableFaceDown || mEnableUpsideDown)
			try
			{
				mSensorManager.unregisterListener(mSensorListener);
			}
			catch(Exception ex) {}
			if (mEnableLightSensor)
				try
				{
					mSensorManager.unregisterListener(mProximitySensorListener);
				}
				catch(Exception ex) {}
				
			brightnessvalue = 0;
			if (isRunning) {
				stopeverything(false);
				isRunning = false;
			}
		}catch(Exception ex)
		{
			Logger.Log(this, ex);
		}
	}
	
    @Override
    public void onCreate() {
        super.onCreate();
        
        resolver = getContentResolver();
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        winman = (WindowManager) getSystemService(WINDOW_SERVICE);
        display = winman.getDefaultDisplay();
        mEnableButtonBrightness = prefs.getBoolean("buttonbrightnessenable", false);
        mBrightnessMethod = prefs.getBoolean("brightnessmethod", true);
        mFastBrightnessmethod = prefs.getBoolean("brightnessmethod_fast", false);
        
		mBlMethod = prefs.getBoolean("blmethod", false);
		mEnableLCDPower = prefs.getBoolean("cutLCDpowermethod", false);
        SpecialDevicesFixes();
        if (mEnableLCDPower) {
        	mEnableButtonBrightness = false;
        	mBrightnessMethod = false;
        	mFastBrightnessmethod = false;
        	mBlMethod = false;
        }
		mOneTwoDimmer = prefs.getBoolean("onetwodimenabling", false);
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));
        
        try {
			if (mBrightnessMethod || mBlMethod) devicePath = getBrightnessPath();
			
			if (devicePath.trim().length() == 0)
			{
				String modelString = android.os.Build.MODEL.toLowerCase(Locale.getDefault());
				if (modelString.contains("nexus s")) devicePath += "/sys/class/backlight/s5p_bl/brightness\n";	
				if (modelString.contains("galaxy nexus")) devicePath += "/sys/devices/omapdss/display0/backlight/s6e8aa0/brightness";
				if (modelString.contains("tf300t")) devicePath += "/sys/class/backlight/pwm-backlight/brightness";
			}
		    mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		    mAccel = 0.00f;
		    mAccelCurrent = SensorManager.GRAVITY_EARTH;
		    mAccelLast = SensorManager.GRAVITY_EARTH;
			Logger.Log(this, "found devices: " + devicePath);
			if (VERBOSE) Logger.Log(this, "End of service creation");
		} catch (Exception e) {
			Logger.Log(this, e);
		}
    }

    private void nonRootEnable()
    {
		winman.addView(filterview, paramsfilter);
		if (VERBOSE) Logger.Log(StandbyService.this, "Filter view added");
		filterview.postInvalidate();
    }
    
    private void nonRootDisable()
    {
    	try
    	{
    	winman.removeView(filterview);
    	}
    	catch(Exception ex) {}
    }
    
    private void setAutoBrightnessEnabled(Boolean IsEnabled)
    {
    	Settings.System.putInt(resolver, SCREEN_BRIGHTNESS_MODE, IsEnabled?SCREEN_BRIGHTNESS_MODE_AUTOMATIC:SCREEN_BRIGHTNESS_MODE_MANUAL); 
    }
    private void setAutoBrightnessEnabled(int Mode)
    {
    	Settings.System.putInt(resolver, SCREEN_BRIGHTNESS_MODE, Mode); 
    }
    
    private void stopeverything()
    {
    	stopeverything(true);
    	if (this.mMediaButtonControl)
        mAudioManager.unregisterMediaButtonEventReceiver(
                mRemoteControlResponder);
    }
    
    private void stopeverything(Boolean stopself)
    {
		Logger.Log(StandbyService.this, "Stopping service");
    	if (isRunning) {
	    	isRunning = false;
	    	try
	    	{
	    		try {
	    			if (mEnableLCDPower) {
	    				lastLCDPower = 1;
	    				doCmds("echo 1 > " + lcdpath);
	    			}
	    		}
	    		catch(Exception ex) {}
	    		try
				{
					if (mOneTwoDimmer)
					{
						autodimmer.Stop();
						winman.removeView(idleview);
					}
				}
				catch(Exception ex) {}
	    		
	    		try
	    		{
					task.cancel();
	    			timer.cancel();
	    			if (!mSGS3US)
	    				mutex.acquire();
					this.stopForeground(true);
	    		}
	    		catch(Exception ex){};
	    		
	    		if (mDisableTouchScreen && myview != null)
	    			winman.removeView(myview);
	    		
	    		try
	    		{
		    		if (mWakeLockEnable)
		    			mWakeLock.release();
	    		}
	    		catch(Exception ex){}
	    		try
	    		{
			        if (mWifiLockEnable)
			        	mWifiLock.release();
	    		}
	    		catch(Exception ex) {}
	    		
	    		try
	    		{
	    			if (mEnableShake || mEnableFaceDown || mEnableUpsideDown)
						mSensorManager.unregisterListener(mSensorListener);
				
					if (mEnableLightSensor)
						mSensorManager.unregisterListener(mProximitySensorListener);
	    		}
	    		catch(Exception ex) {};
	    		
	    		try
	    		{
	    			if (mDetectPhone)
	    				this.unregisterReceiver(mPhoneReceiver);
	    		}
	    		catch(Exception ex){}
	    		
				try {
					this.unregisterReceiver(mReceiver);
				}
				catch(Exception ex){}
				
				try {
					this.unregisterReceiver(receiver);
				}
				catch(Exception ex){}

				try
				{
					if (mVolumeControl) mVolKeyDetector.stop();
				}
				catch(Exception ex){
					Logger.Log(this, ex);
				}
				
				try
				{
					if (mNonRootMethod) Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, mRestoreBrightness);
					if (mInitialAutoBrightnessMode != android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
					{
						if (mHandleAutoBrightness && mBrightnessMethod) {
							setAutoBrightnessEnabled(true);
							if (VERBOSE) Logger.Log(this, "Brightness restored");
						}
					}
				}
				catch(Exception ex) {Logger.Log(this, ex);}
				
				try
				{
					for(String s: devicePath.split("\\r?\\n"))
						if (s.trim().length() > 0) {
							if (s.contains("bl_power"))
							{
								if (mBlMethod) doCmds("echo 0 > " + s.trim());
							}
							else if (mBrightnessMethod)
							{
								doCmds("chmod " + s.trim() + " 666");
								if (VERBOSE) Logger.Log(this,"echo " + mScreenOnBrightnessValue + " > " + s.trim());
							}
						}
				}
				catch(Exception ex){Logger.Log(this, ex);}
				
				try
				{
					if (mEnableTouchpad) {
						HUDNative.destroy();
						if (VERBOSE) Logger.Log(this, "Touchpad driver successfully destroyed");
					}
				}
				catch(Exception ex) {Logger.Log(this, ex);}
				
				if (mFBMethod && stopself)
					doCmds("echo 0 > " + fb0path);
				
				if (mNonRootMethod)
					this.nonRootDisable();
				
				try
				{
					if (myview != null)
		    			winman.removeView(myview);
				}
				catch(Exception ex) {}
				
				try
				{
					if (mouseview != null)
						winman.removeView(mouseview);
				}
				catch(Exception ex) {}
				
				proc=null;
				
				if (stopself) this.stopSelf();
				
	    	}
	    	catch(Exception ex){Logger.Log(this, ex);}
	    	finally{
    			if (!mSGS3US)
	    		mutex.release();
	    	}
    	}
		Logger.Log(this, "Completely stopped self");
    }

    private void SpecialDevicesFixes()
    {
    	String modelString = android.os.Build.MODEL.toLowerCase(Locale.getDefault());
		//SPECIAL DEVICES FIX
		if (modelString.contains("nexus")) //fix for galaxy nexus
			if (!(android.os.Build.MODEL.contains("4"))) //but not Nexus 4
				mFBMethod = false; 
			else
			{
				if (mBrightnessMethod) devicePath = "/sys/devices/platform/msm_fb.525825/leds/lcd-backlight/brightness";
			}
				
		if (modelString.contains("evo")) mFBMethod = false; //fix for HTC EVO
		if (modelString.contains("evita")) mFBMethod = false; //fix for HTC ONE XL
		if (modelString.contains("mt11i")) { //fix for sony mt11i
			mFBMethod = false;
			mFastBrightnessmethod = false;
		}
		if (modelString.contains("tf300t")) mFBMethod = false;
		if (modelString.contains("iconia")) mRestoreBrightness = 65; //fix for iconia tablets
		if ((modelString.contains("atrix")) || (android.os.Build.MODEL.toLowerCase().contains("mb860"))) {
			mFastBrightnessmethod = false;
			fb0path = "/sys/devices/virtual/graphics/fb0/blank";
		}
		if ((modelString.contains("gt-p5113")) || (modelString.contains("mediacom"))) {
			fb0path = "/sys/devices/platform/rk-fb/graphics/fb0/blank";
		}
		
		if ((modelString.contains("sun4i")))
			fb0path = "/sys/devices/platform/disp/graphics/fb0/blank";
    }
    
    private void getConfigurationFromPreferences()
    {
    	mOneTwoDimmer = prefs.getBoolean("onetwodimenabling", false) && (!isRemoteControlMode);
    	mDetectPhone= prefs.getBoolean("detectphoneenable",true);
    	mSurviveLock = (prefs.getBoolean("onetwodimsurvivelock", true) && mOneTwoDimmer);
		mBrightnessMethod = prefs.getBoolean("brightnessmethod", true);
		mFBMethod = prefs.getBoolean("framebuffermethod", true);
		mBlMethod = prefs.getBoolean("blmethod", false);
		mEnableLCDPower = prefs.getBoolean("cutLCDpowermethod", false);
		mWakeLockEnable = prefs.getBoolean("wakelockenable", false);
		mHandleAutoBrightness = prefs.getBoolean("handleautobrightness", true);
		mNonRootMethod = prefs.getBoolean("nonrootmethod", false);
		mEnableButtonBrightness = prefs.getBoolean("buttonbrightnessenable", false);
		mEnableShake = prefs.getBoolean("shakeenabling", true);
		mEnableFaceDown = prefs.getBoolean("facedownenabling", false);
		mEnableUpsideDown = prefs.getBoolean("upsidedownenabling", false);
		mEnableLightSensor = prefs.getBoolean("proximityenabling", false);
		mEnableTouchpad = prefs.getBoolean("enabletouchpad", false) && (!isRemoteControlMode);
		mEnableTouchpadSwipe = prefs.getBoolean("touchpadscrolling", false);
		mEnableTouchpadSoftkey = prefs.getBoolean("touchpadsoftkey", true);
		swipearea = Float.parseFloat(prefs.getString("touchpadscrollingarea", "0.1"));
		swipesensitivity = Float.parseFloat(prefs.getString("touchpadscrollingsensitivity", "10"));
		mSGS3US = prefs.getBoolean("voltagemethod", false);
		mDisableTouchScreen = prefs.getBoolean("disabletouchscreen", false);
		mWifiLockEnable = prefs.getBoolean("wifilockenable", false);
		mMediaButtonControl = prefs.getBoolean("usemediabutton", false);
		mVolumeControl = prefs.getBoolean("usevolumekey", false);
		mVolumeBehaviour = Integer.parseInt(prefs.getString("volumekeybehaviour", "0"));
		SpecialDevicesFixes();
		if (mEnableLCDPower) {
        	mEnableButtonBrightness = false;
        	mBrightnessMethod = false;
        	mFastBrightnessmethod = false;
        	mBlMethod = false;
        }
    }
    
	@Override
	public int onStartCommand(Intent intent, int flags, int startid) {
		if (VERBOSE) Logger.Log(this, "Service start"); Logger.Log(this, intent);
		
		if (!isRunning) isRunning = true;
		else return START_STICKY_COMPATIBILITY;
		
		isRemoteControlMode = intent.getBooleanExtra("remotecontrol", false);
		
		getConfigurationFromPreferences();
		
		brightnessvalue = 0;
		pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wm  = (WifiManager) getSystemService(Context.WIFI_SERVICE);

		lastLCDPower = 1;
    	mOrientation = display.getRotation();
    	MaxWidth = display.getWidth();
    	MaxHeight = display.getHeight();
    	
		if (mNonRootMethod) {
			paramsfilter = new WindowManager.LayoutParams(
	                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
	                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
	                PixelFormat.RGBA_8888);
			paramsfilter.width = Math.max(MaxWidth, MaxHeight) + 50;
			paramsfilter.height = paramsfilter.width;
	        paramsfilter.screenBrightness = 25;
			paramsfilter.gravity = Gravity.TOP;
			paramsfilter.setTitle("Filter");
			filterview = new FilterView(this);
		}

		if (flags == START_FLAG_REDELIVERY)return START_STICKY_COMPATIBILITY;
		
		try {
			float systemBrightness = Settings.System.getFloat(getContentResolver(),android.provider.Settings.System.SCREEN_BRIGHTNESS, 1f);
			mRestoreBrightness = (int)systemBrightness;
			
			try {
				mScreenOnBrightnessValue = getCurrentBrightness();
    		} catch (IOException e1) {
    			mScreenOnBrightnessValue = mRestoreBrightness;
    		}
			
			if (mScreenOnBrightnessValue > 255) mScreenOnBrightnessValue = 255;
			if (mScreenOnBrightnessValue <= 0) mScreenOnBrightnessValue = 10;
			if ((mHandleAutoBrightness && mBrightnessMethod) || mNonRootMethod) {
				mInitialAutoBrightnessMode = Settings.System.getInt(resolver, android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE);
				if (mInitialAutoBrightnessMode != Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
				{
					setAutoBrightnessEnabled(false);
					if (mNonRootMethod) Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, 0);
				}
				if (VERBOSE) Logger.Log(this, "Captured old brightness: " + mScreenOnBrightnessValue + " Mode: " + mInitialAutoBrightnessMode);
			}
		} catch (Exception ex) { 
			Logger.Log(this, ex);
		}
        
		if (mEnableShake || mEnableFaceDown || mEnableUpsideDown)
		{
			try
			{
				mShakeCount = 0;
				mlast = Calendar.getInstance().getTime();
				mlastFace = Calendar.getInstance().getTime();
				mlastHold = Calendar.getInstance().getTime();
				mShakeSensitivity = Integer.parseInt(prefs.getString("shakesensitivity", "5")) / 5f;
				mZSensitivity = Integer.parseInt(prefs.getString("zaxissensitivity", "-3"));
				mYSensitivity = Integer.parseInt(prefs.getString("yaxissensitivity", "-3"));
				mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
				if (VERBOSE) Logger.Log(this, "Accelerometer sensor initialized");
			}
			catch(Exception ex)
			{
				Logger.Log(this, ex);
				Log.e("Standby", "No accelerometer sensor found");
			}
		}
		
		if (mEnableLightSensor)
		{
			try
			{
				mProximityBehaviour = Integer.parseInt(prefs.getString("proximitybehaviour", "0"));
				lightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
				mSensorManager.registerListener(mProximitySensorListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
				if (VERBOSE) Logger.Log(this, "Proximity sensor initialized");
			}
			catch(Exception ex)
			{
				Logger.Log(this, ex);
				Log.e("Standby", "No proximity sensor found");
			}
		}
		
		if (mEnableTouchpad) {
			if (VERBOSE) Logger.Log(this, "Initializing touchpad Driver");
			if (prefs.getBoolean("easterpointer", false))
				bmpCursor = BitmapFactory.decodeResource(getResources(),R.drawable.mousea);
			else
				bmpCursor = BitmapFactory.decodeResource(getResources(),prefs.getBoolean("touchpadinvertcolor", true)?R.drawable.mousei:R.drawable.mouse);
	        bmpScrollH = BitmapFactory.decodeResource(getResources(),R.drawable.mousesh);
	        bmpScrollV = BitmapFactory.decodeResource(getResources(),R.drawable.mousesv);
			mTouchpadSpeed = Float.parseFloat(prefs.getString("touchpadspeed", "1"));
			mouseview = new MouseView(this);
			mouseview.setDrawable(mEnableTouchpad);
			params = new WindowManager.LayoutParams(
	                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
	                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
	                PixelFormat.TRANSLUCENT);
	        mouseview.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
	        params.width = MaxWidth;
	        params.height = MaxHeight;
	        params.gravity = Gravity.TOP;
	        params.setTitle("MOUSE");
	        try
	        {
	        	winman.addView(mouseview, params);
	        	if (VERBOSE) Logger.Log(this, "Mouse added");
	        	mouseview.postInvalidate();
				HUDNative.initialize(this);
				if (VERBOSE) Logger.Log(this, "Touchpad driver initialized");
	        }
	        catch(Exception ex)
	        {
	        	Logger.Log(this, ex);
	        }
		}
		
		if (mMediaButtonControl)
		{
			mHeadSetMediaButtonControl = prefs.getBoolean("useheadsethook", true);
			mHardwareMediaButtonControl = prefs.getBoolean("usehardwaremediabutton", true);
			mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
	        mRemoteControlResponder = new ComponentName(getPackageName(),
	                MediaButtonControlReceiver.class.getName());
	        mAudioManager.registerMediaButtonEventReceiver(mRemoteControlResponder);
	        mediaButtonReceiver = new MediaControlReceiver(); 
	        MediaButtonControlReceiver.RegisterMediaButtonControlReceiver(mediaButtonReceiver);
		}
		
		if (mVolumeControl)
		{
			mVolKeyDetector = new VolumeKeyDetector(this);
			mVolKeyDetector.setCompatibilityMode(prefs.getBoolean("usevolumekeycompatibility", false));
			mVolKeyDetector.setOnVolumeKeyDetectedHandler(this.volKeyDetectorListener);
			mVolKeyDetector.start();
		}
		
		if (mDisableTouchScreen || mEnableTouchpad)
		{
			if (VERBOSE) Logger.Log(this, "Disabling touch screen");
			myview = new HUDView(this);

	        paramsHUD = new WindowManager.LayoutParams(
	                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
	                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
	                PixelFormat.TRANSLUCENT);
	        
	        paramsHUD.width = MaxWidth;
	        paramsHUD.height = MaxHeight;
	        paramsHUD.gravity = Gravity.TOP;

			setTouchpadSize();
			
			if (mEnableTouchpadSwipe && mEnableTouchpad)
				HUDNative.injectMouseEvent(2, 0, 0);
			
	        paramsHUD.setTitle("HUD");
	        try
	        {
	        	winman.addView(myview, paramsHUD);
	        	if (VERBOSE) Logger.Log(this, "Touchscreen disabled");
	        }
	        catch(Exception ex)
	        {
	        	Logger.Log(this, ex);
	        }

		}

		if (mWakeLockEnable)
		{
			mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "SCREENSTANDBY_WAKELOCK");
	    	mWakeLock.acquire();
	    	if (VERBOSE) Logger.Log(this, "Wake lock acquired");
		}
		
		if (mWifiLockEnable)
		{
			mWifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL , "SCREENSTANDBY_WIFILOCK");
			mWifiLock.acquire();
			if (VERBOSE) Logger.Log(this, "Wifi lock acquired");
		}
		
		Toast.makeText(this, mOneTwoDimmer?"One two dim mode":"Screen standby mode", Toast.LENGTH_LONG).show();
		try
		{
	        // REGISTER RECEIVER THAT HANDLES SCREEN ON AND SCREEN OFF LOGIC
	        filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
	        if (!this.mSGS3US)
	        {
	        	filter.addAction(Intent.ACTION_SCREEN_OFF);
	        	//filter.addAction(Intent.ACTION_USER_PRESENT);
	        	if (VERBOSE) Logger.Log(this, "Added filter");
	        }
	        if (mDetectPhone) {
	        	mPhoneReceiver = new PhoneReceiver();
	        	IntentFilter filterPhone = new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
	        	registerReceiver (mPhoneReceiver, filterPhone);
	        }
	        mReceiver = new ScreenReceiver();
	        registerReceiver(mReceiver, filter);
        	IntentFilter filter2 = new IntentFilter();
        	filter2.addAction(TOGGLE_INTENT);
            registerReceiver(receiver, filter2);
            Intent serviceIntent = new Intent();
            serviceIntent.setAction(TOGGLE_INTENT);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, serviceIntent, PendingIntent.FLAG_ONE_SHOT);
            Notification notify = new Notification();
            notify.icon = mOneTwoDimmer?R.drawable.onetwodim:R.drawable.ic_launcher;
            notify.setLatestEventInfo(this, mOneTwoDimmer?"One two dim mode":"Screen standby is active", mOneTwoDimmer?"Click here to disable one two dim mode":"Click here to disable screen standby", pendingIntent);
            this.startForeground(R.string.app_name, notify);

		}
		catch(Exception ex)
		{
			Logger.Log(this, ex);
		}

		if (mOneTwoDimmer)
		{
			proc = null;
			paramsIdle = new WindowManager.LayoutParams(
	                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
	                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
	                PixelFormat.TRANSPARENT);
				        
	        paramsIdle.width = 0;
	        paramsIdle.height = 0;
	        
	        paramsIdle.gravity = Gravity.TOP;

			idleview = new IdleView(this);
			paramsIdle.setTitle("IDLE");
	        try
	        {
	        	winman.addView(idleview, paramsIdle);
	        	if (VERBOSE) Logger.Log(this, "Idle window added");
	        }
	        catch(Exception ex)
	        {
	        	Logger.Log(this, ex);
	        }
			autodimmer = new OneTwoDimmer(this, autodimmerhandler);
			autodimmer.Start();
		}
		else
		{
			if (mNonRootMethod) this.nonRootEnable();
			proc = null;
			root_set_framebuffer_blank();
			proc = null;
			
			Thread t = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						root_set_device_brightness();
						proc = null;
						timer = new Timer();
						task = new TimerTask() {
					        @Override
					        public void run() {
					        	if (!isRunning) stopeverything();
					        	root_set_device_brightness();
					        }
					    };
					    int mCustomRefreshRate = prefs.getInt("mCustomRefreshRate", 4000);
					    timer.scheduleAtFixedRate(task, mBrightnessMethod?mCustomRefreshRate:8000, mBrightnessMethod?mCustomRefreshRate:8000);
					    Logger.Log(StandbyService.this, "Thread started successfully");
					} catch (Exception ex) {
						Logger.Log(StandbyService.this, ex);
					}
					
				}});
				t.start();
		}
		return START_STICKY_COMPATIBILITY;
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
		
		for(String s: devicePath.split("\\r?\\n"))
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
	private String getBrightnessPath() throws Exception
	{
		String value = prefs.getString("brightnessdevices", "");
		if (value.trim().length() > 0) return value;
		if (proc == null)
		{
			proc = Runtime.getRuntime().exec("su");
		    os = new DataOutputStream(proc.getOutputStream());
		    is = new DataInputStream(proc.getInputStream());
		}
		if (mBrightnessMethod)
		{
			if (mFastBrightnessmethod) os.writeBytes("ls /sys/class/backlight/*/brightness\n"); //fast, use LS
			else os.writeBytes("find /sys/devices -name 'brightness' \n"); //slow, use BUSYBOX	
			
		}
		
		if (mBlMethod) os.writeBytes("find /sys/devices -name 'bl_power' \n");
		os.writeBytes("echo end\n");
		os.flush();
		
		InputStreamReader isr = new InputStreamReader(is);
	    BufferedReader br = new BufferedReader(isr);
		String line = null;
		while ((line = br.readLine()) != null)
		{
			if (line.trim().length() == 0 || line.trim().equals("end")) break;
			if (line.toLowerCase(Locale.getDefault()).contains("backlight"))
			{
				if (line.contains("button")) if (!mEnableButtonBrightness) continue;
				value = value + line + "\n";
			}
		}
		if (mFastBrightnessmethod) prefs.edit().putString("brightnessdevices", value).commit();
		return value;
	}
	
	private void root_set_framebuffer_blank()
	{
		if (mFBMethod)
		{
			try {
				doCmds("echo 0 > " + fb0path);
				doCmds("echo 1 > " + fb0path);
			} catch (Exception ex) {
				Logger.Log(this, ex);
			}
		}
	}
	private void root_set_device_brightness()
	{
		if (VERBOSE) Logger.Log(StandbyService.this, "Started a cycle");
		
		try {
			if (mEnableLCDPower) {
				if (brightnessvalue == 0 && lastLCDPower == 1) {
					lastLCDPower = 0;
					doCmds("echo 0 > " + lcdpath);
				}
				else if (brightnessvalue != 0 && lastLCDPower == 0) {
					lastLCDPower = 1;
					doCmds("echo 1 > " + lcdpath);
				}
				return;
			}
			if (!mSGS3US)
				mutex.acquire();
			if (VERBOSE) Logger.Log(StandbyService.this, "Brightness value=" + brightnessvalue);
			if (mFBMethod && (!mSGS3US))
			{
				if (brightnessvalue == 0)
				{
					doCmds("echo 1 > " + fb0path);
				}
				else
				{
					doCmds("echo 0 > " + fb0path);
				}
				if (VERBOSE) Logger.Log(StandbyService.this, "Write fb0 image");
			}
			for(String s: devicePath.split("\\r?\\n"))
			{
				if (VERBOSE) Logger.Log(StandbyService.this, "Inject to " + s);
				if (s.trim().length() > 0)
					if (s.contains("bl_power"))
					{
						if (mBlMethod) 
						{
							if (brightnessvalue == 0) doCmds("echo 1 > " + s.trim());
							else doCmds("echo 0 > " + s.trim());
						}
					}
					else if (mBrightnessMethod)
					{
						doCmds("echo " + brightnessvalue + " > " + s.trim());
					}
			}
		} 
		catch (Exception e) {
			Logger.Log(this, e);
		}
		finally {
			if (!mSGS3US)
				mutex.release();
		}
		if (VERBOSE) Logger.Log(StandbyService.this, "Finished a cycle");
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
	
	// Receiver listening for program's ACTION_DISABLE
	private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	Logger.Log(context, intent);
        	StandbyService.this.brightnessvalue = mScreenOnBrightnessValue;
        	if (mNonRootMethod)
    		{
				handler.post(new Runnable() {
					@Override
					public void run() {
						StandbyService.this.nonRootDisable();
						setIdleViewFullscreen(brightnessvalue == 0);
					}});
    		}
        	StandbyService.this.root_set_device_brightness();
        	stopeverything();
        }
    };

    // Receiver listening for Screen_off and Screen_on (power off/on button pressed)
	private class ScreenReceiver extends BroadcastReceiver {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	    	Logger.Log(context, intent);
	    	
	    	if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF))
	    		if (StandbyService.this.mOneTwoDimmer)
	    			autodimmer.Stop();
	    	else
	    	{
		    	if (mSurviveLock)
		    	{
		    		StandbyService.this.brightnessvalue = mScreenOnBrightnessValue;
		    		if (mNonRootMethod)
		    		{
						handler.post(new Runnable() {
							@Override
							public void run() {
								StandbyService.this.nonRootDisable();
							}});
		    		}
		    		StandbyService.this.root_set_device_brightness();
		    		autodimmer.Start();
		    		autodimmer.Reset();
		    	}
		    	else
		    	{
			    	// STOP FROM SCREEN
			    	if (mSGS3US) {
			    		stopeverything(false);
			    		StandbyService.this.stopSelf();
			    		isRunning = false;
			    	}
			    	else
			    		stopeverything();
		    	}
	    	}

			handler.post(new Runnable() {
				@Override
				public void run() {
			    	setIdleViewFullscreen(false);
				}});
	    }
	}
	
	private SensorEventListener mProximitySensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
                float currentReading = event.values[0];
                switch(mProximityBehaviour){
                	case 0:
	                	if (currentReading <= 0.0000001f) {
		                	toggleBrightness();
		                }
	                	break;
                	case 1:
            			brightnessvalue = (currentReading <= 0.0000001f)?mScreenOnBrightnessValue:0;
            			changeBrightness();
                    	setIdleViewFullscreen(brightnessvalue == 0);
                    	break;
                	case 2:
                		brightnessvalue = (currentReading <= 0.0000001f)?0:mScreenOnBrightnessValue;
                		changeBrightness();
                		setIdleViewFullscreen(brightnessvalue == 0);
                		break;
                }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    private void processFaceUpDown(float z) {
    	if (mEnableFaceDown) {
    		if (faceup != ((-10.1 <= z) && (z <= mZSensitivity)))
    		{
    			long diff = Calendar.getInstance().getTime().getTime() - mlastFace.getTime();
    			if (diff >= 1000)
    			{
    				faceup = ((-10.1 <= z) && (z <= mZSensitivity));
    				brightnessvalue = faceup ? mScreenOnBrightnessValue : 0;
    				toggleBrightness();
    				mlastFace = Calendar.getInstance().getTime();
    			}
    		}
    		else
    			mlastFace = Calendar.getInstance().getTime();
    	}
    }

    private void processHoldUpDown(float y) {
    	if (mEnableUpsideDown) {
    		if (faceup != ((-10.1 <= y) && (y <= mYSensitivity)))
    		{
    			long diff = Calendar.getInstance().getTime().getTime() - mlastHold.getTime();
    			if (diff >= 1000)
    			{
    				faceup = ((-10.1 <= y) && (y <= mYSensitivity));
    				brightnessvalue = faceup ? mScreenOnBrightnessValue : 0;
    				toggleBrightness();
    				mlastHold = Calendar.getInstance().getTime();
    			}
    		}
    		else
    			mlastHold = Calendar.getInstance().getTime();
    	}
    }
    
    private void toggleBrightness()
    {
    	brightnessvalue = mScreenOnBrightnessValue - brightnessvalue;
    	changeBrightness();
    	setIdleViewFullscreen(brightnessvalue == 0);
    }
    
    private void changeBrightness()
    {
    	try
    	{
    		if (mNonRootMethod)
        	{
    			if (brightnessvalue == 0)
    				StandbyService.this.nonRootEnable();
    			else
    				StandbyService.this.nonRootDisable();
        	}
    		setIdleViewFullscreen(brightnessvalue == 0);
        	if (mFBMethod && mSGS3US)
			{
				if (brightnessvalue == 0)
					doCmds("echo 1 > " + fb0path);
				else
					doCmds("echo 0 > " + fb0path);
				if (VERBOSE) Logger.Log(StandbyService.this, "Shaking Write to FB0 Image");
			}
    	}
    	catch(Exception ex) {}
    	if (mOneTwoDimmer) {
	    	if (brightnessvalue == 0)
        		autodimmer.Stop();
        	else
        		autodimmer.Start();
    	}
  		root_set_device_brightness();
    }
    
    private void processShaking(float x, float y, float z) {
    	if (mEnableShake) {
	      mAccelLast = mAccelCurrent;
	      mAccelCurrent = android.util.FloatMath.sqrt((x*x + y*y + z*z));
	      float delta = mAccelCurrent - mAccelLast;
	      mAccel = mAccel * 0.9f + delta; // perform low-cut filter

	      long diff = Calendar.getInstance().getTime().getTime() - mlast.getTime();
	      
	      if (mAccel >= 2.5f * mShakeSensitivity)
	      {
	    	  mShakeCount ++;
	    	  if (mShakeCount == 3)
	    	  {
			    if (diff >= 1000)
			    {
			    	toggleBrightness();
			    	if (VERBOSE) Logger.Log(StandbyService.this, "Shaking!");
	    	  		mlast = Calendar.getInstance().getTime();
			    }
	    	  	mShakeCount = 0;
	    	  }
	      }
    	}
    }
	
    private final SensorEventListener mSensorListener = new SensorEventListener() {
	  
	    public void onSensorChanged(SensorEvent se) {
	      processShaking(se.values[0],se.values[1],se.values[2]);
	      processHoldUpDown(se.values[1]);
	      processFaceUpDown(se.values[2]);
	    }

	    public void onAccuracyChanged(Sensor sensor, int accuracy) {
	    }
	  };
	  
	//non-root screen filter
	private class FilterView extends ViewGroup {
		    public FilterView(Context context) {
		        super(context);
		    	myPaint.setColor(Color.argb(Integer.parseInt(prefs.getString("nonrootvalue","200")), 0, 0, 0));
		    	myPaint.setStyle(Style.FILL);
		    }

	    	Paint myPaint = new Paint();
	    	
		    @Override
		    protected void onDraw(Canvas canvas) {
		    	super.onDraw(canvas);
		    	canvas.drawRect(0, 0, paramsfilter.width, paramsfilter.height, myPaint);
		    }
		    
			@Override
			protected void onLayout(boolean changed, int l, int t, int r, int b) {
			}
		}
	
	//toggle filter for One Two Dimmer
	private void setIdleViewFullscreen(final Boolean fullscreen)
	{
		if (idleview != null) {
			handler.post(new Runnable() {

				@Override
				public void run() {
					paramsIdle.width = fullscreen?Math.max(MaxWidth, MaxHeight) + 20:0;
					paramsIdle.height = paramsIdle.width;
					winman.updateViewLayout(idleview, paramsIdle);
				}
			});
		}
	}
	
	class IdleView extends ViewGroup {

	    @Override
	    public boolean onTouchEvent(MotionEvent event) {
	    	if (autodimmer.IsRunning())
	    	{
	    		autodimmer.Reset();
	    	}
	    	else
	    	{
	    		brightnessvalue = mScreenOnBrightnessValue;
	    		if (mNonRootMethod && brightnessvalue > 0)
				{
					handler.post(new Runnable() {
						@Override
						public void run() {
							StandbyService.this.nonRootDisable();
						}});
				}
	    		root_set_device_brightness();
	    		autodimmer.Start();
	    		setIdleViewFullscreen(false);
	    	}
	    	return false;
	    }
	    public IdleView(Context context) {
	        super(context);
	    }
		@Override
		protected void onLayout(boolean changed, int l, int t, int r, int b) {
		}
	}
	class HUDView extends ViewGroup {
	    private Date mlastDown, mNow;
	    Boolean mOnMove = false;
	    int mOnSwipe = 0;
	    Boolean mOnClick = false;
	    WindowManager wm;
	    public void makeActive() {
	    	this.setVisibility(View.VISIBLE);
			wm.removeView(this);
			wm.addView(this,paramsHUD);
		}
		
		public void makeInactive() {
	    	this.setVisibility(View.GONE);
			wm.removeView(this);
			wm.addView(this,paramsHUD);
		}
		
		public int getDirection(int original)
		{
			return dpaddirection[mOrientation][original];
		}
		
	    @Override
	    public boolean onTouchEvent(MotionEvent event) {
	    	if (!mEnableTouchpad) return super.onTouchEvent(event);
			if (Math.abs(event.getSize()) > 0.005f)
			{
	    		if (event.getAction() == MotionEvent.ACTION_DOWN)
	    		{
	    			if ((event.getX() < touchpadwidth) && (event.getY() < touchpadheight))
	    			{
	    				mOnMove = true;
	    				oldXcur = mouseview.getXpos();
	    				oldYcur = mouseview.getYpos();
	    				mlastDown = Calendar.getInstance().getTime();
	    			}
	    			else
	    			{
	    				mOnSwipe = (event.getX() >= touchpadwidth)?1:2;
	    				mouseview.setBitmapToDraw((mOnSwipe == 1)?bmpScrollV:bmpScrollH);
	    			}
    				oldX = event.getX();
    				oldY = event.getY();
	    		}
	    		else if (event.getAction() == MotionEvent.ACTION_MOVE)
	    		{
	    			if (mOnMove)
	    			{
	    				mouseview.setPosition(oldXcur + (int)((event.getX() - oldX) * mTouchpadSpeed), oldYcur + (int)((event.getY() - oldY) * mTouchpadSpeed));
	    			}
	    			else if (mOnSwipe == 1)
	    			{
	    				if (Math.abs(oldY - event.getY()) >= swipesensitivity)
	    				{
	    					if (oldY < event.getY()) //movedown
	    						HUDNative.injectMouseEvent(3, getDirection(1), 0);
	    					else
	    						HUDNative.injectMouseEvent(3, getDirection(0), 0);
	    					oldY = event.getY();
	    				}
	    			}
	    			else if (mOnSwipe == 2)
	    			{
	    				if (Math.abs(oldX - event.getX()) >= swipesensitivity)
	    				{
	    					if (oldX < event.getX()) //movedown
	    						HUDNative.injectMouseEvent(3, getDirection(3), 0);
	    					else
	    						HUDNative.injectMouseEvent(3, getDirection(2), 0);
	    					oldX = event.getX();
	    				}
	    			}
	    		}
	    		else if (event.getAction() == MotionEvent.ACTION_UP)
	    		{
	    			mOnMove = false;
	    			if (mOnSwipe != 0)
	    			{
	    				mouseview.setBitmapToDraw(bmpCursor);
	    				mOnSwipe = 0;
	    			}
	    			mNow = Calendar.getInstance().getTime();
	    			if (mNow.getTime() - mlastDown.getTime() < 200)
	    			{
	    					this.makeInactive();
	    					new Thread(new Runnable(){

								@Override
								public void run() {
									try {
					    				Thread.sleep(50);
					    				mouseview.Click();
										Thread.sleep(50);
					    				mouseview.Mouseup();
					    				handler.post(new Runnable(){

											@Override
											public void run() {
												HUDView.this.makeActive();
											}});
									} catch (InterruptedException e) {
										// TODO Auto-generated catch block
										Logger.Log(StandbyService.this, e);
									}
								}}).start();
	    			}
	    		}
		    	return true;
			}
	    	return false;
	    }
	    
	    public HUDView(Context context) {
	        super(context);
	        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
	    	myPaint.setColor(Color.argb(1, 0, 0, 0));
	    	myPaint.setStyle(Style.FILL);
			mlastDown = Calendar.getInstance().getTime();
			this.setOnKeyListener(new View.OnKeyListener() {

				@Override
				public boolean onKey(View v, int keyCode, KeyEvent event) {
					Log.w("Standby", "Key received: " + keyCode);
					return false;
				}});
	    }

    	Paint myPaint = new Paint();
    	
	    @Override
	    protected void onDraw(Canvas canvas) {
	    	super.onDraw(canvas);
	    	canvas.drawRect(-10, -10, 2000, 2000, myPaint);
	    }

	    float oldX = -1;
	    float oldY = -1;
	    int oldXcur = -1;
	    int oldYcur = -1;
	    
		@Override
		protected void onLayout(boolean changed, int l, int t, int r, int b) {			
		}
	    
	}
	class MouseView extends ViewGroup {
        Bitmap bitmapToDraw;
        public void setBitmapToDraw(Bitmap b)
        {
        	bitmapToDraw = b;
        	this.postInvalidate();
        }
        
	    public MouseView(Context context) {
	        super(context);
	        Xpos = 10;
	        Ypos = 10;
	        bitmapToDraw = bmpCursor;
	    }
	    
	    boolean bDraw = true;
	    
	    @Override
	    protected void onDraw(Canvas canvas) {
	    	super.onDraw(canvas);
	    	if (bDraw)
	    	canvas.drawBitmap(bitmapToDraw, Xpos, Ypos, null);
	    }
	    
	    public void setDrawable(boolean drawable)
	    {
	    	bDraw = drawable;
    		this.postInvalidate();
	    }
	    
	    private int Xpos, Ypos;
	    
	    public int getXpos()
	    {
	    	return Xpos;
	    }
	    public int getYpos()
	    {
	    	return Ypos;
	    }
	    public void checkBound()
	    {
	    	if (Xpos < 0) Xpos = 0; if (Ypos < 0) Ypos = 0;
	    	if (Xpos > touchpadwidthbound) Xpos = touchpadwidthbound;
	    	if (Ypos > touchpadheightbound) Ypos = touchpadheightbound;
	    }
	    public void setPosition(int X, int Y)
	    {
	    	Xpos = X;
	    	Ypos = Y;
	    	checkBound();
	    	this.postInvalidate();
	    }
	    @Override
	    
	    protected void onLayout(boolean arg0, int arg1, int arg2, int arg3, int arg4) {
	    }
	    
	    boolean mouseDown = false;
	    public void Click()
	    {
	    	if (mouseDown != true && mEnableTouchpad)
	    	{
		    	// ROTATION PERFORM counter clockwise
		    	switch (mOrientation)
		    	{
		    		case 0: //PORTRAIT, NOTHING TO DO
				    	HUDNative.injectMouseEvent(1, Xpos, Ypos);
		    			break;
		    		case 1: //Landscape right side top
				    	HUDNative.injectMouseEvent(1, params.height - Ypos, Xpos);
		    			break;
		    		case 2: //PORTRAIT, Upside down
		    			HUDNative.injectMouseEvent(1, params.width - Xpos, params.height - Xpos);
		    			break;
		    		case 3: //Landscape left side top
		    			HUDNative.injectMouseEvent(1, Ypos, params.width - Xpos);
		    			break;
		    	}
	    	
		    	mouseDown =true;
	    	}
	    }
	    public void Mouseup()
	    {
	    	// ROTATION PERFORM counter clockwise
	    	if (mEnableTouchpad)
	    	switch (mOrientation)
	    	{
	    		case 0: //PORTRAIT, NOTHING TO DO
			    	HUDNative.injectMouseEvent(0, Xpos, Ypos);
	    			break;
	    		case 1: //Landscape right side top
			    	HUDNative.injectMouseEvent(0, params.height - Ypos, Xpos);
	    			break;
	    		case 2: //PORTRAIT, Upside down
	    			HUDNative.injectMouseEvent(0, params.width - Xpos, params.height - Xpos);
	    			break;
	    		case 3: //Landscape left side top
	    			HUDNative.injectMouseEvent(0, Ypos, params.width - Xpos);
	    			break;
	    	}
	    	mouseDown = false;
	    }
	}
	private void setTouchpadSize()
	{
		int w= paramsHUD.width;
		int h= paramsHUD.height;
		if (mEnableTouchpadSoftkey)
		{
			w=HUDNative.NormaliseValue(w) + 4;
			h=HUDNative.NormaliseValue(h) + 4;
		}
		if (mEnableTouchpadSwipe)
		{
			touchpadwidth = w *(1 - swipearea);
			touchpadheight = h *(1 - swipearea);
		}
		else
		{
			touchpadwidth = w;
			touchpadheight = h;
		}
		touchpadwidthbound = w;
		touchpadheightbound = h;
	}
    @Override 
    public void onConfigurationChanged(Configuration newConfig) 
    {
    	if (mEnableTouchpad) {
			if (display.getRotation() != mOrientation) {
	    		mOrientation = display.getRotation();	
	    		paramsHUD.width = display.getWidth();
	    		paramsHUD.height = display.getHeight();
	    		params.width = display.getWidth();
	    		params.height = display.getHeight();
	    		setTouchpadSize();
	    		winman.removeView(myview);
	    		winman.removeView(mouseview);
	    		winman.addView(mouseview,params);
	    		winman.addView(myview,paramsHUD);
	    		mouseview.checkBound();
	    		mouseview.postInvalidate();
	    	}
    	}
        super.onConfigurationChanged(newConfig);
    }
    private OneTwoDimmer.DimToggleEventHandler autodimmerhandler = new
    		OneTwoDimmer.DimToggleEventHandler() {
				@Override
				public void OnDimToggleEventHandler() {
					brightnessvalue = 0;
					if (mNonRootMethod)
					{
						handler.post(new Runnable() {
							@Override
							public void run() {
								StandbyService.this.nonRootEnable();
							}});
					}
					root_set_device_brightness();
					setIdleViewFullscreen(brightnessvalue == 0);
				}
    };
    
	private class PhoneReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
        	Logger.Log(context, intent);
            if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(
                    TelephonyManager.EXTRA_STATE_RINGING)) {
            	// TURN ON THE SCREEN
            	old_phonebrightness = brightnessvalue;
            	StandbyService.this.brightnessvalue = mScreenOnBrightnessValue;
            	changeBrightness();
            	setIdleViewFullscreen(false);
            	if (mOneTwoDimmer)
            		autodimmer.Stop();
        		if (mEnableShake || mEnableFaceDown || mEnableUpsideDown)
    			try
    			{
    				mSensorManager.unregisterListener(mSensorListener);
    			}
    			catch(Exception ex) {}
    			if (mEnableLightSensor)
				try
				{
					mSensorManager.unregisterListener(mProximitySensorListener);
				}
				catch(Exception ex) {}
            } else if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(
                    TelephonyManager.EXTRA_STATE_IDLE)
                    || intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(
                            TelephonyManager.EXTRA_STATE_OFFHOOK)) {
            	StandbyService.this.brightnessvalue = old_phonebrightness;
            	changeBrightness();
            	setIdleViewFullscreen(StandbyService.this.brightnessvalue == 0);
            	if (mOneTwoDimmer && (old_phonebrightness != 0))
            		autodimmer.Start();
            	if (mEnableShake || mEnableFaceDown || mEnableUpsideDown)
	    			try
	    			{
	    				mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
	    			}
	    			catch(Exception ex) {}
    			if (mEnableLightSensor)
					try
					{
						mSensorManager.registerListener(mProximitySensorListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
					}
					catch(Exception ex) {}
            }
        }
    };
    
    private VolumeKeyDetector.OnVolumeKeyDetectedHandler volKeyDetectorListener = 
    			new VolumeKeyDetector.OnVolumeKeyDetectedHandler()
    {
		@Override
		public void OnVolumeKeyDetected(final char key) {
			handler.post(new Runnable() {
				@Override
				public void run() {
					switch (mVolumeBehaviour)
					{
						case 0:
							toggleBrightness();
							break;
						case 1:
					    	brightnessvalue = (key == VolumeKeyDetector.VOLUME_UP)?mScreenOnBrightnessValue:0;
							changeBrightness();
					    	setIdleViewFullscreen(brightnessvalue == 0);
					    	break;
						case 2:
					    	brightnessvalue = (key == VolumeKeyDetector.VOLUME_DOWN)?mScreenOnBrightnessValue:0;
							changeBrightness();
					    	setIdleViewFullscreen(brightnessvalue == 0);
					    	break;
					}
				}});
		}
    };
    private class MediaControlReceiver implements
    		MediaButtonControlReceiver.MediaButtonControlReceiverHandler
    {
		@Override
		public void onMediaButtonReceived(int keycode) {
			if ((keycode == KeyEvent.KEYCODE_HEADSETHOOK && mHeadSetMediaButtonControl) ||
			    (keycode != KeyEvent.KEYCODE_HEADSETHOOK && mHardwareMediaButtonControl))
				StandbyService.this.toggleBrightness();
		}
    };
}

package com.nkahoang.kernelswitchobserver;

import android.os.Handler;
import android.os.Message;
import android.os.UEventObserver;
import android.util.Log;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class GenericHardwareObserver extends UEventObserver {
    private static final String TAG = GenericHardwareObserver.class.getSimpleName();
    private static final boolean LOG = true;
    private java.util.ArrayList<UEventStateChangeHandler> mChangeHandlers;
    protected String HARDWARE_UEVENT_MATCH = "DEVPATH=/devices/virtual/switch/h2w";
    protected String HARDWARE_STATE_PATH = "/sys/class/switch/h2w/state";
    protected String HARDWARE_NAME_PATH = "/sys/class/switch/h2w/name";

    private String HARDWARE_NAME;
    private String mHardwareSwitch;
    private String mHardwareState = "";
    private String mPreviousHardwareState = "";
    private Boolean mIsRunning = false;
    
    public Boolean getIsRunning()
    {
    	return mIsRunning;
    }
    
    public static List<String> getHardwareSwitches()
    {
    	List<String> result = new ArrayList<String>();
    	
    	File file = new File("/sys/class/switch");
    	for(File f : file.listFiles())
    	{
    		if (f.isDirectory())
    			result.add(f.getName());
    	}
    	return result;
    }
    
    public GenericHardwareObserver (String hardware_switch_name) throws HardwareNotFoundException {
    	HARDWARE_UEVENT_MATCH = "DEVPATH=/devices/virtual/switch/" + hardware_switch_name;
    	HARDWARE_STATE_PATH = "/sys/class/switch/" + hardware_switch_name + "/state";
    	HARDWARE_NAME_PATH = "/sys/class/switch/" + hardware_switch_name + "/name";
    	mHardwareSwitch = hardware_switch_name;
        mChangeHandlers = new java.util.ArrayList<UEventStateChangeHandler>();
        init();
    }

    public void start()
    {
        startObserving(HARDWARE_UEVENT_MATCH);
    	mIsRunning = true;
    }
    
    public void stop()
    {
    	stopObserving();
    	mIsRunning = false;
    }
    
    public String getHardwareName()
    {
    	return HARDWARE_NAME;
    }
    public void setOnUEventChangeHandler(UEventStateChangeHandler handler)
    {
    	if ((handler != null) && (!mChangeHandlers.contains(handler)))
    		mChangeHandlers.add(handler);
    }
    public void removeOnUEventChangeHandler(UEventStateChangeHandler handler)
    {
    	if ((handler != null) && (mChangeHandlers.contains(handler)))
    		mChangeHandlers.remove(handler);
    }
    
    public String getPreviousHardwareState()
    {
    	return mPreviousHardwareState;
    }
    
    public String getCurrentHardwareState()
    {
    	return mHardwareState;
    }
    @Override
    public void onUEvent(UEventObserver.UEvent event) {
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "Dock UEVENT: " + event.toString());
        }

        synchronized (this) {
            try {
                String newState = event.get("SWITCH_STATE").trim();
                if (!newState.equals(mHardwareState)) {
                	mPreviousHardwareState = mHardwareState;
                	mHardwareState = newState;
                	update();
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "Could not parse switch state from event " + event);
            }
        }
    }

    private final void init() throws HardwareNotFoundException {
        char[] buffer = new char[1024];

        try {
            FileReader file = new FileReader(HARDWARE_STATE_PATH);
            int len = file.read(buffer, 0, 1024);
            file.close();
            mPreviousHardwareState = mHardwareState = (new String(buffer, 0, len)).trim();
            file = new FileReader(HARDWARE_NAME_PATH);
            len = file.read(buffer, 0, 1024);
            HARDWARE_NAME = new String(buffer, 0, len).trim();
            update();
            file.close();
        } catch (FileNotFoundException e) {
        	if (LOG)
            Log.w(TAG, "This kernel does not have that hardware");
        	throw new HardwareNotFoundException (this.mHardwareSwitch);
        } catch (Exception e) {
        	if (LOG)
            Log.e(TAG, "" , e);
        }
    }

    private final void update() {
        mHandler.sendEmptyMessage(0);
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    synchronized (this) {
                    	if (LOG)
                    	{
                    		String message = "Hardware (" + HARDWARE_NAME + ") status: " + mHardwareState;
                    		Log.i(TAG, message);
                    	}
                    	for(UEventStateChangeHandler u : mChangeHandlers)
                    		if (u!=null) u.OnUEventStateChange(mHardwareState);
                    }
                    break;
            }
        }
    };
}
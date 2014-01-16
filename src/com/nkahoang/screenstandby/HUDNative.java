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

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.Build;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

public class HUDNative {

private final static String executable_file = "screenstandby";

public static void injectMouseEvent(int down, int x, int y)
{
	if (_bInitialized)
	try {
		os.writeBytes(x + " " + y + " " + down + "\n");
	    os.flush();
	} catch (IOException e) {
		Logger.Log(_context, e);
	}
}

public static void injectKeyEvent(int down, int key)
{
	if (_bInitialized)
	try {
		os.writeBytes(key + " " + key + " " + down + "\n");
	    os.flush();
	} catch (IOException e) {
		Logger.Log(_context, e);
	}
}

public static void destroy()
{
	if (_bInitialized) {
		try
		{
			injectMouseEvent(0,0,0);
			injectMouseEvent(5,0,0);
		}
		catch(Exception e)
		{
			Logger.Log(_context, e);
		}
		try
		{
			_bInitialized = false;
			dpProc.destroy();
			dpProc = null;
			Logger.Log(_context, "HUD driver destroyed");
		}
		catch(Exception e)
		{
			Logger.Log(_context, e);
		}
	}
}

static Process dpProc;
static Context _context;
static DataOutputStream os;
static boolean _bInitialized = false;

public static boolean IsInitialized()
{
	return _bInitialized;
}
private final static int[] screenRes = {480, 540, 720, 800, 960, 1080, 1280, 1920};
public static int NormaliseValue(int res)
{
	int value = res;
	for (int r: screenRes)
	{
		if ((r == res)) return r;
		else if ((res < r) && (Math.abs(res - r) <= 48)) value = r;
	}
	return value;
}
@TargetApi(Build.VERSION_CODES.FROYO)
public static void initialize(Context context)
{
	_context = context;
	copyFileOrDir(executable_file, context);
	Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
	Logger.Log(context, "HUD driver initialised: " + NormaliseValue((display.getRotation() == Surface.ROTATION_0)?display.getWidth():display.getHeight()) + " " + NormaliseValue((display.getRotation() == Surface.ROTATION_0)?display.getHeight():display.getWidth()));
	try {
		dpProc = Runtime.getRuntime().exec("su");
	    os = new DataOutputStream(dpProc.getOutputStream());
	    String nativePath = "/data/data/" + context.getPackageName() + "/" + executable_file;
	    os.writeBytes("chmod 755 " + nativePath + "\n");
	    os.writeBytes(nativePath + " " + NormaliseValue((display.getRotation() == Surface.ROTATION_0)?display.getWidth():display.getHeight()) + " " + NormaliseValue((display.getRotation() == Surface.ROTATION_0)?display.getHeight():display.getWidth()) + "\n");
	    os.flush();
	    _bInitialized = true;
	} catch (Exception e) {
		_bInitialized = false;
	}
}

private static void copyFileOrDir(String path, Context context) {
    try {
        AssetManager assetManager = context.getAssets();
        String assets[] = null;
        assets = assetManager.list(path);
        if (assets.length == 0) {
            copyFile(path,context);
        } else {
            String fullPath = "/data/data/" + context.getPackageName() + "/" + path;
            File dir = new File(fullPath);
            if (!dir.exists())
                dir.mkdir();
            for (int i = 0; i < assets.length; ++i) {
                copyFileOrDir(path + "/" + assets[i],context);
            }
        }
    } catch (IOException ex) {
        Log.e("tag", "I/O Exception", ex);
    }
}

private static void copyFile(String filename, Context context) {
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
	    } catch (Exception e) {
	        Log.e("tag", e.getMessage());
	    }
	}
}

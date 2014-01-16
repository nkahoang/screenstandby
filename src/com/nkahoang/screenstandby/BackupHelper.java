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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import java.util.zip.GZIPOutputStream;
import java.util.zip.GZIPInputStream;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Environment;
import android.preference.PreferenceManager;

class BackupHelper {
	static String GenerateFilename()
	{
		String logfile = Environment.getExternalStorageDirectory().getPath() + "/screenstandby"+getDateTime()+".backup-ss";
		return logfile;
	}
	
	final static String getDateTime()  
	{  
	    SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd-hhmmss");
	    return df.format(Calendar.getInstance().getTime());  
	}
	
	static String saveSharedPreferencesToFile(Context context)
	{
		try
		{
			File myFile = new File(GenerateFilename());
			myFile.createNewFile();
			return saveSharedPreferencesToFile(context,myFile);
		}
		catch(Exception e)
		{
			Logger.Log(context, e);
			return null;
		}
	}
	static String saveSharedPreferencesToFile(Context context, File dst) {
	    String res = null;
	    ObjectOutputStream output = null;
	    try {
	    	GZIPOutputStream outputGZIP = new GZIPOutputStream(new FileOutputStream(dst));
	        output = new ObjectOutputStream(outputGZIP);
	        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
	        Map<String, Object> shallowCopy = new HashMap<String, Object>(pref.getAll());
	        shallowCopy.remove("extendedlog");
	        output.writeObject(shallowCopy); //write everything but not the log
	        res = dst.getAbsolutePath();
	    } catch (FileNotFoundException e) {
	        e.printStackTrace();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }finally {
	        try {
	            if (output != null) {
	                output.flush();
	                output.close();
	            }
	        } catch (IOException ex) {
	            ex.printStackTrace();
	        }
	    }
	    return res;
	}

	@SuppressWarnings("unchecked")
	static boolean loadSharedPreferencesFromFile(Context context, File src) {
	    boolean res = false;
	    ObjectInputStream input = null;
	    try {
	    	GZIPInputStream inputGZIP = new GZIPInputStream(new FileInputStream(src));
	        input = new ObjectInputStream(inputGZIP);
	            Editor prefEdit = PreferenceManager.getDefaultSharedPreferences(context).edit();
	            prefEdit.clear();
	            Map<String, Object> entries = (Map<String, Object>) input.readObject();
	            for (Entry<String, ?> entry : entries.entrySet()) {
	                Object v = entry.getValue();
	                String key = entry.getKey();

	                if (v instanceof Boolean)
	                    prefEdit.putBoolean(key, ((Boolean) v).booleanValue());
	                else if (v instanceof Float)
	                    prefEdit.putFloat(key, ((Float) v).floatValue());
	                else if (v instanceof Integer)
	                    prefEdit.putInt(key, ((Integer) v).intValue());
	                else if (v instanceof Long)
	                    prefEdit.putLong(key, ((Long) v).longValue());
	                else if (v instanceof String)
	                    prefEdit.putString(key, ((String) v));
	            }
	            prefEdit.commit();
	        res = true;         
	    } catch (FileNotFoundException e) {
	        e.printStackTrace();
	    } catch (IOException e) {
	        e.printStackTrace();
	    } catch (ClassNotFoundException e) {
	        e.printStackTrace();
	    }finally {
	        try {
	            if (input != null) {
	                input.close();
	            }
	        } catch (IOException ex) {
	            ex.printStackTrace();
	        }
	    }
	    return res;
	}
}

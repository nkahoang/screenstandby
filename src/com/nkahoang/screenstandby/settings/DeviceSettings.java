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
package com.nkahoang.screenstandby.settings;
import javax.xml.parsers.*;
import android.os.Build;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.nkahoang.screenstandby.Logger;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DeviceSettings {
	private static AutoConfigurations configurationBase;
	private static DeviceConf bestMatch;
	public static void Initialize(Context context)
	{
		try
		{
			configurationBase = new AutoConfigurations();
			DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document document = docBuilder.parse(context.getAssets().open("autoconf.xml"));
			parseDocument(document); //parsing default autoconfig
			File rootDirectory = new File(Environment.getExternalStorageDirectory().getPath());
			String[] files = rootDirectory.list(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String filename) {
					String fLowerCase = filename.toLowerCase();
					return (fLowerCase.endsWith("ss-autoconf.xml"));
				}});
			
			for(String fname: files)
			{
				try
				{
					java.io.FileInputStream fis = new java.io.FileInputStream(fname);
					parseDocument(docBuilder.parse(fis));
					fis.close();
				}
				catch(Exception ex)
				{
				}
			}
			bestMatch = GetLastMatch();
			Sort(configurationBase.DeviceConfigurations);
		}
		catch(Exception ex)
		{
			Logger.Log(context, ex);
		}
	}
	
	private static void Sort(List<ConfEntry> list)
	{
		ConfEntry cur;
		for(int i = 0; i < (list.size()-1); i++)
		{
			int y = i + 1;
			cur = list.get(y);
			while ((y > 0) && String.CASE_INSENSITIVE_ORDER.compare(list.get(y-1).DisplayName, cur.DisplayName) >= 0) {
				list.set(y, list.get(y-1));
				y --;
			}
			list.set(y, cur);
		}
	}

	public static ConfEntry MergeEntries(ConfEntry[] entries)
	{
		ConfEntry mergedEntry = new ConfEntry();
		for(ConfEntry entry: entries)
		{
			if (entry.SetZeroBrightnessMethod != null) mergedEntry.SetZeroBrightnessMethod = entry.SetZeroBrightnessMethod;
			if (entry.SetFasterZeroBrightness != null) mergedEntry.SetFasterZeroBrightness = entry.SetFasterZeroBrightness;
			if (entry.SetPersistentBlankSwitch != null) mergedEntry.SetPersistentBlankSwitch = entry.SetPersistentBlankSwitch;
			if (entry.SetLinuxBlSwitch != null) mergedEntry.SetLinuxBlSwitch = entry.SetLinuxBlSwitch;
			if (entry.SetSGS3Compatibility != null) mergedEntry.SetSGS3Compatibility =  entry.SetSGS3Compatibility;
			if (entry.SetNonRoot != null) mergedEntry.SetNonRoot = entry.SetNonRoot;
			if (entry.SetBrightnessPath != null) mergedEntry.SetBrightnessPath = entry.SetBrightnessPath;
			if (entry.SetFb0Path != null) mergedEntry.SetFb0Path = entry.SetFb0Path;
			if (entry.SetJBDisplayHWOverlay != null) mergedEntry.SetJBDisplayHWOverlay = entry.SetJBDisplayHWOverlay;
			if (entry.SetRequireRoot != null) mergedEntry.SetRequireRoot = entry.SetRequireRoot;
			if (entry.SetAutoHDMI != null) mergedEntry.SetAutoHDMI = entry.SetAutoHDMI;
			if (entry.SetAcceleratorSensor != null) mergedEntry.SetAcceleratorSensor = entry.SetAcceleratorSensor;
			if (entry.SetProximitySensor != null) mergedEntry.SetProximitySensor = entry.SetProximitySensor;
			if (entry.SetProximityMode != null) mergedEntry.SetProximityMode = entry.SetProximityMode;
			if (entry.SetWakeLock != null) mergedEntry.SetWakeLock = entry.SetWakeLock;
			if (entry.SetVolumeButton != null) mergedEntry.SetVolumeButton = entry.SetVolumeButton;
		}
		return mergedEntry;
	}
	public static String GetPendingSettings(Context context, ConfEntry[] entries, boolean resetToDefault)
	{
		return GetPendingSettings(context, MergeEntries(entries),resetToDefault);
	}
	public static String GetPendingSettings(Context context, ConfEntry entry, boolean resetToDefault)
	{
		String result = "";
		if (resetToDefault)
			result += "- Reset all settings to default value\n";
		
		if (entry.SetZeroBrightnessMethod != null)
		{
			if (entry.SetZeroBrightnessMethod.equals("true")) result += "- Enable zero backlight brightness";
			else result += "- Disable zero backlight brightness";
			result += "\n";
		}
		if (entry.SetFasterZeroBrightness != null)
		{
			if (entry.SetFasterZeroBrightness.equals("true")) result += "- Enable fast zero brightness";
			else result += "- Disable fast zero brightness";
			result += "\n";
		}
		if (entry.SetPersistentBlankSwitch != null)
		{
			if (entry.SetPersistentBlankSwitch.equals("true")) result += "- Enable persistent blank frame";
			else result += "- Disable persistent blank frame";
			result += "\n";
		}
		if (entry.SetLinuxBlSwitch != null)
		{
			if (entry.SetLinuxBlSwitch.equals("true")) result += "- Enable linux native backlight switch";
			else result += "- Disable linux native backlight switch";
			result += "\n";
		}
		if (entry.SetSGS3Compatibility != null)
		{
			if (entry.SetSGS3Compatibility.equals("true")) result += "- Enable Samsung US variant compatibility mode";
			else result += "- Disable Samsung US variant compatibility mode";
			result += "\n";
		}
		if (entry.SetNonRoot != null)
		{
			if (entry.SetNonRoot.equals("true")) result += "- Enable screen filter method";
			else result += "- Disable screen filter method";
			result += "\n";
		}
		if (entry.SetBrightnessPath != null || entry.SetFb0Path != null)
		{
			result += "- Enable custom device path\n";
		}
		if (entry.SetAutoHDMI != null)
		{
			if (entry.SetAutoHDMI.equals("true")) result += "- Enable auto detect HDMI/MHL connection";
			else result += "- Disable auto detect HDMI/MHL connection";
			result += "\n";
		}
		if (entry.SetAcceleratorSensor != null)
		{
			if (entry.SetAcceleratorSensor.equals("true")) result += "- Enable accelerator sensor (shaking to toggle)";
			else result += "- Disable accelerator sensor (shaking to toggle)";
			result += "\n";
		}
		if (entry.SetProximitySensor != null)
		{
			if (entry.SetProximitySensor.equals("true")) result += "- Enable proximity sensor";
			else result += "- Disable proximity sensor";
			result += "\n";
		}
		if (entry.SetProximityMode != null)
		{
			if (entry.SetProximityMode.equals("hover-screenoff")) result += "- Set proximity mode: Screen on by default. Hover hand / Put phone to pocket to turn screen off";
			else if (entry.SetProximityMode.equals("hover-screenon")) result += "- Set proximity mode: Screen off by default. Hover hand / Put phone to pocket to turn screen on";
			else result += "- Set proximity mode: Hover hand to toggle between screen on and off";
			result += "\n";
		}
		if (entry.SetWakeLock != null)
		{
			if (entry.SetWakeLock.equals("true")) result += "- Prevent device from going to sleep";
			else result += "- Disable sleep prevention";
			result += "\n";
		}
		if (entry.SetVolumeButton != null)
		{
			if (entry.SetVolumeButton.equals("true")) result += "- Enable using volume key to turn on/off device's screen";
			else result += "- Disable the use of volume key toggle screen on/off";
			result += "\n";
		}
		if (entry.SetSurviveScreenLock != null)
		{
			if (entry.SetSurviveScreenLock.equals("true")) result += "- Enable screen off to survive true device off (true screen off)";
			else result += "- Set screen off to be disabled on screen off";
			result += "\n";
		}
		return result;
	}
	public static boolean ApplySettings(Context context, ConfEntry[] entries, boolean resetToDefault)
	{
		return ApplySettings(context, MergeEntries(entries), resetToDefault);
	}
	public static boolean ApplySettings(Context context, ConfEntry entry, boolean resetToDefault)
	{
		boolean OK = true;
		try
		{

			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			SharedPreferences.Editor prefsEditor = prefs.edit();
			if (resetToDefault)
				prefsEditor = prefsEditor.clear();
			
			if (entry.SetZeroBrightnessMethod != null)
				prefsEditor = prefsEditor.putBoolean("brightnessmethod", entry.SetZeroBrightnessMethod.equals("true"));
			
			if (entry.SetFasterZeroBrightness != null)
				prefsEditor = prefsEditor.putBoolean("brightnessmethod_fast", entry.SetZeroBrightnessMethod.equals("true"));
			
			if (entry.SetPersistentBlankSwitch != null)
				prefsEditor = prefsEditor.putBoolean("framebuffermethod", entry.SetPersistentBlankSwitch.equals("true"));
			
			if (entry.SetLinuxBlSwitch != null)
				prefsEditor = prefsEditor.putBoolean("blmethod", entry.SetLinuxBlSwitch.equals("true"));
			
			if (entry.SetSGS3Compatibility != null)
				prefsEditor = prefsEditor.putBoolean("voltagemethod", entry.SetSGS3Compatibility.equals("true"));
			
			
			if (entry.SetNonRoot != null)
			{
				prefsEditor = prefsEditor.putBoolean("nonrootmethod", entry.SetNonRoot.equals("true"));
				prefsEditor = prefsEditor.putString("nonrootvalue","255");
			}

			if (entry.SetBrightnessPath != null)
				prefsEditor = prefsEditor.putString("mCustomPath",entry.SetBrightnessPath);
			else
				prefsEditor = prefsEditor.putString("mCustomPath","");
			
			if (entry.SetRefreshRate != null)
				prefsEditor = prefsEditor.putInt("mCustomRefreshRate",Integer.parseInt(entry.SetRefreshRate));
			else
				prefsEditor = prefsEditor.remove("mCustomRefreshRate");
			
			if (entry.SetFb0Path != null)
				prefsEditor = prefsEditor.putString("mCustomFB0",entry.SetBrightnessPath);
			else
				prefsEditor = prefsEditor.putString("mCustomFB0","");
			
			if (entry.SetAutoHDMI != null)
				prefsEditor = prefsEditor.putBoolean("hdmidetection", entry.SetAutoHDMI.equals("true"));
			
			if (entry.SetAcceleratorSensor != null)
				prefsEditor = prefsEditor.putBoolean("shakeenabling", entry.SetAcceleratorSensor.equals("true"));
			
			if (entry.SetProximitySensor != null)
				prefsEditor = prefsEditor.putBoolean("proximityenabling", entry.SetProximitySensor.equals("true"));
			
			if (entry.SetProximityMode != null)
			{
				if (entry.SetProximityMode.equals("hover-screenoff")) prefsEditor = prefsEditor.putString("proximitybehaviour", "2");
				else if (entry.SetProximityMode.equals("hover-screenon")) prefsEditor = prefsEditor.putString("proximitybehaviour", "1");
				else prefsEditor = prefsEditor.putString("proximitybehaviour", "0");
			}
			
			if (entry.SetWakeLock != null)
				prefsEditor = prefsEditor.putBoolean("wakelockenable", entry.SetWakeLock.equals("true"));
			
			if (entry.SetVolumeButton != null)
				prefsEditor = prefsEditor.putBoolean("usevolumekey", entry.SetVolumeButton.equals("true"));
			
			if (entry.SetSurviveScreenLock != null)
				prefsEditor = prefsEditor.putBoolean("survivescreenlock", entry.SetSurviveScreenLock.equals("true"));
			
			OK = prefsEditor.commit();	
		}
		catch(Exception ex)
		{
			Logger.Log(context, ex);
		}
		return OK;
	}
	
	public static DeviceConf GetMatchDevice()
	{
		return bestMatch; 
	}
	
	private static void parseDocument(Document doc)
	{
		NodeList nl = doc.getElementsByTagName("device");
		Node node, attrib;
		NamedNodeMap attribs;
		String name, value;
		
		for(int i = 0; i < nl.getLength(); i++)
		{
			node = nl.item(i);
			attribs = node.getAttributes();
			DeviceConf dc = new DeviceConf();
			dc.id=configurationBase.DeviceConfigurations.size();
			for(int y = 0; y < attribs.getLength(); y++)
			{
				attrib = attribs.item(y);
				name = attrib.getNodeName().trim().toLowerCase(Locale.getDefault());
				value = attrib.getNodeValue().trim();
				if (name.equals("displayname")) dc.DisplayName = value;
				else if (name.equals("filtermodel")) dc.FilterModel = value;
				else if (name.equals("filterbrand")) dc.FilterBrand = value;
				else if (name.equals("filterhardware")) dc.FilterHardware = value;
				else if (name.equals("filtermanufacturer")) dc.FilterManufacturer = value;
				else if (name.equals("filterdisplay")) dc.FilterDisplay = value;
				else if (name.equals("filterproduct")) dc.FilterProduct = value;
				else if (name.equals("filterplatform")) dc.FilterPlatform = value;
				else if (name.equals("setrefreshrate")) dc.SetRefreshRate = value;
				else if (name.equals("setzerobrightnessmethod")) dc.SetZeroBrightnessMethod = value;
				else if (name.equals("setfasterzerobrightness")) dc.SetFasterZeroBrightness = value;
				else if (name.equals("setpersistentblankSwitch")) dc.SetPersistentBlankSwitch = value;
				else if (name.equals("setlinuxblswitch")) dc.SetLinuxBlSwitch = value;
				else if (name.equals("setsgs3compatibility")) dc.SetSGS3Compatibility = value;
				else if (name.equals("setnonroot")) dc.SetNonRoot = value;
				else if (name.equals("setbrightnesspath")) dc.SetBrightnessPath = value;
				else if (name.equals("setfb0path")) dc.SetFb0Path = value;
				else if (name.equals("setjbdisplayhwoverlay")) dc.SetJBDisplayHWOverlay = value;
				else if (name.equals("setrequireroot")) dc.SetRequireRoot = value;
				else if (name.equals("setautohdmi")) dc.SetAutoHDMI = value;
				else if (name.equals("setacceleratorsensor")) dc.SetAcceleratorSensor = value;
				else if (name.equals("setproximitysensor")) dc.SetProximitySensor = value;
				else if (name.equals("setproximitymode")) dc.SetProximityMode = value;
				else if (name.equals("setvolumebutton")) dc.SetVolumeButton = value;
				else if (name.equals("setwakelock")) dc.SetWakeLock = value;
				else if (name.equals("setSurviveScreenLock")) dc.SetSurviveScreenLock = value;
			}
			configurationBase.DeviceConfigurations.add(dc);
		}

		nl = doc.getElementsByTagName("usecase");
		for(int i = 0; i < nl.getLength(); i++)
		{
			node = nl.item(i);
			attribs = node.getAttributes();
			UseCaseConf dc = new UseCaseConf();
			dc.id=configurationBase.UsecaseConfigurations.size();
			for(int y = 0; y < attribs.getLength(); y++)
			{
				attrib = attribs.item(y);
				name = attrib.getNodeName().trim().toLowerCase(Locale.getDefault());
				value = attrib.getNodeValue().trim();
				if (name.equals("displayname")) dc.DisplayName = value;
				else if (name.equals("setzerobrightnessmethod")) dc.SetZeroBrightnessMethod = value;
				else if (name.equals("setfasterzerobrightness")) dc.SetFasterZeroBrightness = value;
				else if (name.equals("setpersistentblankSwitch")) dc.SetPersistentBlankSwitch = value;
				else if (name.equals("setlinuxblswitch")) dc.SetLinuxBlSwitch = value;
				else if (name.equals("setsgs3compatibility")) dc.SetSGS3Compatibility = value;
				else if (name.equals("setnonroot")) dc.SetNonRoot = value;
				else if (name.equals("setbrightnesspath")) dc.SetBrightnessPath = value;
				else if (name.equals("setfb0path")) dc.SetFb0Path = value;
				else if (name.equals("setrefreshrate")) dc.SetRefreshRate = value;
				else if (name.equals("setjbdisplayhwoverlay")) dc.SetJBDisplayHWOverlay = value;
				else if (name.equals("setrequireroot")) dc.SetRequireRoot = value;
				else if (name.equals("setautohdmi")) dc.SetAutoHDMI = value;
				else if (name.equals("setacceleratorsensor")) dc.SetAcceleratorSensor = value;
				else if (name.equals("setproximitysensor")) dc.SetProximitySensor = value;
				else if (name.equals("setproximitymode")) dc.SetProximityMode = value;
				else if (name.equals("setvolumebutton")) dc.SetVolumeButton = value;
				else if (name.equals("setwakelock")) dc.SetWakeLock = value;
				else if (name.equals("setSurviveScreenLock")) dc.SetSurviveScreenLock = value;
			}
			configurationBase.UsecaseConfigurations.add(dc);
		}
	}
	
	public static DeviceConf GetLastMatch()
	{
		DeviceConf match = null;
		for(ConfEntry conf : configurationBase.DeviceConfigurations)
			if (((DeviceConf)conf).MatchCurrentConfig()) match = (DeviceConf)conf;
		return match;
	}
	
	public static List<ConfEntry> GetDevicesList()
	{
		return configurationBase.DeviceConfigurations;
	}
	
	public static List<ConfEntry> GetUsecaseList()
	{
		return configurationBase.UsecaseConfigurations;
	}
	
	static class AutoConfigurations
	{
		List<ConfEntry> DeviceConfigurations = new ArrayList<ConfEntry>();
		List<ConfEntry> UsecaseConfigurations = new ArrayList<ConfEntry>();
	}
	public static class ConfEntry
	{
		public int id;
		public String DisplayName;
		String SetZeroBrightnessMethod; 
		String SetFasterZeroBrightness;
		String SetPersistentBlankSwitch; 
		String SetLinuxBlSwitch;
		String SetSGS3Compatibility; 
		String SetRefreshRate;
		String SetNonRoot;
		String SetBrightnessPath; 
		String SetFb0Path;
		public String SetJBDisplayHWOverlay;
		public String SetRequireRoot;
		String SetAutoHDMI;
		String SetSurviveScreenLock;
		String SetAcceleratorSensor;
		String SetProximitySensor;
		String SetProximityMode;
		String SetVolumeButton;	
		String SetWakeLock;
		
	}
	public static class DeviceConf extends ConfEntry
	{
		String FilterModel;
		String FilterManufacturer;
		String FilterBrand;
		String FilterHardware;
		String FilterDisplay;
		String FilterProduct;
		String FilterPlatform;
		
		private boolean Match(String confString, String value)
		{
			return confString.toLowerCase(Locale.getDefault()).contains(value.toLowerCase(Locale.getDefault()));
		}
		
		@SuppressLint("NewApi")
		public boolean MatchCurrentConfig()
		{
			return ((FilterModel == null) || (Match(Build.MODEL, FilterModel))) &&
				   ((FilterManufacturer == null) || (Match(Build.MANUFACTURER, FilterManufacturer))) &&
				   ((FilterBrand == null) || (Match(Build.BRAND, FilterBrand))) &&
				   ((Build.VERSION.SDK_INT <= 5) || (FilterHardware == null) || (Match(Build.HARDWARE, FilterHardware))) &&
				   ((FilterDisplay == null) || (Match(Build.DISPLAY, FilterDisplay))) &&
				   ((FilterProduct== null) || (Match(Build.PRODUCT, FilterProduct))) &&
				   ((FilterPlatform == null) || (Match(Build.BOARD, FilterPlatform))); 
		}
	}
	static class UseCaseConf extends ConfEntry
	{
	}
}

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
import java.io.FilenameFilter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import android.R.drawable;
import android.net.Uri;
import android.os.Environment;
import android.os.Parcel;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Color;

import com.nkahoang.screenstandby.R;

public class SettingActivity {
	Boolean run_dialog_inited = false;
	IconContextMenu iconContextMenu;
	Preference p, papp1, papp2, papp3, papp4, prunwizard, pappdetect;
	Preference pdock, pheadset, pheadsetcheckbox, pclearlog, pSendLog, ponetwotimer, pbackup, prestore, pbt, psearchplugin, pintentHDMIConnect, pintentHDMIDisconnect;
	Preference paddcallaction, pautodialerclearall;
	Preference puseautodialer;
	Preference pCutPower;
	PreferenceCategory screenAutoCaller,brightnessCateg,otherRootCateg,otherNonRootCateg;
	SharedPreferences settings;
	AlertDialog.Builder trackingdialog = null;
	PreferenceScreen prefScreen;
	int app_select_for = 1; //1 = HDMI; 2 = DOCK; 3 = HEADSET
	int count = 0;
	int selectedfile = 0;
	Context context;
	@SuppressLint("NewApi")
	public SettingActivity(final Context _c, final PreferenceScreen pref)
	{	
		context = _c;
		prefScreen = pref;
			settings = PreferenceManager.getDefaultSharedPreferences(context);
			prunwizard = pref.findPreference("runwizard");
			papp1 = pref.findPreference("selectedapp1");
			papp2 = pref.findPreference("selectedapp2");
			papp3 = pref.findPreference("selectedapp3");
			papp4 = pref.findPreference("selectedapp4");
			pappdetect = pref.findPreference("appdetection");
			psearchplugin = pref.findPreference("searchplugin");
			pintentHDMIConnect = pref.findPreference("hdmi_broadcast_connect_action");
			pintentHDMIDisconnect = pref.findPreference("hdmi_broadcast_disconnect_action");
			paddcallaction = pref.findPreference("autodialeraddaction");
			pautodialerclearall = pref.findPreference("autodialerclearall");
			screenAutoCaller = (PreferenceCategory) pref.findPreference("autodialerscreen");
			puseautodialer = pref.findPreference("useautodialer");
			pCutPower = pref.findPreference("cutLCDpowermethod");
			
			brightnessCateg = (PreferenceCategory) pref.findPreference("brightnessCateg");
			otherRootCateg = (PreferenceCategory) pref.findPreference("otherRootCateg");
			otherNonRootCateg = (PreferenceCategory) pref.findPreference("otherNonRootCateg");
			
			Spannable note = new SpannableString ( "Note" );
			note.setSpan( new ForegroundColorSpan( Color.RED ), 0, note.length(), 0 );
			pref.findPreference("sensornote").setTitle(note);
			pref.findPreference("appdetectionnote").setTitle(note);
			
			Spannable summaryHeadset = new SpannableString ( "Headset detection may be interfere with music player trying to take control of headset. If headset detection stops working, go back here and turn off then turn on the setting again" );
			summaryHeadset.setSpan( new ForegroundColorSpan( Color.RED ), 0, summaryHeadset.length(), 0 );
			pref.findPreference("headsetwarning").setSummary(summaryHeadset);
			
			Spannable summary = new SpannableString ( "Although it is possible to enable multiple sensor-related features, it is recommended that you enable only one of the following options to prevent unexpected behaviour" );
			summary.setSpan( new ForegroundColorSpan( Color.RED ), 0, summary.length(), 0 );
			pref.findPreference("sensornote").setSummary(summary);
			
			boolean mEnableLCDPower = settings.getBoolean("cutLCDpowermethod", false);

			brightnessCateg.setEnabled(!mEnableLCDPower);
			otherRootCateg.setEnabled(!mEnableLCDPower);
			otherNonRootCateg.setEnabled(!mEnableLCDPower);
			
			pCutPower.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference,
						Object newValue) {
					Boolean value = (Boolean)newValue;

					brightnessCateg.setEnabled(!value);
					otherRootCateg.setEnabled(!value);
					otherNonRootCateg.setEnabled(!value);
					
					return true;
				}
			});
			Spannable summaryAppDetector = new SpannableString ( "This feature is for ROOTED device only" );
			summaryAppDetector.setSpan( new ForegroundColorSpan( Color.RED ), 0, summaryAppDetector.length(), 0 );
			pref.findPreference("appdetectionnote").setSummary(summaryAppDetector);
			String HDMIConnectIntent = settings.getString("hdmi_broadcast_connect_action", "");
			String HDMIDisconnectIntent = settings.getString("hdmi_broadcast_connect_action", "");
			pintentHDMIConnect.setSummary(HDMIConnectIntent.length() > 0?HDMIConnectIntent:"(None)");
			pintentHDMIDisconnect.setSummary(HDMIDisconnectIntent.length() > 0?HDMIDisconnectIntent:"(None)");
			pintentHDMIConnect.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference,
						Object newValue) {
					String value = newValue.toString().trim();
					pintentHDMIConnect.setSummary(value.length() > 0?value:"(None)");
					return true;
				}
			});
			pintentHDMIDisconnect.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference,
						Object newValue) {
					String value = newValue.toString().trim();
					pintentHDMIDisconnect.setSummary(value.length() > 0?value:"(None)");
					return true;
				}
			});
			psearchplugin.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					if (appInstalledOrNot("com.nkahoang.plugins.sssearch"))
					{
						Intent LaunchIntent = new Intent("com.nkahoang.plugins.sssearch.action.LAUNCH");
				        context.sendBroadcast(LaunchIntent);
					}
					else
					{
						AlertDialog.Builder builder = new AlertDialog.Builder(context);
						String msg = "Please install <b>Screen Standby Search plugin</b> to enable long-press search key feature.<br/><br/>" + 
								     "The plugin will let you automate various Screen standby task such as launching the app, toggling screen on/off or launch remote controller."; 
						builder
						    .setTitle("Long-press search plugin")
						    .setMessage(android.text.Html.fromHtml(msg))
						    .setNegativeButton("Dismiss", dialogClickListener)
						    .setPositiveButton("Install plugin", new OnClickListener(){
								@Override
								public void onClick(DialogInterface dialog,
										int which) {	
									Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.nkahoang.plugins.sssearch"));
					            	marketIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
					            	context.startActivity(marketIntent);
								}
						    });
						builder.show();
					}
					return false;
				}
			});
			prunwizard.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
			        Intent i = new Intent(_c,com.nkahoang.screenstandby.AutoSettingWizard.class);
			        _c.startActivity(i);
			        ((android.app.Activity)_c).finish();
			        return false;
				} });
			//if (Integer.valueOf(android.os.Build.VERSION.SDK) > 11)
			//pref.findPreference("mousepointerscreen").setIcon(settings.getBoolean("easterpointer", false)?R.drawable.mousea:R.drawable.mouse);
			/*
			pref.findPreference("programversion").setOnPreferenceClickListener(new OnPreferenceClickListener() {
				@SuppressLint("NewApi")
				@Override
				public boolean onPreferenceClick(Preference preference) {
					count ++;
					if (count == 5)
					{
						Boolean enable = !settings.getBoolean("easterpointer", false);
						Toast.makeText(context, "Easter egg " + (enable?"activated":"deactivated"), Toast.LENGTH_SHORT).show();
						settings.edit().putBoolean("easterpointer", enable).commit();
						count = 0;
						if (Integer.valueOf(android.os.Build.VERSION.SDK) > 11)
						pref.findPreference("mousepointerscreen").setIcon(enable?R.drawable.mousea:R.drawable.mouse);
					}
					return false;
				}});
			*/
			pdock = pref.findPreference("dockrun");
			pbt = pref.findPreference("btrun");
			p = pref.findPreference("hdmirun");
			pheadset = pref.findPreference("headsetrun");
			pheadsetcheckbox = pref.findPreference("useheadset");
			ponetwotimer = pref.findPreference("onetwodimtimer");
			pbackup = pref.findPreference("backupsettings");
			prestore = pref.findPreference("restoresettings");
			
			pheadsetcheckbox.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

				@Override
				public boolean onPreferenceChange(Preference preference,
						Object newValue) {
					try
					{
						if ((Boolean)newValue)
						{
							if (BootTimeReceiver.headsetreceiver == null)
								BootTimeReceiver.headsetreceiver = new HeadsetReceiver();
							context.registerReceiver(BootTimeReceiver.headsetreceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
						}
						else
						{
							context.unregisterReceiver(BootTimeReceiver.headsetreceiver);
						}
					}
					catch(Exception ex)
					{
						return true;
					}
					return true;
				}});
			pbackup.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					
					AlertDialog.Builder alert = new AlertDialog.Builder(context);

					alert.setTitle("Backup name");
					alert.setMessage("Enter a name for your backup");

					// Set an EditText view to get user input 
					final EditText input = new EditText(context);
					input.setText("BACKUP-" + BackupHelper.getDateTime());
					alert.setView(input);

					alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
					    String value = input.getText() + ".backup-ss";
					    value = value.replaceAll("^[.\\\\/:*?\"<>|]?[\\\\/:*?\"<>|]*", "");;
						try
						{
							File myFile = new File(Environment.getExternalStorageDirectory().getPath() + "/" + value);
							if (myFile.exists())
								myFile.delete();
							myFile.createNewFile();
							String filename = BackupHelper.saveSharedPreferencesToFile(context, myFile);
							if (filename != null) {
								Toast.makeText(context, "Settings saved at: " + filename, Toast.LENGTH_SHORT).show();
							}
							else {
								Toast.makeText(context, "Error occured while saving setting", Toast.LENGTH_SHORT).show();
						    }
						}
						catch(Exception e)
						{
							Toast.makeText(context, "Error occured while creating backup with provided filename", Toast.LENGTH_SHORT).show();
							Logger.Log(context, e);
						}
					}});

					alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					  public void onClick(DialogInterface dialog, int whichButton) {
					    // Canceled.
					  }
					});
					alert.show();
					input.selectAll();
					return false;
				}});
				
			prestore.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					AlertDialog.Builder restoreDialog = new AlertDialog.Builder(context).setCancelable(true);
					restoreDialog.setTitle("Select saved settings file");
			    	restoreDialog.setIcon(android.R.drawable.ic_menu_revert);
					File dir = new File(Environment.getExternalStorageDirectory().getPath());

			    	final File[] f = dir.listFiles(new FilenameFilter() { 
			    	         public boolean accept(File dir, String filename)
			    	              { return filename.endsWith(".backup-ss"); 
			    	              }
			    	} );
			    	
			    	final CharSequence[] restoreFiles = new CharSequence[f.length];
			    	if (restoreFiles.length == 0)
			    		restoreDialog.setMessage("No saved backup file found.\nBackup files must be located on root of external storage");
			    	for (int i = 0; i < f.length; i++)
			    		restoreFiles[i] = f[i].getName().replaceAll("(?i).backup-ss", "");
			    	restoreDialog.setNegativeButton("Cancel", dialogClickListener);
			    	selectedfile = 0;
			    	restoreDialog.setSingleChoiceItems(restoreFiles, 0, new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							selectedfile = which;								
						}});
						
			    	if (restoreFiles.length > 0)
			    	restoreDialog.setPositiveButton("Restore", new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							try
							{
								if (selectedfile >= 0)
								{
									if (BackupHelper.loadSharedPreferencesFromFile(context, f[selectedfile]))
									{
										Toast.makeText(context, "Settings successfully restored from \"" +
														restoreFiles[selectedfile] + "\"", Toast.LENGTH_SHORT).show();
									}
									else
										Toast.makeText(context, "Error occured while reading from file", Toast.LENGTH_SHORT).show();
								}
							}
							catch(Exception e) {}
							dialog.dismiss();
						}
			    	});
			    	
			    	if (restoreFiles.length > 0)
			    	restoreDialog.setNeutralButton("Delete", new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (selectedfile >= 0) {
								f[selectedfile].delete();
								Toast.makeText(context, "\"" + restoreFiles[selectedfile] + "\" deleted", Toast.LENGTH_SHORT).show();
							}
							dialog.dismiss();
						}});
						
			    	restoreDialog.show();
					return false;
				}});
			
			ponetwotimer.setOnPreferenceClickListener(new OnPreferenceClickListener() {

				@Override
				public boolean onPreferenceClick(Preference preference) {
					TimePickerDialog tpd = new TimePickerDialog(context, new TimePickerDialog.OnTimeSetListener() {
						@Override
						public void onTimeSet(TimePicker view, int hour,
								int minute, int second, boolean cancel) {
							if (!cancel)
								if (hour*3600+minute*60+second >= 3)
								{
									settings.edit().putInt("onetwotimerh", hour).
													putInt("onetwotimerm", minute).
													putInt("onetwotimers", second).commit();
								}
								else
								{
									Toast.makeText(context, "Timer only accepts a minimum value of 3 seconds", Toast.LENGTH_SHORT).show();
									settings.edit().putInt("onetwotimerh", 0).
													putInt("onetwotimerm", 0).
													putInt("onetwotimers", 3).commit();
								}
						}}, 
						settings.getInt("onetwotimerh", 0),
						settings.getInt("onetwotimerm", 0),
						settings.getInt("onetwotimers", 15));
					tpd.show();
					return false;
				}});
			
	        pref.findPreference("touchpadscrolling").setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

				@Override
				public boolean onPreferenceChange(Preference preference,
						Object newValue) {
					if ((Boolean)newValue == true)
					{
						if (trackingdialog == null)
						{ trackingdialog 
					         = new AlertDialog.Builder(context).setCancelable(false);
						trackingdialog.setTitle(R.string.pref_touchpad_scroll_notice);
				        LayoutInflater inflater = (LayoutInflater) context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );

				        LinearLayout layout = new LinearLayout(context);
				        layout.setOrientation(LinearLayout.VERTICAL);
				        inflater.inflate(R.layout.touchpad_preference_layout, layout);
				        trackingdialog.setView(layout);
				            
				        trackingdialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				            // do something when the button is clicked
				            public void onClick(DialogInterface arg0, int arg1) {
				            	arg0.dismiss();
				            	trackingdialog = null;
				             }
				            });				
						}
						trackingdialog.show();
					}
					return true;
				}});
			pclearlog = pref.findPreference("clearlog");
			changeLogSize();
			pclearlog.setOnPreferenceClickListener(new OnPreferenceClickListener() {

				@Override
				public boolean onPreferenceClick(Preference preference) {
					settings.edit().putString("extendedlog", "").commit();
					changeLogSize();
					return false;
				}});
			pSendLog = pref.findPreference("savelog");
			pSendLog.setOnPreferenceClickListener(new OnPreferenceClickListener() {

				@Override
				public boolean onPreferenceClick(Preference preference) {
					Logger.ShowLog(context);
					return false;
				}});
			pref.findPreference("resetdefault").setOnPreferenceClickListener(new OnPreferenceClickListener() {

				@Override
				public boolean onPreferenceClick(Preference preference) {
					AlertDialog.Builder builder = new AlertDialog.Builder(context);
					builder.setTitle("Reset to default");
					builder.setIcon(android.R.drawable.ic_menu_set_as);
					builder.setMessage("Are you sure to reset all settings to default?").setPositiveButton("Yes", dialogClickListener)
    				.setNegativeButton("No", dialogClickListener).show();
					return false;
				}
			});
			
			if (settings.getString("appdetect1package", "").length() > 0)
				papp1.setSummary(settings.getString("appdetect1name", settings.getString("appdetect1package", "")));
			else
				papp1.setSummary(R.string.pref_appdetection_noapp);
			
			if (settings.getString("appdetect2package", "").length() > 0)
				papp2.setSummary(settings.getString("appdetect2name", settings.getString("appdetect2package", "")));
			else
				papp2.setSummary(R.string.pref_appdetection_noapp);
			
			if (settings.getString("appdetect3package", "").length() > 0)
				papp3.setSummary(settings.getString("appdetect3name", settings.getString("appdetect3package", "")));
			else
				papp3.setSummary(R.string.pref_appdetection_noapp);
			
			if (settings.getString("appdetect4package", "").length() > 0)
				papp4.setSummary(settings.getString("appdetect4name", settings.getString("appdetect4package", "")));
			else
				papp4.setSummary(R.string.pref_appdetection_noapp);
			pappdetect.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference arg0, Object arg1) {
					Boolean enabled = (Boolean)arg1;
					if (enabled) {
						if (AppDetector.isRunning) return true;
						else context.startService(new Intent(context, AppDetector.class));
						Toast.makeText(context, "App detector started", Toast.LENGTH_SHORT).show();
					}
					else
					{
						Intent serviceIntent = new Intent();
			            serviceIntent.setAction(AppDetector.DETECT_SERVICE_CHANGE);
			            serviceIntent.putExtra("enabled", false);
			            context.sendBroadcast(serviceIntent);
			            Toast.makeText(context, "App detector stopped", Toast.LENGTH_SHORT).show();
					}
					return true;
				}});
			
			if (settings.getString("hdmirunpackage", "").length() > 0)
				p.setSummary("Launch " + settings.getString("hdmirunpackagename", settings.getString("hdmirunpackage", "")));
			if (settings.getString("dockrunpackage", "").length() > 0)
				pdock.setSummary("Launch " + settings.getString("dockrunpackagename", settings.getString("dockrunpackage", "")));
			if (settings.getString("headsetrunpackage", "").length() > 0)
				pheadset.setSummary("Launch " + settings.getString("headsetrunpackagename", settings.getString("headsetrunpackage", "")));
			if (settings.getString("btrunpackage", "").length() > 0)
				pbt.setSummary("Launch " + settings.getString("pbrunpackagename", settings.getString("pbrunpackage", "")));
			
			papp1.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					app_select_for = -1;
					showAppDialog();
					return false;
				}});
			papp2.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					app_select_for = -2;
					showAppDialog();
					return false;
				}});
			papp3.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					app_select_for = -3;
					showAppDialog();
					return false;
				}});
			papp4.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					app_select_for = -4;
					showAppDialog();
					return false;
				}});
			
			p.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					app_select_for = 1;
					showAppDialog();
					return false;
				}});
			
			pdock.setOnPreferenceClickListener(new OnPreferenceClickListener() {

				@Override
				public boolean onPreferenceClick(Preference preference) {
					app_select_for = 2;
					showAppDialog();
					return false;
				}});
			
			pheadset.setOnPreferenceClickListener(new OnPreferenceClickListener() {

				@Override
				public boolean onPreferenceClick(Preference preference) {
					app_select_for = 3;
					showAppDialog();
					return false;
				}});
			
			pbt.setOnPreferenceClickListener(new OnPreferenceClickListener() {

				@Override
				public boolean onPreferenceClick(Preference preference) {
					app_select_for = 4;
					showAppDialog();
					return false;
				}});
			
			paddcallaction.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					AlertDialog.Builder builder = new AlertDialog.Builder(context);
					LinearLayout actionView = getCallerActionView();
					isAddNewAction = true;
					builder.setView(actionView); 
					builder.setIcon(android.R.drawable.ic_menu_add);
					builder.setTitle("Add number to action")
						   .setPositiveButton("Add action", addCallActionClickListener)
					       .setNegativeButton("Cancel", addCallActionClickListener);
					builder.show();
					return false;
				}});

			pautodialerclearall.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					AlertDialog.Builder builder = new AlertDialog.Builder(context);
						builder.setTitle("Clear all dialer actions");
						builder.setIcon(android.R.drawable.ic_menu_delete);
						builder.setMessage("Are you sure to clear all actions?").setPositiveButton("Yes", new DialogInterface.OnClickListener(){
							@Override
							public void onClick(DialogInterface dialog,
									int which) {	
								CallAction.ClearAll(settings);
								displayAutoCallerAction();
							}
						})
    					.setNegativeButton("No", dialogClickListener).show();
					return false;
				}
			});
			displayAutoCallerAction();
			
	}
	
	private void displayAutoCallerAction() {
		screenAutoCaller.removeAll();
		phoneCallActions = CallAction.GetActions(settings);
		for(String phoneNo: phoneCallActions.keySet())
		{
			CallAction ca = phoneCallActions.get(phoneNo);
			Preference p = new Preference(context);
			if (ca.assignedAction < 3)
				p.setTitle(autoCallerActions[ca.assignedAction]);
			else
				p.setTitle("Launch " + ca.assignedAppLabel);
			p.setSummary("Dial number: " + ca.assignedPhoneNo);
			p.setKey(ca.assignedPhoneNo);
			p.setOnPreferenceClickListener(autoCallerModifyClick);
			screenAutoCaller.addPreference(p);
			p.setDependency(puseautodialer.getKey());
		}
	}
	HashMap<String,CallAction> phoneCallActions;
	private OnPreferenceClickListener autoCallerModifyClick = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			LinearLayout actionView = getCallerActionView();
			isAddNewAction = false;
			oldPhoneNo = preference.getKey();
			CallAction act = phoneCallActions.get(oldPhoneNo);
			if (act.assignedAction == 4)
			{
				spAction.setTag(null);
				aa.add("Launch " + act.assignedAppLabel);
			}
			spAction.setSelection(act.assignedAction);
			txtMappedNumber.setText(act.assignedPhoneNo);
			builder.setView(actionView); 
			builder.setIcon(android.R.drawable.ic_menu_edit);
			builder.setTitle("Edit action")
				   .setPositiveButton("Edit action", addCallActionClickListener)
				   .setNeutralButton("Remove action", addCallActionClickListener)
			       .setNegativeButton("Cancel", addCallActionClickListener);
			builder.show();
			return false;
		}};
		
	Spinner spAction = null;
	EditText txtMappedNumber = null;
	ArrayAdapter<String> aa ;
	String[] autoCallerActions = new String[]
			{
				"Launch SS main interface",
				"Toggle screen on and off",
				"Open remote control",
				"Launch an app",
			};
	private LinearLayout getCallerActionView()
	{
		LayoutInflater inflater = (LayoutInflater) context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        inflater.inflate(R.layout.auto_caller_add_action, layout);

		spAction = (Spinner)layout.findViewById(R.id.spCallerAction);
		txtMappedNumber = (EditText) layout.findViewById(R.id.txtMappedNumber);
		ArrayList<String> entries = new ArrayList<String>(Arrays.asList(autoCallerActions));
		aa = new ArrayAdapter<String>(context, R.layout.spinner_list_item, entries);
		spAction.setAdapter(aa);
		spAction.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				if (arg2 != 4 && aa.getCount() > 4)
					aa.remove(aa.getItem(4));
				
				if (arg2 == 3) {
					app_select_for = 5;
					showAppDialog();
				}
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {}
		});
        return layout;
	}
	private List<ResolveInfo> pkgAppsList;
	private PackageManager pm;
	private void showAppDialog()
	{
		if (!run_dialog_inited){
			final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
			mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
			pm = context.getPackageManager();
			pkgAppsList = pm.queryIntentActivities( mainIntent, 0);

        	ResolveInfo r;
        	String label;
        	int i;
			for (int j = 1; j < pkgAppsList.size() - 1; j++)
			{
				r= pkgAppsList.get(j);
				label = r.loadLabel(pm).toString().toLowerCase().trim();
				for(i = j - 1; (i >= 0) && (pkgAppsList.get(i).loadLabel(pm).toString().toLowerCase().trim().compareTo(label) > 0); i--) {
					pkgAppsList.set(i+1, pkgAppsList.get(i));
		        }
				pkgAppsList.set(i+1, r);
			}
			iconContextMenu = new IconContextMenu(context, 1);

        	iconContextMenu.addItem(Resources.getSystem(), "No app", 
		        	drawable.ic_menu_delete, -1);
	        for(i = 0; i < pkgAppsList.size(); i++)
	        {
	        	r = pkgAppsList.get(i);
	        	iconContextMenu.addItem(r.loadIcon(pm),r.loadLabel(pm),i);
	        }
	        iconContextMenu.setOnClickListener(new IconContextMenu.IconContextMenuOnClickListener() {
				@Override
				public void onClick(int menuId) {
					switch (app_select_for)
					{
						case -1:
							if (menuId == -1) {
								papp1.setSummary(R.string.pref_appdetection_noapp);
								settings.edit().putString("appdetect1package", "").commit();
							}
							else
							{
								papp1.setSummary(pkgAppsList.get(menuId).loadLabel(pm));
								settings.edit().putString("appdetect1package", pkgAppsList.get(menuId).activityInfo.packageName).commit();
								settings.edit().putString("appdetect1name", pkgAppsList.get(menuId).loadLabel(pm).toString()).commit();
							}

								Intent intent1 = new Intent();
								intent1.setAction(AppDetector.DETECT_SERVICE_CHANGE);
				            	context.sendBroadcast(intent1);
							break;
						case -2:
							if (menuId == -1) {
								papp2.setSummary(R.string.pref_appdetection_noapp);
								settings.edit().putString("appdetect2package", "").commit();
							}
							else
							{
								papp2.setSummary(pkgAppsList.get(menuId).loadLabel(pm));
								settings.edit().putString("appdetect2package", pkgAppsList.get(menuId).activityInfo.packageName).commit();
								settings.edit().putString("appdetect2name", pkgAppsList.get(menuId).loadLabel(pm).toString()).commit();
							}
								Intent intent2 = new Intent();
								intent2.setAction(AppDetector.DETECT_SERVICE_CHANGE);
								context.sendBroadcast(intent2);
							break;
						case -3:
							if (menuId == -1) {
								papp3.setSummary(R.string.pref_appdetection_noapp);
								settings.edit().putString("appdetect3package", "").commit();
							}
							else
							{
								papp3.setSummary(pkgAppsList.get(menuId).loadLabel(pm));
								settings.edit().putString("appdetect3package", pkgAppsList.get(menuId).activityInfo.packageName).commit();
								settings.edit().putString("appdetect3name", pkgAppsList.get(menuId).loadLabel(pm).toString()).commit();
							}
								Intent intent3 = new Intent();
								intent3.setAction(AppDetector.DETECT_SERVICE_CHANGE);
								context.sendBroadcast(intent3);
							break;
						case -4:
							if (menuId == -1) {
								papp4.setSummary(R.string.pref_appdetection_noapp);
								settings.edit().putString("appdetect4package", "").commit();
							}
							else
							{
								papp4.setSummary(pkgAppsList.get(menuId).loadLabel(pm));
								settings.edit().putString("appdetect4package", pkgAppsList.get(menuId).activityInfo.packageName).commit();
								settings.edit().putString("appdetect4name", pkgAppsList.get(menuId).loadLabel(pm).toString()).commit();
							}
								Intent intent4 = new Intent();
								intent4.setAction(AppDetector.DETECT_SERVICE_CHANGE);
								context.sendBroadcast(intent4);
							break;
						case 1:
							if (menuId == -1) {
								p.setSummary(R.string.pref_hdmi_run_sum);
								settings.edit().putString("hdmirunpackage", "").commit();
							}
							else
							{
								p.setSummary("Launch " + pkgAppsList.get(menuId).loadLabel(pm));
								settings.edit().putString("hdmirunpackage", pkgAppsList.get(menuId).activityInfo.packageName).commit();
								settings.edit().putString("hdmirunpackagename", pkgAppsList.get(menuId).loadLabel(pm).toString()).commit();
							}
							break;
						case 2:
							if (menuId == -1) {
								pdock.setSummary(R.string.pref_dock_run_sum);
								settings.edit().putString("dockrunpackage", "").commit();
							}
							else
							{
								pdock.setSummary("Launch " + pkgAppsList.get(menuId).loadLabel(pm));
								settings.edit().putString("dockrunpackage", pkgAppsList.get(menuId).activityInfo.packageName).commit();
								settings.edit().putString("dockrunpackagename", pkgAppsList.get(menuId).loadLabel(pm).toString()).commit();
							}
							break;
						case 3:
							if (menuId == -1) {
								pheadset.setSummary(R.string.pref_headset_run_sum);
								settings.edit().putString("headsetrunpackage", "").commit();
							}
							else
							{
								pheadset.setSummary("Launch " + pkgAppsList.get(menuId).loadLabel(pm));
								settings.edit().putString("headsetrunpackage", pkgAppsList.get(menuId).activityInfo.packageName).commit();
								settings.edit().putString("headsetrunpackagename", pkgAppsList.get(menuId).loadLabel(pm).toString()).commit();
							}
							break;
						case 4:
							if (menuId == -1) {
								pbt.setSummary(R.string.pref_bt_run_sum);
								settings.edit().putString("btrunpackage", "").commit();
							}
							else
							{
								pbt.setSummary("Launch " + pkgAppsList.get(menuId).loadLabel(pm));
								settings.edit().putString("btrunpackage", pkgAppsList.get(menuId).activityInfo.packageName).commit();
								settings.edit().putString("btrunpackagename", pkgAppsList.get(menuId).loadLabel(pm).toString()).commit();
							}
							break;
						case 5:{
							if (spAction != null)
							{
								if (menuId == -1) {
									spAction.setSelection(0);
								}
								else
								{
									String label = pkgAppsList.get(menuId).loadLabel(pm).toString();
									spAction.setTag(menuId);
									aa.add("Launch " + label);
									spAction.setSelection(4);
								}
							}
						}
					}
				}
			});
	        run_dialog_inited = true;
		}
        iconContextMenu.createMenu("Select application").show();
	}
	
	boolean isAddNewAction = true;
	String oldPhoneNo = null;
	final String[] restrictedNumberList = new String[] {"000","999","911","*#06*","#06*","*#4636#*"};
	private boolean isEmergencyNumber(String phoneNo)
	{
		for(String s : restrictedNumberList)
			if (phoneNo.equals(s)) return true;
		return false;
	}
	DialogInterface.OnClickListener addCallActionClickListener = new DialogInterface.OnClickListener() {
	    @Override
	    public void onClick(DialogInterface dialog, int which) {
	    	switch (which){
	        case DialogInterface.BUTTON_POSITIVE: //OK or MODIFY button
	        	if (spAction != null && txtMappedNumber != null)
	        	{
	        		String label = "";
	        		String packageName = "";
	        		int id = -1;
	        		if (spAction.getSelectedItemPosition() == 4)
	        		{
	        			if ( spAction.getTag() != null)
	        			{
	        				id = (Integer)spAction.getTag();
	        				label = pkgAppsList.get(id).loadLabel(pm).toString();
	        				packageName = pkgAppsList.get(id).activityInfo.packageName;
	        			}
	        		}
	        		
	        		String key = txtMappedNumber.getText().toString().trim();
	        		if (isEmergencyNumber(key))
	        		{
	        			Toast.makeText(context, "Cannot assign action to emergency or reserved number. Select another number", Toast.LENGTH_LONG).show();
	        			return;
	        		}
	        		if (phoneCallActions.containsKey(key))
	        		{
	        			if (isAddNewAction)
	        				Toast.makeText(context, "This phone number is assigned to another action. Remove or change the phone number of that action first", Toast.LENGTH_SHORT).show();
	        			else
	        			{
	        				if (key.equals(oldPhoneNo))
	        				{
		        				//number existed, but modified from the old phone no
		        				CallAction act = phoneCallActions.get(key);
		        				act.assignedAction = spAction.getSelectedItemPosition();
		        				act.assignedPhoneNo = key;
		        				if (id > -1)
		        				{
		        					act.assignedAppLabel = label;
		        					act.assignedAppPkg = packageName;
		        				}
		        				phoneCallActions.remove(key);
		        				phoneCallActions.put(key, act);
		        				CallAction.SaveToPreference(phoneCallActions, settings);
		        				displayAutoCallerAction();
		        				dialog.dismiss();
	        				}
	        				else
	        					Toast.makeText(context, "This phone number is assigned to another action. Remove or change the phone number of that action first", Toast.LENGTH_SHORT).show();
	        			}
	        		}
	        		else
	        		{
	        			if (isAddNewAction)
	        			{
	        				CallAction act = new CallAction(key, spAction.getSelectedItemPosition(), label, packageName);
	        				phoneCallActions.put(key, act);
	        				CallAction.SaveToPreference(phoneCallActions, settings);
	        				displayAutoCallerAction();
        					dialog.dismiss();
	        			}
	        			else
	        			{
	        				CallAction act = phoneCallActions.get(oldPhoneNo);
	        				act.assignedAction = spAction.getSelectedItemPosition();
	        				act.assignedPhoneNo = key;
	        				if (id > -1)
	        				{
	        					act.assignedAppLabel = label;
	        					act.assignedAppPkg = packageName;
	        				}
	        				phoneCallActions.remove(oldPhoneNo);
	        				phoneCallActions.put(key, act);
	        				CallAction.SaveToPreference(phoneCallActions, settings);
	        				displayAutoCallerAction();
	        				dialog.dismiss();
	        			}
	        		}
	        	}
	            break;
	        case DialogInterface.BUTTON_NEUTRAL:
	        	if (phoneCallActions.containsKey(oldPhoneNo))
	        	{
	        		phoneCallActions.remove(oldPhoneNo);
    				CallAction.SaveToPreference(phoneCallActions, settings);
    				displayAutoCallerAction();
	        	}
	        	dialog.dismiss();
	        	break;
	        case DialogInterface.BUTTON_NEGATIVE: //CANCEL
	            dialog.dismiss();
	            break;
	        }
	    }
	};
	public static class CallAction{
		private String assignedPhoneNo;
		private int assignedAction;
		private String assignedAppLabel;
		private String assignedAppPkg;
		public int GetAction()
		{
			return assignedAction;
		}
		public String GetAppLabel()
		{
			return assignedAppLabel;
		}
		public String GetAppPackage()
		{
			return assignedAppPkg;
		}
		public static HashMap<String,CallAction> GetActions(String[] prefActionArrays)
		{
			HashMap<String,CallAction> actions = new HashMap<String,CallAction>();
			for(String s : prefActionArrays)
			{
				CallAction c = new CallAction(s);
				actions.put(c.assignedPhoneNo, c);
			}
			return actions;
		}
		public static void ClearAll(SharedPreferences prefs)
		{
			saveArray(new String[]{},"callautomatorlist",prefs);
		}
		public static HashMap<String,CallAction> GetActions(SharedPreferences prefs)
		{
			return GetActions(loadArray("callautomatorlist",prefs));
		}
		
		public static void SaveToPreference(HashMap<String,CallAction> actions, SharedPreferences prefs)
		{
			String[] actionPrefs= new String[actions.size()];
			int i = 0;
			for(Entry<String, CallAction> act : actions.entrySet())
			{
				actionPrefs[i++] = act.getValue().ToPreferenceString();
			}
			saveArray(actionPrefs,"callautomatorlist",prefs);
		}
		
		public CallAction(String phoneNo, int action, String appLabel, String appPkg)
		{
			assignedPhoneNo = phoneNo;
			assignedAction = action;
			assignedAppLabel = appLabel;
			assignedAppPkg = appPkg;
		}
		
		public CallAction(String prefString)
		{
			String[] s = prefString.split("\\|");
			assignedPhoneNo = s[0];
			assignedAction =Integer.parseInt(s[1]);
			assignedAppLabel = (s.length > 2)?s[2]:"";
			assignedAppPkg = (s.length > 2)?s[3]:"";
		}
		
		public String ToPreferenceString()
		{
			return assignedPhoneNo + "|" + assignedAction + "|" + assignedAppLabel + "|" + assignedAppPkg; 
		}
	}
	
	public static boolean saveArray(String[] array, String arrayName, SharedPreferences prefs) {
	    SharedPreferences.Editor editor = prefs.edit();  
	    editor.putInt(arrayName +"_size", array.length);  
	    for(int i=0;i<array.length;i++) editor.putString(arrayName + "_" + i, array[i]);  
	    return editor.commit();  
	}
	public static String[] loadArray(String arrayName, SharedPreferences prefs) {    
	    int size = prefs.getInt(arrayName + "_size", 0);  
	    String array[] = new String[size];  
	    for(int i=0;i<size;i++) array[i] = prefs.getString(arrayName + "_" + i, null);  
	    return array;  
	}  
	
	DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
	    @Override
	    public void onClick(DialogInterface dialog, int which) {
	        switch (which){
	        case DialogInterface.BUTTON_POSITIVE:
	        	PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit();
	        	Intent intent = ((BaseActivity)context).getIntent();
	        	((BaseActivity)context).finish();
	        	context.startActivity(intent);
	            break;

	        case DialogInterface.BUTTON_NEGATIVE:
	            dialog.dismiss();
	            break;
	        }
	    }
	};
	private void changeLogSize()
	{
		int length = Logger.GetLogLength(context);
		DecimalFormat dFormat = new DecimalFormat("0.00");
		
		if (length > 1024 * 1024)
		{
			String formattedString = dFormat.format((double)length / (double)(1024*1024));
			pclearlog.setSummary("Click here to clear log.\nLog size: " + formattedString + " MB");	
		}
		else if (length > 1024)
		{
			String formattedString = dFormat.format((double)length / (double)(1024));
			pclearlog.setSummary("Click here to clear log.\nLog size: " + formattedString + " KB");
		}
		else
		{	
			pclearlog.setSummary("Click here to clear log.\nLog size: " + length + " B");
		}
	}

    private boolean appInstalledOrNot(String uri)
    {
        PackageManager pm = context.getPackageManager();
        boolean app_installed = false;
        try
        {
               pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
               app_installed = true;
        }
        catch (PackageManager.NameNotFoundException e)
        {
               app_installed = false;
        }
        return app_installed ;
    }
}

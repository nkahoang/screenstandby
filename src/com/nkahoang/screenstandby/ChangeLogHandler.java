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

import android.app.AlertDialog;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.widget.CheckBox;
import android.widget.LinearLayout;

public class ChangeLogHandler {
	private static AlertDialog.Builder changelogDialog;
	public static Boolean IsChangeLogRead(final Context context)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getString("changelogread", "").equals(context.getString(R.string.changelogversion));
	}
	public static void setRead(final Context context)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		prefs.edit().putString("changelogread", context.getString(R.string.changelogversion)).commit();
	}
	public static void ShowChangelog(final Activity context)
	{
		try
		{
			changelogDialog = new AlertDialog.Builder(context).setCancelable(false);
	        LayoutInflater inflater = context.getLayoutInflater();
	        LinearLayout layout = new LinearLayout(context);
	        layout.setOrientation(LinearLayout.VERTICAL);
	        inflater.inflate(R.layout.changelog_dialog, layout);
	        changelogDialog.setView(layout);
	        final CheckBox chkDoNotShow = (CheckBox)layout.findViewById(R.id.chkDoNotShow);
	        
	        /*
	        ((Button)layout.findViewById(R.id.btnYoutube)).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
	            	Intent youtubeIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.youtube.com/watch?v=pUpPZPx28Tc"));
	            	youtubeIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
	            	context.startActivity(youtubeIntent);
				}
	        });
	        ((Button)layout.findViewById(R.id.btnDevSetting)).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					//context.startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
					context.startActivity(new Intent(context,
		      			      TroubleshootingActivity.class));
				}
	        });
	        */
	        
	        changelogDialog.setPositiveButton("Dismiss", new DialogInterface.OnClickListener() {
	            // do something when the button is clicked
	            public void onClick(DialogInterface arg0, int arg1) {
	            	if (chkDoNotShow.isChecked())
	            		setRead(context);
	            	arg0.dismiss();
	            	changelogDialog = null;
	             }
	            });
			changelogDialog.show();
		}
		catch(Exception ex)
		{
			
		}
	}
}

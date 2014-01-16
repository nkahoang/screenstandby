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

import com.nkahoang.screenstandby.settings.RootChecker;

import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;

public class TroubleshootingActivity extends BaseActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_troubleshooting);
        ((TextView)this.findViewById(R.id.txtTitle)).setTypeface(FontManager.getThinFont(this));
        ((TextView)this.findViewById(R.id.txtSummary)).setTypeface(FontManager.getCondensedFont(this));
        Button btnDevSettings= (Button)findViewById(R.id.btnTroubleshooting_DevSettings);
        Button btnBusybox = (Button)findViewById(R.id.btnBusybox);
        Button btnCleardata = (Button)findViewById(R.id.btnClearData);
        //Button btnGoToSetting = (Button)findViewById(R.id.btnGotoSetting);
        Button btnXDAThread = (Button)findViewById(R.id.btnXDAThread);
        Button btnTryBeta = (Button)findViewById(R.id.btnTryBeta);
        btnDevSettings.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
		        startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));		        
			}
        });
        btnBusybox.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				RootChecker.CheckForRoot(TroubleshootingActivity.this);
			}});
        
        btnCleardata.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(TroubleshootingActivity.this);
				builder.setTitle("Reset to default");
				builder.setIcon(android.R.drawable.ic_menu_set_as);
				builder.setMessage("Are you sure to clear all data and settings to default?").setPositiveButton("Yes", dialogClickListener)
				.setNegativeButton("No", dialogClickListener).show();
			}});
        /*
        btnGoToSetting.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(TroubleshootingActivity.this,
      			      SettingActivity.class);
					  startActivity(intent);
			}});*/

        btnXDAThread.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
		    	Intent i = new Intent(Intent.ACTION_VIEW);
		    	i.setData(Uri.parse("http://forum.xda-developers.com/showthread.php?p=32732821"));
		    	startActivity(i);
			}});
        btnTryBeta.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
		    	Intent i = new Intent(Intent.ACTION_VIEW);
		    	i.setData(Uri.parse("http://forum.xda-developers.com/showthread.php?p=32732821"));
		    	startActivity(i);
			}});
    }
    

	DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
	    @Override
	    public void onClick(DialogInterface dialog, int which) {
	        switch (which){
	        case DialogInterface.BUTTON_POSITIVE:
	        	PreferenceManager.getDefaultSharedPreferences(TroubleshootingActivity.this).edit().clear().commit();
	        	TroubleshootingActivity.this.finish();
	            break;

	        case DialogInterface.BUTTON_NEGATIVE:
	            dialog.dismiss();
	            break;
	        }
	    }
	};
}

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
import java.io.OutputStreamWriter;
import android.os.Bundle;
import android.os.Parcelable;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RadioButton;
import android.widget.TextView;

public class XTopDPIChanger extends Activity {
	RadioButton rb120, rb240, rbCustom;
	TextView txtDPI, txtCurrentDPI;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent().getStringExtra("DPI") != null) {
        	ChangeDPI(getIntent().getStringExtra("DPI"));
        	this.finish();
        }
        else {
            setContentView(R.layout.activity_xtop_dpichanger);
        	rb120 = (RadioButton) this.findViewById(R.id.rb120);
        	rb240 = (RadioButton) this.findViewById(R.id.rb240);
        	rbCustom = (RadioButton) this.findViewById(R.id.rbCustom);
        	txtDPI = (TextView) this.findViewById(R.id.txtCustomDPI);
        	txtCurrentDPI = (TextView) this.findViewById(R.id.txtCurrentDPI);
        	txtCurrentDPI.setText("Current DPI: " + (int)getResources().getDisplayMetrics().xdpi);
        	rbCustom.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				txtDPI.setEnabled(isChecked);
			}});
        	
        	Button btnCreate = (Button) this.findViewById(R.id.btnCreateDPI);
        	btnCreate.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent serviceIntent = new Intent(Intent.ACTION_MAIN);
			        ComponentName name = new ComponentName(getPackageName(), ".XTopDPIChanger");
			        serviceIntent.setComponent(name);
			        serviceIntent.putExtra("DPI", getDPI());
			        Intent shortcutintent = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
			        shortcutintent.putExtra("duplicate", false);
			        shortcutintent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getDPI() + "DPI");
			        Parcelable icon = Intent.ShortcutIconResource.fromContext(getApplicationContext(), R.drawable.ic_launcher);
			        shortcutintent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon);
			        shortcutintent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, serviceIntent);
			        sendBroadcast(shortcutintent);
				}});

        	((Button) this.findViewById(R.id.btnChangeDPI)).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					ChangeDPI(getDPI());
				}});
        }
    }

    public String getDPI()
    {
    	if (rb120.isChecked()) return "120";
    	else if (rb240.isChecked()) return "240";
    	else if (rbCustom.isChecked())
    	{
    		int dpi = (int)getResources().getDisplayMetrics().xdpi; 
    		try
    		{
    			dpi = Integer.parseInt((String) txtDPI.getText());
    			if ((dpi > 0) && (dpi < 400))
    			{
    				return dpi + "";
    			}
    		}
    		catch(Exception e)
    		{
    			
    		}
    	}
    	return getResources().getDisplayMetrics().xdpi + ""; 
    }
    
    private void ChangeDPI(String NewDPI)
    {
    	try
    	{
	    	String newFileName = "/data/data/" + this.getPackageName() + "/DPIChanger";
			File myFile = new File(newFileName);
			myFile.createNewFile();
			FileOutputStream fOut = new FileOutputStream(myFile);
			OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
			myOutWriter.append("setprop qemu.sf.lcd_density " + NewDPI + "\n");
			myOutWriter.append("stop\n");
			myOutWriter.append("sleep 1000\n");
			myOutWriter.append("echo done > /sdcard/done.txt\n");
			myOutWriter.append("start\n");
			myOutWriter.flush();
			myOutWriter.close();
			Process dpProc = Runtime.getRuntime().exec("sh");
		    DataOutputStream os = new DataOutputStream(dpProc.getOutputStream());
		    os.writeBytes("chmod 777 " + newFileName + "\n");
		    os.writeBytes("su -c " + newFileName + "\n");
		    os.flush();
    	}
    	catch(Exception ex)
    	{
    		Logger.Log(this, ex);
    	}
    }
}

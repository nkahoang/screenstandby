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

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Locale;

import com.nkahoang.screenstandby.BaseActivity;

import android.R;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.widget.TextView;

public class RootChecker {
	public static void CheckForRoot(final Context context)
	{
		final DialogInterface.OnClickListener dialogResultClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
	        	switch (which){
		        	case DialogInterface.BUTTON_POSITIVE:
		        		Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
		        		intent.putExtra(SearchManager.QUERY, "Root guide " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL);
		        		context.startActivity(intent);
		        		break;
		        	case DialogInterface.BUTTON_NEUTRAL:
		        		Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=busybox"));
	            		marketIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
	            		context.startActivity(marketIntent);
		        		break;
		        	case DialogInterface.BUTTON_NEGATIVE:
		        		dialog.dismiss();
		        }
			}
		};
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
		        switch (which){
		        case DialogInterface.BUTTON_POSITIVE:
		        	boolean hasBusyboxWorking = false;
	        	    boolean hasSuWorking = false;
	        	    boolean hasBusyboxBinary = false;
	        	    boolean hasSuBinary = false;
	        	    String busyboxVersion = "";
	        	    try
	        	    {
		        	    File su1 = new File("/system/bin/su");
		        	    File su2 = new File("/system/xbin/su");
		        	    File bb1 = new File("/system/bin/busybox");
		        	    File bb2 = new File("/system/xbin/busybox");
		        	    hasSuBinary = (su1.exists() || su2.exists());
		        	    hasBusyboxBinary = (bb1.exists() || bb2.exists());
	        	    }
	        	    catch(Exception ex)
	        	    {}
	        	    
		            try
		            {
		        		Process proc = Runtime.getRuntime().exec("sh");
		        		
		        		DataOutputStream os = new DataOutputStream(proc.getOutputStream());
		        		DataInputStream is = new DataInputStream(proc.getInputStream());
		        		os.writeBytes("which busybox \n");
		        		os.writeBytes("busybox | head -1 \n");
		        		os.writeBytes("su \n");
		        		os.writeBytes("ls /data \n");
		        		os.writeBytes("exit \n");
		        		os.writeBytes("exit \n");
		        		os.flush();
		        	    BufferedReader br = new BufferedReader(new InputStreamReader(is));
		        	    String line = null;
		        		while ((line = br.readLine()) != null)
		        		{
		        			line = line.toLowerCase(Locale.getDefault());
		        			if (line.contains("/bin/busybox") || line.contains("/xbin/busybox"))
		        				hasBusyboxWorking = true;
		        			if (line.contains("busybox"))
		        				busyboxVersion = line;
		        		}
		        		proc.waitFor();
		        		int exitValue = proc.exitValue();
		        		if (exitValue == 0) hasSuWorking = true;
		        		if (hasSuWorking) hasSuBinary = true;
		        		if (hasBusyboxWorking) hasBusyboxBinary = true;
		            }
		            catch(Exception ex) { return; }
		            String msg = "+ Root binary: <b>" + ((hasSuBinary)?"<font color='#7ccd24'>FOUND</font>":"<font color='red'>NOT FOUND</font>") + "</b><br/>" +
	        		    		 "+ BusyBox binary: <b>" + ((hasBusyboxBinary)?"<font color='#7ccd24'>FOUND</font>":"<font color='red'>NOT FOUND</font>") + "</b><br/>" +
	        		    		 "+ Root permission: <b>" + ((hasSuWorking)?"<font color='#7ccd24'>WORKING</font>":"<font color='red'>NOT WORKING</font>" + ((hasSuBinary)?"</b><i><small> (Did you grant permission?)</small></i><b>":"")) + "</b><br/>" +
	        		    		 "+ BusyBox status: <b>" + ((hasBusyboxWorking)?"<font color='#7ccd24'>WORKING</font>":"<font color='red'>NOT WORKING</font>") + "</b><br/>" +
    		    		    	 ((hasBusyboxWorking)?"<br/><small>BusyBox info: <br/><b>" + busyboxVersion: "</b></small>") + "<br/><br/>";
		            
		            msg += "<big>";
		            if (hasSuWorking && hasBusyboxWorking)
		            	msg += "<i><font color='#7ccd24'><b>Congratulation! </b></font></i> Your device has Root and BusyBox";
		            else
		            {
		            	if (!hasSuWorking && !hasBusyboxWorking) msg += "<i><font color='#ff4e50'>Error</font></i>, please try rooting your device and installing busybox";
		            	else if (!hasSuWorking && hasSuBinary) msg += "<i><font color='#ff4e50'>Error</font></i>, please grant Superuser permission to Screen Standby";
		            	else if (!hasSuWorking && hasSuBinary) msg += "<i><font color='#ff4e50'>Error</font></i>, please grant Superuser permission to Screen Standby";
		            	else if (!hasBusyboxWorking && hasBusyboxBinary) msg += "<i><font color='#ff9200'>Error</font></i>, please try re-installing busybox";
		            	else if (!hasBusyboxWorking && !hasBusyboxBinary) msg += "<i><font color='#ff9200'>Error</font></i>, please try downloading and installing busybox";
		            }
		            msg += "</big>";
	        		AlertDialog.Builder builder = new AlertDialog.Builder(context);
	        		TextView content = new TextView(context);
	                	content.setText(android.text.Html.fromHtml(msg));
	                	content.setTextAppearance(context, R.style.TextAppearance_Medium);
	                	content.setTypeface(BaseActivity.typeface);
	                	content.setPadding(15, 15, 15, 15);
	        		builder
	        		    .setTitle("Root and BusyBox check result")
	        		    .setView(content)
	        		    .setNegativeButton("Dismiss", dialogResultClickListener);
	        		if (!hasBusyboxWorking)
	        			builder = builder.setNeutralButton("Download BusyBox", dialogResultClickListener);
	        		if (!hasSuWorking)
	        			builder = builder.setPositiveButton("Help rooting device", dialogResultClickListener);
	        		builder.show();
	        		break;
		        case DialogInterface.BUTTON_NEGATIVE:
		            dialog.cancel();
		            break;
		        }
		    }
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		String msg = "<b>Screen standby</b> will now check for root access and BusyBox.<br/><br/>" + 
				     "Please grant and remember the app's Superuser access permission if asked " + 
				     "otherwise Screen standby may not function properly.<br/><br/>" + 
				     "This app is guaranteed to <b><i>do no harm</i></b> to your device and data.<br/><br/>Do you wish to continue?<br/>"; 
		builder
		    .setTitle("Root and BusyBox check")
		    .setMessage(android.text.Html.fromHtml(msg))
			.setPositiveButton("Yes", dialogClickListener)
		    .setNegativeButton("No", dialogClickListener);
		builder.show();
	}
}

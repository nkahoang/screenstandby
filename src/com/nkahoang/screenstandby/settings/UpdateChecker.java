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
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.apache.http.params.BasicHttpParams;

import com.nkahoang.screenstandby.Logger;
import com.nkahoang.screenstandby.R;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.Html;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

public class UpdateChecker {

	public static final String updateServiceURI = "http://www.screenstandby.com/update.php";
	public static final String channel = "Playstore";
	//public static final String channel = "XDA";
	public static ProgressDialog progressDialog;
	public static void CheckForUpdate(Context c)
	{
		progressDialog = new ProgressDialog(c);    
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER | ProgressDialog.THEME_HOLO_DARK);
        progressDialog.setTitle("Checking for update");  
        progressDialog.setMessage("Please wait...\n");
        progressDialog.setCancelable(false);
        progressDialog.setIcon(android.R.drawable.ic_menu_upload);
        progressDialog.setIndeterminate(true);
        progressDialog.show();
        new LoadViewTask(c).execute();
	}
	private static class LoadViewTask extends AsyncTask<Void, Integer, Void>  
    {
    	private Context context;
    	private boolean hasNew;
    	private byte error;
    	private String name, date, url, changes;
    	private int code;
    	
    	public LoadViewTask(Context context)
    	{this.context = context;}
    	
		@Override
		protected Void doInBackground(Void... params) {
			error = 0;
			PackageInfo pInfo;
			try {
				pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			    HttpClient client = new DefaultHttpClient();
			    HttpGet httpReq = new HttpGet(updateServiceURI + "?v=" + pInfo.versionCode + "&ch=" + (channel.equals("XDA")?"xda":"ps"));
			    httpReq.setHeader("Content-type", "application/json");
			    //BasicHttpParams p = new BasicHttpParams();
			    //httpReq.setParams(p.setIntParameter("v", pInfo.versionCode).setParameter("ch", channel.equals("XDA")?"xda":"ps"));
			    HttpResponse res = client.execute(httpReq);
			    HttpEntity entity = res.getEntity();
			    BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent(), "UTF-8"), 8);
			    StringBuilder sb = new StringBuilder();

			    String line = null;
			    while ((line = reader.readLine()) != null){sb.append(line + "\n");}
			    String result = sb.toString();
			    JSONObject jObject = new JSONObject(result);
			    hasNew = jObject.getBoolean("new");
			    if (hasNew)
			    {
			    	name = jObject.getString("name");
			    	code = jObject.getInt("code");
			    	date = jObject.getString("date");
			    	url = jObject.getString("url");
			    	changes = jObject.getString("changes");
			    }
			} catch (NameNotFoundException e) {
				Logger.Log(context, e);
				error = 1;
			} catch (ClientProtocolException e) {
				Logger.Log(context, e);
				error = 2;
			} catch (IOException e) {
				Logger.Log(context, e);
				error = 3;
			} catch (JSONException e) {
				Logger.Log(context, e);
				error = 4;
			}
		    return null;
		}
		  
        //after executing the code in the thread  
        @Override  
        protected void onPostExecute(Void result)  
        {
        	progressDialog.dismiss();
        	if (error == 0)
        	{
        		if (hasNew)
            	{
            		AlertDialog.Builder b = new AlertDialog.Builder(context);
            		b.setTitle("New version: " + name);
            		b.setMessage(Html.fromHtml("Changes version <b>" + name + "</b> <small><i>(" + date + ")</i></small>"));
            		TextView v = new TextView(context);
            		v.setText(Html.fromHtml(changes));
            		v.setPadding(10, 0, 10, 0);
            		b.setView(v);
            		b.setNegativeButton("Dismiss",updateDialogClickListener);
            		b.setPositiveButton("Take me to update",updateDialogClickListener);
            		b.create().show();
            		Toast.makeText(context, Html.fromHtml("<b><font color='#8ebc00'>Update found: </font></b> version " + name), Toast.LENGTH_SHORT).show();
            	}
            	else
            		Toast.makeText(context, Html.fromHtml("<b><font color='#3399ff'>No new update found</font></b><br/>You are running the latest version"), Toast.LENGTH_SHORT).show();	
        	}
        	else
        	{
        		String err = "";
        		switch (error)
        		{
        			case 1: err="Invalid app signature (01)";break;
        			case 2:case 3: err="Cannot connect to update server.<br/>Please check your internet connection.(02)";break;
        			case 4: err="Server is having error (04).<br/>Please check again later";break;
        		}
        		Toast.makeText(context, Html.fromHtml("<b><font color='#ff2000'>Error while checking update</font></b><br/>" + err), Toast.LENGTH_SHORT).show();
        	}
        }
        
        private DialogInterface.OnClickListener updateDialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch(which)
				{
					case DialogInterface.BUTTON_NEGATIVE:
						dialog.dismiss();break;
					case DialogInterface.BUTTON_POSITIVE:
						Intent i = new Intent(Intent.ACTION_VIEW);
			        	i.setData(Uri.parse(url));
			        	context.startActivity(i);
						dialog.dismiss();break;
				}
			}
        };
    }
}

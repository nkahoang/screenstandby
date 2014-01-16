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

import android.os.Bundle;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

public class RemoteControlSelectActivity extends BaseActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_remote_control_select);
        
		TextView txtTitle = (TextView)this.findViewById(R.id.txtTitle);
		txtTitle.setTypeface(typefaceLight);
		TextView txtSummary = (TextView)this.findViewById(R.id.txtSummary);
		txtSummary.setTypeface(typeface);
		ImageButton btnHome = (ImageButton)this.findViewById(R.id.btnhome);

		btnHome.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(RemoteControlSelectActivity.this,
						Main.class);
        		startActivity(intent);
				finish();
			}
		});
		
		Button btnReceiver = (Button)this.findViewById(R.id.btnReceiver);
		btnReceiver.setTypeface(typeface);
		Button btnController = (Button)this.findViewById(R.id.btnController);
		btnController.setTypeface(typeface);
		
		btnReceiver.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(RemoteControlSelectActivity.this,
						RemoteServerActivity.class);
        		startActivity(intent);
				finish();
			}
			
		});

		btnController.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(RemoteControlSelectActivity.this,
						RemoteControllerActivity.class);
        		startActivity(intent);
				finish();
			}
			
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_remote_control_select, menu);
		return true;
	}

}

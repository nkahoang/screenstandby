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
import android.os.Parcelable;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;

public class ShortcutActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent().getBooleanExtra("FromShortcut", false)) {
        	Intent serviceIntent = new Intent();
            serviceIntent.setAction(StandbyService.ENABLE_INTENT);
	        sendBroadcast(serviceIntent);
        }
        else {
	        Intent serviceIntent = new Intent(Intent.ACTION_MAIN);
	        ComponentName name = new ComponentName(getPackageName(), ".ShortcutActivity");
	        serviceIntent.setComponent(name);
	        serviceIntent.putExtra("FromShortcut", true);
	        // a Intent to create a shortCut
	        Intent shortcutintent = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
	        //repeat to create is forbidden
	        shortcutintent.putExtra("duplicate", false);
	        //set the name of shortCut
	        shortcutintent.putExtra(Intent.EXTRA_SHORTCUT_NAME, "Screen off shortcut");
	        //set icon
	        Parcelable icon = Intent.ShortcutIconResource.fromContext(getApplicationContext(), R.drawable.ic_launcher);
	        shortcutintent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon);
	        //set the application to lunch when you click the icon
	        shortcutintent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, serviceIntent);
	        //sendBroadcast,done
	        sendBroadcast(shortcutintent);
        }
        this.finish();
    }
}

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

public class ShortcutOnActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent().getBooleanExtra("FromShortcut", false)) {
        	Intent serviceIntent = new Intent();
            serviceIntent.setAction(StandbyService.TOGGLE_INTENT);
	        sendBroadcast(serviceIntent);
        }
        else {
	        Intent serviceIntent = new Intent(Intent.ACTION_MAIN);
	        ComponentName name = new ComponentName(getPackageName(), ".ShortcutOnActivity");
	        serviceIntent.setComponent(name);
	        serviceIntent.putExtra("FromShortcut", true);
	        Intent shortcutintent = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
	        shortcutintent.putExtra("duplicate", false);
	        shortcutintent.putExtra(Intent.EXTRA_SHORTCUT_NAME, "Screen on shortcut");
	        Parcelable icon = Intent.ShortcutIconResource.fromContext(getApplicationContext(), R.drawable.ic_launcher);
	        shortcutintent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon);
	        shortcutintent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, serviceIntent);
	        sendBroadcast(shortcutintent);
        }
        this.finish();
    }
}

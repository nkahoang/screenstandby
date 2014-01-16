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

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.view.ViewGroup;
import android.widget.TextView;

public class BaseActivity extends FragmentActivity {
	//NotificationBar bar = new NotificationBar();
	public static Typeface typefaceLight;
	public static Typeface typeface;

    protected static boolean useMetro = false;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Pass on the activity result to the helper for handling
    	/*
        if (!LicenseManager.GetIabHelper().handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
        else {
            Logger.Log(this, "onActivityResult handled by IABUtil.");
        }*/
    }
    @Override
    protected void onCreate(Bundle arg0)
    {
    	super.onCreate(arg0);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		useMetro = prefs.getBoolean("usemetro", true);
		typefaceLight = FontManager.getThinFont(this);
		typeface = FontManager.getCondensedFont(this);
    	//typefaceLight = useMetro?FontManager.getThinFont(this):Typeface.DEFAULT;
    	//typeface = useMetro?FontManager.getCondensedFont(this):Typeface.DEFAULT;
    }
    
    protected static void SetMetroFont(ViewGroup layout)
    {
    	for (int i = 0; i < layout.getChildCount(); i++)
    	{
    		TextView text = (layout.getChildAt(i) instanceof TextView ? (TextView)layout.getChildAt(i) : null);
    		if (text != null)
    			text.setTypeface(typeface);
    	}
    }
}

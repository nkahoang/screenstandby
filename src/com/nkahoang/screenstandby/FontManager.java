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

import android.content.Context;
import android.graphics.Typeface;

public class FontManager {
	private static Typeface thin, condensed;
	private FontManager()
	{
	}
	public static Typeface getThinFont(Context context)
	{
		if (thin == null)
			thin =Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Thin.ttf"); 
		return thin;
	}
	public static Typeface getCondensedFont(Context context)
	{
		if (condensed == null)
			condensed = Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Light.ttf");
		return condensed;
	}
}

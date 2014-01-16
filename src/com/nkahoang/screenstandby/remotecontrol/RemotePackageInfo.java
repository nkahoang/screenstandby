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
package com.nkahoang.screenstandby.remotecontrol;
import java.io.Serializable;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class RemotePackageInfo implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	byte[] images;
	String packageName, label;
	public RemotePackageInfo (Drawable drawable, String packageName, String label)
	{
		java.io.ByteArrayOutputStream jByteArray = new java.io.ByteArrayOutputStream() ;
		Bitmap.createScaledBitmap(((BitmapDrawable) drawable).getBitmap(),65,65,true).compress(CompressFormat.PNG, 100, jByteArray);		
		images = jByteArray.toByteArray();
		this.packageName = packageName;
		this.label = label;
	}
	public Drawable getDrawable()
	{
		Bitmap b = BitmapFactory.decodeStream(new java.io.ByteArrayInputStream(images));
		BitmapDrawable bd = new BitmapDrawable(b);
		return bd;
	}
	public Drawable getDrawable(Context context)
	{
		Bitmap b = BitmapFactory.decodeStream(new java.io.ByteArrayInputStream(images));
		BitmapDrawable bd = new BitmapDrawable(context.getResources(), b);
		return bd;
	}
	
	public String getPackageName()
	{
		return packageName;
	}
	public String getLabel()
	{
		return label;
	}
}

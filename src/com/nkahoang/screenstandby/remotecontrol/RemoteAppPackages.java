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
import java.util.ArrayList;

import android.graphics.drawable.Drawable;

public class RemoteAppPackages implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	ArrayList<RemotePackageInfo> packages = new ArrayList<RemotePackageInfo>();
	
	public RemotePackageInfo get(int index)
	{
		return (index < packages.size()) ? packages.get(index) : null;		
	}
	
	public void put(Drawable drawable, String packageName, String label)
	{
		packages.add(new RemotePackageInfo(drawable,packageName,label));
	}
	
	public void put(RemotePackageInfo item)
	{
		if (!packages.contains(item)) packages.add(item);
	}
	
	public ArrayList<RemotePackageInfo> getPackages()
	{
		return packages;
	}
}

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

import android.app.Service;
import android.os.Binder;

public class ServiceBinder extends Binder {
	private Service _service;
	public ServiceBinder(Service service)
	{
		_service = service;
	}
	public Service getService() {
		return _service;
    }
}

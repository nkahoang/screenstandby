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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

public class MediaButtonControlReceiver extends BroadcastReceiver {
	static MediaButtonControlReceiverHandler callbackhandler;
	public static void RegisterMediaButtonControlReceiver(MediaButtonControlReceiverHandler handler) {
		callbackhandler = handler;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		Logger.Log(context, intent);
		KeyEvent event = (KeyEvent)intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
		if (event.getAction() == KeyEvent.ACTION_DOWN)
			if (callbackhandler != null)
				callbackhandler.onMediaButtonReceived(event.getKeyCode());
	}
	
	interface MediaButtonControlReceiverHandler
	{
		void onMediaButtonReceived(int keycode);
	}
}

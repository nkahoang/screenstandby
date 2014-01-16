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

import java.io.*;
import android.content.*;
import android.os.Process;
import android.util.Log;

public class ExceptionHandler implements java.lang.Thread.UncaughtExceptionHandler {
    private final Context myContext;
    public ExceptionHandler(Context context) {
        myContext = context;
    }

    public void uncaughtException(Thread thread, Throwable exception) {
        StringWriter stackTrace = new StringWriter();
        exception.printStackTrace(new PrintWriter(stackTrace));
        System.err.println(stackTrace);
        java.io.StringWriter errMessage = new StringWriter();
        PrintWriter pw = new PrintWriter(errMessage);
        exception.printStackTrace(pw);
        Log.e("Screen standby", errMessage.toString());
        Logger.Log(myContext, "Uncaught exception here:");
        Logger.Log(myContext, exception);
        try
        {
        	Intent serviceIntent = new Intent();
        	serviceIntent.setAction(StandbyService.TOGGLE_INTENT);
        	myContext.sendBroadcast(serviceIntent);
        }
        catch(Exception e)
        {
        	Logger.Log(myContext, e);	
        }
        Process.killProcess(Process.myPid());
        System.exit(10);
    }
}

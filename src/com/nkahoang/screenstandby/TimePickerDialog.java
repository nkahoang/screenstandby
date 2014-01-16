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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

/**
 * A dialog that prompts the user for the time of day using a {@link TimePicker}.
 */
public class TimePickerDialog extends AlertDialog implements OnClickListener, 
        TimePicker.OnTimeChangedListener {

    /**
     * The callback interface used to indicate the user is done filling in
     * the time (they clicked on the 'Set' button).
     */
    public interface OnTimeSetListener {

        /**
         * @param view The view associated with this listener.
         * @param hour The hour that was set.
         * @param minute The minute that was set.
         * @param second The second that was set.
         * @param cancel If the set is cancel
         */
        void onTimeSet(TimePicker view, int hour, int minute, int second, boolean cancel);
    }

    private static final String HOUR = "hour";
    private static final String MINUTE = "minute";
    private static final String SECOND = "second";
    
    private final TimePicker mTimePicker;
    private final OnTimeSetListener mCallback;
    
    private int mInitialHour;
    private int mInitialMinute;
    private int mInitialSecond;

    /**
     * @param context Parent.
     * @param callBack How parent is notified.
     * @param hour The initial hour.
     * @param minute The initial minute.
     * @param second The initial second.
     */
    public TimePickerDialog(Context context,
            OnTimeSetListener callBack,
            int hour, int minute, int second) {
//        this(context, R.style.Theme_Dialog_Alert,
        this(context, android.R.style.Theme_Holo_Dialog, callBack, hour, minute, second);
    }

    /**
     * @param context Parent.
     * @param theme the theme to apply to this dialog
     * @param callBack How parent is notified.
     * @param hour The initial hour.
     * @param minute The initial minute.
     * @param second The initial second.
     */
    public TimePickerDialog(Context context,
            int theme,
            OnTimeSetListener callBack,
            int hour, int minute, int second) {
        super(context, theme);

        mCallback = callBack;
        mInitialHour = hour;
        mInitialMinute = minute;
        mInitialSecond = second;

        updateTitle(mInitialHour, mInitialMinute, mInitialSecond);
        
        setButton(BUTTON_POSITIVE, "OK", this);
        setButton(BUTTON_NEGATIVE, "Cancel", this);
        setIcon(android.R.drawable.ic_menu_agenda);

        LayoutInflater inflater = 
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.time_picker_dialog, null);
        setView(view);
        mTimePicker = (TimePicker) view.findViewById(R.id.timePicker);

        // initialize state
        mTimePicker.setCurrentHour(mInitialHour);
        mTimePicker.setCurrentMinute(mInitialMinute);
        mTimePicker.setCurrentSecond(mInitialSecond);
        mTimePicker.setOnTimeChangedListener(this);
    }

    public void onClick(DialogInterface dialog, int which) {
        boolean cancel = false;
        Log.i(this.getClass().getName(), Integer.valueOf(which).toString());
        if (which == BUTTON_NEGATIVE) {cancel=true;}
        if (mCallback != null) {
            mTimePicker.clearFocus();
            mCallback.onTimeSet(mTimePicker, mTimePicker.getCurrentHour(), 
                    mTimePicker.getCurrentMinute(), mTimePicker.getCurrentSecond(),cancel);
        }
    }

    public void onTimeChanged(TimePicker view, int hour, int minute, int second) {
        // FIXME: Enable them only the first time
        if (hour > 0 || minute > 0 || second > 0) { 
            getButton(BUTTON_POSITIVE).setEnabled(true);
            getButton(BUTTON_NEGATIVE).setEnabled(true);
        } else {
            getButton(BUTTON_POSITIVE).setEnabled(false);
            getButton(BUTTON_NEGATIVE).setEnabled(false);
        }
        updateTitle(hour, minute, second);
    }
    
    public void updateTime(int hour, int minute, int second) {
        mTimePicker.setCurrentHour(hour);
        mTimePicker.setCurrentMinute(minute);
        mTimePicker.setCurrentSecond(second);
    }
    
    public static String pad(int c) {
        if (c >= 10)
            return String.valueOf(c);
        else
            return "0" + String.valueOf(c);
    }
    
    private void updateTitle(int hour, int minute, int second) {
        String title = "Timer: ";
        setTitle(title+" "+
                 pad(hour)+":"+pad(minute)+":"+pad(second));
    }
    
    @Override
    public Bundle onSaveInstanceState() {
        Bundle state = super.onSaveInstanceState();
        state.putInt(HOUR, mTimePicker.getCurrentHour());
        state.putInt(MINUTE, mTimePicker.getCurrentMinute());
        state.putInt(SECOND, mTimePicker.getCurrentSecond());
        return state;
    }
    
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        int hour = savedInstanceState.getInt(HOUR);
        int minute = savedInstanceState.getInt(MINUTE);
        int second = savedInstanceState.getInt(SECOND);
        mTimePicker.setCurrentHour(hour);
        mTimePicker.setCurrentMinute(minute);
        mTimePicker.setCurrentSecond(second);
        mTimePicker.setOnTimeChangedListener(this);
        updateTitle(hour, minute, second);
    }
}

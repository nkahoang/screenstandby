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

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class IconContextMenu implements DialogInterface.OnCancelListener, 
										DialogInterface.OnDismissListener{

	private static final int LIST_PREFERED_HEIGHT = 65;
	
	private IconMenuAdapter menuAdapter = null;
	private Context parentActivity = null;
	private Typeface typeface = null;
	private IconContextMenuOnClickListener clickHandler = null;
	
	/**
	 * constructor
	 * @param parent
	 * @param id
	 */
	public IconContextMenu(Context parent, int id) {
		this.parentActivity = parent;
		
		menuAdapter = new IconMenuAdapter(parentActivity);
		typeface = FontManager.getCondensedFont(parent);
	}
	
	public void addItem(Drawable image, CharSequence title, int actionTag)
	{
		menuAdapter.addItem(new IconContextMenuItem(image, title, actionTag));
	}
	/**
	 * Add menu item
	 * @param menuItem
	 */
	public void addItem(Resources res, CharSequence title,
			int imageResourceId, int actionTag) {
		menuAdapter.addItem(new IconContextMenuItem(res, title, imageResourceId, actionTag));
	}
	
	public void addItem(Resources res, int textResourceId,
			int imageResourceId, int actionTag) {
		menuAdapter.addItem(new IconContextMenuItem(res, textResourceId, imageResourceId, actionTag));
	}
	
	/**
	 * Set menu onclick listener
	 * @param listener
	 */
	public void setOnClickListener(IconContextMenuOnClickListener listener) {
		clickHandler = listener;
	}
	
	/**
	 * Create menu
	 * @return
	 */
	public Dialog createMenu(String menuItitle) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(parentActivity);
        builder.setTitle(menuItitle);
        builder.setAdapter(menuAdapter, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialoginterface, int i) {
				IconContextMenuItem item = (IconContextMenuItem) menuAdapter.getItem(i);
				
				if (clickHandler != null) {
					clickHandler.onClick(item.actionTag);
				}
			}
		});

        builder.setInverseBackgroundForced(true);

        AlertDialog dialog = builder.create();
        dialog.setOnCancelListener(this);
        dialog.setOnDismissListener(this);
        dialog.setIcon(R.drawable.appico);
        return dialog;
	}
	
	public void onCancel(DialogInterface dialog) {
        cleanup();
    }

    public void onDismiss(DialogInterface dialog) {
    }
   
    private void cleanup() {
    }
	
    /**
     * IconContextMenu On Click Listener interface
     */
    public interface IconContextMenuOnClickListener {
		public abstract void onClick(int menuId);
    }
    
	/**
	 * Menu-like list adapter with icon
	 */
    protected class IconMenuAdapter extends BaseAdapter {
		private Context context = null;
		
	    private ArrayList<IconContextMenuItem> mItems = new ArrayList<IconContextMenuItem>();
		
	    public IconMenuAdapter(Context context) {
	    	this.context = context;
	    }
	    
	    /**
	     * add item to adapter
	     * @param menuItem
	     */
	    public void addItem(IconContextMenuItem menuItem) {
	    	mItems.add(menuItem);
	    }
	    
		@Override
		public int getCount() {
			return mItems.size();
		}

		@Override
		public Object getItem(int position) {
			return mItems.get(position);
		}

		@Override
		public long getItemId(int position) {
			IconContextMenuItem item = (IconContextMenuItem) getItem(position);
			return item.actionTag;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			IconContextMenuItem item = (IconContextMenuItem) getItem(position);
			Resources res = parentActivity.getResources();
			
			if (convertView == null) {
	        	TextView temp = new TextView(context);
	        	AbsListView.LayoutParams param = new AbsListView.LayoutParams(AbsListView.LayoutParams.FILL_PARENT, 
	        																  AbsListView.LayoutParams.WRAP_CONTENT);
	        	temp.setLayoutParams(param);
	        	temp.setPadding((int)toPixel(res, 20), 2, (int)toPixel(res, 20), 2);
	        	temp.setGravity(android.view.Gravity.CENTER_VERTICAL);
	        	Theme th = context.getTheme();
				TypedValue tv = new TypedValue();
				
				if (th.resolveAttribute(android.R.attr.textAppearanceLargeInverse, tv, true)) {
					temp.setTextAppearance(context, tv.resourceId);
				}
	        	
	        	temp.setMinHeight(LIST_PREFERED_HEIGHT);
	        	
	        	temp.setCompoundDrawablePadding((int)toPixel(res, 14));
	        	convertView = temp;
			}
			
			TextView textView = (TextView) convertView;
			textView.setTag(item);
			textView.setText(item.text);
			textView.setTextColor(Color.WHITE);
			textView.setTypeface(typeface);
			
			Bitmap bitmap = ((BitmapDrawable) item.image).getBitmap();
			Drawable d = new BitmapDrawable(parent.getResources(), Bitmap.createScaledBitmap(bitmap, LIST_PREFERED_HEIGHT, LIST_PREFERED_HEIGHT, true));
			textView.setCompoundDrawablesWithIntrinsicBounds(d, null, null, null);
        	
	        return textView;
		}
		
		private float toPixel(Resources res, int dip) {
			float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, res.getDisplayMetrics());
			return px;
		}
	}

	/**
	 * menu-like list item with icon
	 */
    protected class IconContextMenuItem {
		public final CharSequence text;
		public final Drawable image;
		public final int actionTag;

		public IconContextMenuItem(Drawable image, CharSequence text, int Tag)
		{
			this.text = text;
			this.actionTag = Tag;
			this.image = image;
		}
		public IconContextMenuItem(Resources res, int textResourceId,
				int imageResourceId, int actionTag) {
			text = res.getString(textResourceId);
			if (imageResourceId != -1) {
				image = res.getDrawable(imageResourceId);
			} else {
				image = null;
			}
			this.actionTag = actionTag;
		}

		public IconContextMenuItem(Resources res, CharSequence title,
				int imageResourceId, int actionTag) {
			text = title;
			if (imageResourceId != -1) {
				image = res.getDrawable(imageResourceId);
			} else {
				image = null;
			}
			this.actionTag = actionTag;
		}
	}
}

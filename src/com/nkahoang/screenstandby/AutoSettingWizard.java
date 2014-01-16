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
import java.util.HashMap;
import com.nkahoang.screenstandby.settings.DeviceSettings;
import com.nkahoang.screenstandby.settings.RootChecker;
import com.nkahoang.screenstandby.settings.DeviceSettings.ConfEntry;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;

public class AutoSettingWizard extends BaseActivity {
	
	private static final int NUM_PAGES = 4;
	private Button mButtonBack, mButtonNext;
	private ViewPager mPager;
    private SettingsPagerAdapter mPagerAdapter;
    private byte prevPage = 0;
    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        // super.onSaveInstanceState(outState);
    }
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_auto_setting_wizard);
		
		
		Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));
		final TextView txtTitle = ((TextView)this.findViewById(R.id.txtTitle));
        txtTitle.setTypeface(typefaceLight);
        
        mButtonNext = (Button)this.findViewById(R.id.btnNext);
        mButtonBack = (Button)this.findViewById(R.id.btnBack);
        
        mPager = (ViewPager) findViewById(R.id.mainpager);
        mPagerAdapter = new SettingsPagerAdapter(this.getSupportFragmentManager());
        mPager.setOnPageChangeListener(new OnPageChangeListener() {
			@Override
			public void onPageScrollStateChanged(int arg0) {	
			}
			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
			}

			@Override
			public void onPageSelected(int arg0) {
				checkPage(arg0);
			}
        });
        mPager.setAdapter(mPagerAdapter);
        

        mButtonBack.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (mPager.getCurrentItem() == 0){
					finishWizard();
				}
				else mPager.setCurrentItem(mPager.getCurrentItem() - 1, true);
				checkPage(mPager.getCurrentItem());
			}
        });

        mButtonNext.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (mPager.getCurrentItem() == (NUM_PAGES - 1)) {
					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(AutoSettingWizard.this);
					prefs.edit().putBoolean("usemetro", mPagerAdapter.step3.chkBox.isChecked()).commit();
					
					ConfEntry dev = DeviceSettings.GetDevicesList().get(mPagerAdapter.step1.spDevices.getSelectedItemPosition());
					ConfEntry uc = DeviceSettings.GetUsecaseList().get(mPagerAdapter.step1.spUsecases.getSelectedItemPosition());
					DeviceSettings.ApplySettings(AutoSettingWizard.this, new ConfEntry[] {dev,uc}, mPagerAdapter.step1.resetDefault);
					finishWizard();
				}
				else mPager.setCurrentItem(mPager.getCurrentItem() + 1, true);
				checkPage(mPager.getCurrentItem());
			}
        });
	}
	
	private void finishWizard()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.edit().putBoolean("wizardRun", true).commit();
		AutoSettingWizard.this.finish();

        Intent i = new Intent(this,com.nkahoang.screenstandby.Main.class);
        this.startActivity(i);
	}

	private void checkPage(int arg0)
	{
		mButtonBack.setText((arg0==0)?"Skip wizard":"Previous");
		mButtonNext.setText((arg0<(NUM_PAGES-1))?"Next":"Finish");
		if (arg0 == 2)
		{
			ConfEntry dev = DeviceSettings.GetDevicesList().get(mPagerAdapter.step1.selectedDevice);
			ConfEntry uc = DeviceSettings.GetUsecaseList().get(mPagerAdapter.step1.selectedUsecase);
			ConfEntry merged = DeviceSettings.MergeEntries(new ConfEntry[] {dev,uc});
			boolean enableHW =(Build.VERSION.SDK_INT > 16) && (merged.SetJBDisplayHWOverlay != null && merged.SetJBDisplayHWOverlay.equals("true"));
			boolean enableWarnRoot = ((merged.SetRequireRoot != null && merged.SetRequireRoot.equals("true")));
			if (!(enableHW || enableWarnRoot))
			{
				if (prevPage == 1) mPager.setCurrentItem(4, false);
				else mPager.setCurrentItem(1, false);
			}
			else
			{
				mPagerAdapter.step2.SetWarnHWOverlayVisibility(enableHW);
				mPagerAdapter.step2.SetWarnRootVisibility(enableWarnRoot);
			}
		}
		else if (arg0 == 3)
		{
			ConfEntry dev = DeviceSettings.GetDevicesList().get(mPagerAdapter.step1.spDevices.getSelectedItemPosition());
			ConfEntry uc = DeviceSettings.GetUsecaseList().get(mPagerAdapter.step1.spUsecases.getSelectedItemPosition());
			mPagerAdapter.step3.txtSummary.setText(DeviceSettings.GetPendingSettings(AutoSettingWizard.this, new ConfEntry[] {dev,uc}, mPagerAdapter.step1.resetDefault));
		}
		prevPage = (byte)mPager.getCurrentItem();
	}
	
	class SettingsPagerAdapter extends FragmentStatePagerAdapter {
		public Step0PageFragment step0; Step1PageFragment step1; Step2PageFragment step2; Step3PageFragment step3;
        public SettingsPagerAdapter(FragmentManager fm) {
            super(fm);
            step0 = new Step0PageFragment();
            step1 = new Step1PageFragment();
            step2 = new Step2PageFragment();
            step3 = new Step3PageFragment();
        }
        @Override
        public Fragment getItem(int position) {
        	switch (position)
        	{
        		case 0: return step0;
        		case 1: return step1;
        		case 2: return step2;
        		case 3: return step3;
        	}
            return null;
        }

        @Override
        public CharSequence getPageTitle (int position)
        {
        	switch (position)
        	{
        		case 0:
        			return "intro";
        		case 1:
        			return "devices";
        		case 2:
        			return "notes";
        		case 3:
        			return "summary";
        	}
        	return "";
        }
        @Override
        public int getCount() {
            return NUM_PAGES;
        }
    }
	
	public static class Step0PageFragment extends Fragment {
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
        	ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.auto_settings_step0, container, false);
            SetMetroFont(rootView);
        	
        	return rootView;
        }
    }
	
	public static class Step1PageFragment extends Fragment {
		Spinner spDevices, spUsecases;
		CheckBox chkResetToDefault;
		static int selectedDevice = -1;
		static int selectedUsecase = -1;
		static boolean resetDefault = false;
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        	ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.auto_settings_step1, container, false);
            SetMetroFont(rootView);

			spDevices = (Spinner) rootView.findViewById(R.id.spinnerDevices);
			spUsecases = (Spinner) rootView.findViewById(R.id.spinnerUsecase);
			chkResetToDefault = (CheckBox) rootView.findViewById(R.id.chkResetAll);
			chkResetToDefault.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
					resetDefault = arg1;	
				}
			});
			chkResetToDefault.setTypeface(typeface);
			((TextView) rootView.findViewById(R.id.txtInstructionStep1)).setTypeface(typefaceLight);
			((TextView) rootView.findViewById(R.id.txtSelectDevice)).setTypeface(typeface);
			((TextView) rootView.findViewById(R.id.txtSelectUsecase)).setTypeface(typeface);
			
            DeviceSettings.Initialize(getActivity());
			ArrayList<HashMap<String,String>> dataDevices = new ArrayList<HashMap<String,String>>();
			int pos = 0;
            for(ConfEntry entry : DeviceSettings.GetDevicesList())
            {
            	HashMap<String,String> h = new HashMap<String, String>();
            	h.put("title", entry.DisplayName);
            	if (entry.id == DeviceSettings.GetLastMatch().id)
            		pos = dataDevices.size();
            	dataDevices.add(h);
            }
			spDevices.setAdapter(new SimpleAdapter(getActivity(), dataDevices, R.layout.list_item, new String[] {"title"}, new int[]{R.id.txtLabel}));
			spDevices.setSelection(pos);
			spDevices.setOnItemSelectedListener(new OnItemSelectedListener() {

				@Override
				public void onItemSelected(AdapterView<?> arg0, View arg1,
						int arg2, long arg3) {
					selectedDevice = arg2;
				}

				@Override
				public void onNothingSelected(AdapterView<?> arg0) {
					selectedDevice = -1;
				}
			});
			ArrayList<HashMap<String,String>> dataUsecases = new ArrayList<HashMap<String,String>>();
			pos = 0;
            for(ConfEntry entry : DeviceSettings.GetUsecaseList())
            {
            	HashMap<String,String> h = new HashMap<String, String>();
            	h.put("title", entry.DisplayName);
            	dataUsecases.add(h);
            }
            
            spUsecases.setAdapter(new SimpleAdapter(getActivity(), dataUsecases, R.layout.list_item, new String[] {"title"}, new int[]{R.id.txtLabel}));
			spUsecases.setSelection(pos);
			spUsecases.setOnItemSelectedListener(new OnItemSelectedListener() {

				@Override
				public void onItemSelected(AdapterView<?> arg0, View arg1,
						int arg2, long arg3) {
					selectedUsecase = arg2;
				}

				@Override
				public void onNothingSelected(AdapterView<?> arg0) {
					selectedUsecase = -1;
				}
			});
			SetSpinnerFont(spDevices);
			SetSpinnerFont(spUsecases);
        	return rootView;
        }
        
        private void SetSpinnerFont(Spinner spinner)
        {
        	for(int i = 0; i < spinner.getChildCount(); i++)
        		((TextView)(((ViewGroup)(spinner.getChildAt(i))).getChildAt(0))).setTypeface(BaseActivity.typefaceLight);
        }
    }
	
	public static class Step2PageFragment extends Fragment {
		View groupHWOverlay, groupRoot;
		Button btnCheckRoot, btnCheckHW;
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
        	ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.auto_settings_step2, container, false);
        	((TextView) rootView.findViewById(R.id.txtHWOverlay)).setTypeface(BaseActivity.typefaceLight);
        	((TextView) rootView.findViewById(R.id.txtRoot)).setTypeface(BaseActivity.typefaceLight);
        	btnCheckRoot = (Button) rootView.findViewById(R.id.btnCheckRoot);
        	btnCheckRoot.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					RootChecker.CheckForRoot(getActivity());
				}
        	});
        	btnCheckHW = (Button) rootView.findViewById(R.id.btnDeveloperSettings);
        	btnCheckHW.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
				}
        	});
        	return rootView;
        }
        @Override
        public void onResume()
        {
        	groupHWOverlay = this.getView().findViewById(R.id.pnHWOverlay);
        	groupRoot = this.getView().findViewById(R.id.pnRoot);
        	super.onResume();
        }
        public void SetWarnHWOverlayVisibility(boolean isVisible)
        {
        	if (groupHWOverlay != null)
        		groupHWOverlay.setVisibility(isVisible?View.VISIBLE:View.GONE);	
        }
        public void SetWarnRootVisibility(boolean isVisible)
        {
        	if (groupRoot != null)
        		groupRoot.setVisibility(isVisible?View.VISIBLE:View.GONE);	
        }
    }
	public static class Step3PageFragment extends Fragment {
		TextView txtSummary;
		CheckBox chkBox;
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
        	ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.auto_settings_step3, container, false);
            txtSummary = (TextView)rootView.findViewById(R.id.txtSummary);
            txtSummary.setTypeface(typefaceLight);
            chkBox = (CheckBox) rootView.findViewById(R.id.chkNewInterface);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            chkBox.setChecked(prefs.getBoolean("usemetro", true));
            chkBox.setText(Html.fromHtml("Use new interface <small><i>(can be changed later in Settings -> Extra settings)</i></small>"));
        	return rootView;
        }
    }
}

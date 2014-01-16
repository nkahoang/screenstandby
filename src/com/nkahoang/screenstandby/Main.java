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

import java.text.SimpleDateFormat;
import java.util.Locale;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.Animator.AnimatorListener;
import com.nineoldandroids.animation.AnimatorInflater;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.animation.AnimatorProxy;
import com.nkahoang.screenstandby.PreferenceListFragment.OnPreferenceAttachedListener;
import com.nkahoang.screenstandby.remotecontrol.ClientService;
import com.nkahoang.screenstandby.remotecontrol.ServerService;
import com.nkahoang.screenstandby.settings.UpdateChecker;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

public class Main extends BaseActivity implements OnPreferenceAttachedListener, OnPreferenceChangeListener, OnPreferenceClickListener  {

    private static final int NUM_PAGES = 3;
    
    private ViewPager mPager;
    private MainPagerAdapter mPagerAdapter;
    private static PackageInfo pInfo;
    
	private void warning()
	{
		try
		{
		String build = android.os.Build.MODEL.toLowerCase();
			if (build.contains("i535") || build.contains("d2vzw") ||
				build.contains("d2spr") || build.contains("d2tmo"))
			{
				TextView tv = (TextView)this.findViewById(R.id.textView1);
				Spannable warning = new SpannableString ( "IMPORTANT! This app is currently has compatibility problem with your device (" + build + ") as it may freezes up the screen. I am sorry for the problem and is trying my best to fix this" );
				warning.setSpan( new ForegroundColorSpan( Color.RED ), 0, warning.length(), 0 );
				tv.setText(warning);
			}
		}
		catch(Exception ex){}
	}

	static ObjectAnimator animFlippingWholePage;
	static ObjectAnimator animFlippingRight, animFlippingLeft;
	static ObjectAnimator animFlippingBack, animFlippingBackFromLeft;
	static AnimatorSet animZoomingIn, animZoomingOut;
	static byte lastAnimator = -1;
    private static OnTouchListener MetroButtonFlipListener = new OnTouchListener() {

		@Override
		public boolean onTouch(View arg0, MotionEvent arg1) {
			if (useMetro)
			switch (arg1.getAction())
			{
				case MotionEvent.ACTION_DOWN:
					if (arg1.getX() > (arg0.getWidth() * 2 / 3))
					{
						animFlippingRight.setTarget(arg0);
						animFlippingRight.start();
						lastAnimator = 0;
					}
					else if (arg1.getX() < (arg0.getWidth() / 3))
					{
						animFlippingLeft.setTarget(arg0);
						animFlippingLeft.start();
						lastAnimator = 1;
					}
					else
					{
						animZoomingOut.setTarget(arg0);
						animZoomingOut.start();
						lastAnimator = 2;
					}
					break;
				case MotionEvent.ACTION_UP:
				case MotionEvent.ACTION_OUTSIDE:
				case MotionEvent.ACTION_CANCEL:
					switch (lastAnimator)
					{
						case 0:
							animFlippingBack.setTarget(arg0);
							animFlippingBack.start();break;
						case 1:
							animFlippingBackFromLeft.setTarget(arg0);
							animFlippingBackFromLeft.start();break;
						case 2:
							animZoomingIn.setTarget(arg0);
							animZoomingIn.start();break;
					}
					lastAnimator = -1;
					break;
			}
			return false;
		}};
	
	@SuppressLint("NewApi")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if (!prefs.getBoolean("wizardRun", false))
		{
	        Intent i = new Intent(this,com.nkahoang.screenstandby.AutoSettingWizard.class);
	        this.startActivity(i);
	        this.finish();
		}
        
        animFlippingWholePage = (ObjectAnimator) AnimatorInflater.loadAnimator(Main.this, R.animator.flipping_whole_page);
        
        animFlippingRight = (ObjectAnimator) AnimatorInflater.loadAnimator(Main.this, R.animator.flipping_right);
    	animFlippingLeft = (ObjectAnimator) AnimatorInflater.loadAnimator(Main.this, R.animator.flipping_left);
    	
    	animFlippingBack = (ObjectAnimator) AnimatorInflater.loadAnimator(Main.this, R.animator.flipping_back);
    	animFlippingBackFromLeft = (ObjectAnimator) AnimatorInflater.loadAnimator(Main.this, R.animator.flipping_back_from_left);
    	
    	animZoomingOut = (AnimatorSet) AnimatorInflater.loadAnimator(Main.this, R.animator.zoomout);
    	animZoomingIn = (AnimatorSet) AnimatorInflater.loadAnimator(Main.this, R.animator.zoomin);
		
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));
        
        setContentView(useMetro?R.layout.activity_main:R.layout.activity_main_alt);
        View rootView = this.findViewById(R.id.rootView);
        animFlippingWholePage.setTarget(rootView);
        if (!useMetro) animFlippingWholePage.setDuration(0);
        if (android.os.Build.VERSION.SDK_INT >= 11)
        {
        	rootView.setPivotX(0);
    		rootView.setPivotY(0);
        }
    	AnimatorProxy.wrap(rootView).setPivotX(0);
    	AnimatorProxy.wrap(rootView).setPivotY(0);
        
        mPager = (ViewPager) findViewById(R.id.mainpager);
        mPagerAdapter = new MainPagerAdapter(this.getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);

        final TextView txtTitle = ((TextView)this.findViewById(R.id.txtTitle));
        txtTitle.setTypeface(useMetro?typefaceLight:typeface);
        final Button txtTitleNext = ((Button)this.findViewById(R.id.txtTitleNex));
        txtTitleNext.setTypeface(typefaceLight);
        txtTitleNext.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {	
				if (mPager.getCurrentItem() < (NUM_PAGES - 1))
				{
					mPager.setCurrentItem(mPager.getCurrentItem() + 1);
				}
			}
        });
        
        final View indicator1 =Main.this.findViewById(R.id.indicator1);
        final View indicator2 =Main.this.findViewById(R.id.indicator2);
        final View indicator3 =Main.this.findViewById(R.id.indicator3);
        
        mPager.setOnPageChangeListener(new OnPageChangeListener() {

			@Override
			public void onPageScrollStateChanged(int arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onPageSelected(int arg0) {
				txtTitle.setText(mPagerAdapter.getPageTitle(arg0));
				txtTitleNext.setText(mPagerAdapter.getPageTitle(arg0+1));
				int selected = useMetro?R.drawable.circleindicator_selected:R.drawable.barindicator_selected;
				int normal = useMetro?R.drawable.circleindicator:R.drawable.barindicator;
				indicator1.setBackgroundResource(arg0 == 0?selected:normal);
				indicator2.setBackgroundResource(arg0 == 1?selected:normal);
				indicator3.setBackgroundResource(arg0 == 2?selected:normal);
			}});
        
        if (!useMetro) {
        	((ImageButton) this.findViewById(R.id.btnBackToMain)).setOnClickListener(new OnClickListener() {
    			@Override
    			public void onClick(View v) {
    				mPager.setCurrentItem(0);
    			}}
            );
        }
        ImageButton btnOverflows = (ImageButton) this.findViewById(R.id.btnOverflows);
        btnOverflows.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Main.this.openOptionsMenu();
			}}
        );
        
        ImageButton btnSettings = (ImageButton) this.findViewById(R.id.btnSettings);
        btnSettings.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				openSettings();
			}}
        );
        
        ImageButton btnVideoClip = (ImageButton) this.findViewById(R.id.btnVidClip);
        btnVideoClip.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
		        openVideoClip();
			}}
        );
        
        ImageButton btnTroubleshooting = (ImageButton) this.findViewById(R.id.btntroubleshooting);
        btnTroubleshooting.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				animFlippingWholePage.start();
				animFlippingWholePage.removeAllListeners();
				animFlippingWholePage.addListener(new AnimatorListener(){

					@Override
					public void onAnimationCancel(Animator arg0) {
					}

					@Override
					public void onAnimationEnd(Animator arg0) {
	    				Intent intent = new Intent(Main.this,
	    	      			      TroubleshootingActivity.class);
    	            		startActivity(intent);
    	            	animFlippingWholePage.reverse();
					}

					@Override
					public void onAnimationRepeat(Animator arg0) {
					}

					@Override
					public void onAnimationStart(Animator arg0) {
					}
				});
			}}
        );
        
        /*
        if (!ChangeLogHandler.IsChangeLogRead(this))
        {
        	ChangeLogHandler.ShowChangelog(this);
        }
        */
        //warning();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }  
    
    private final String[] clipsName = {"1. Basic features demo", "2. New features demo", "3. XDA app review"};
    private final String[] clipsUrl = {"zV0ygqfS-M0", "pUpPZPx28Tc", "5U4Er3LuHZ8"};
    
    private void openVideoClip()
    {
    	Builder watchDialog = new AlertDialog.Builder(this).setCancelable(true);
		watchDialog.setTitle("Select a video clip:");
		watchDialog.setIcon(android.R.drawable.ic_menu_slideshow);
		watchDialog.setItems(clipsName, new android.content.DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (which >= 0) {
					Intent youtubeIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.youtube.com/watch?v=" + clipsUrl[which]));
	            	youtubeIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
	            	startActivity(youtubeIntent);							
				}
			}});
		watchDialog.setNegativeButton("Dismiss", null);
		watchDialog.show();
    }
    private void openSettings()
    {
    	mPager.setCurrentItem(1);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
	        case R.id.menu_toggle:
	        {
    			Intent intentToggle = new Intent("com.nkahoang.screenstandby.action.TOGGLE");
    			Main.this.sendBroadcast(intentToggle);
    			break;
	        }
	        case R.id.menu_exit:
	        {
	        	this.finish();
	        	break;
	        }
            case R.id.menu_debuginfo:
				Logger.ShowLog(Main.this);
				break;
            case R.id.menu_rate:
            	Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.nkahoang.screenstandby"));
            	marketIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            	startActivity(marketIntent);
            	break;
            case R.id.menu_watch_youtube:
            	openVideoClip();
            	break;
            case R.id.menu_troubleshooting:
            	Intent intentTroubleshooting = new Intent(Main.this,
      			      TroubleshootingActivity.class);
            	startActivity(intentTroubleshooting);
            	break;
            case R.id.menu_manual:
            	Intent intentManual = new Intent(Main.this,
            			      ManualBrightnessChangerActivity.class);
            	startActivity(intentManual);
                break;
            case R.id.menu_setting:
            	openSettings();
                break;
            case R.id.menu_devmessage:
            	ChangeLogHandler.ShowChangelog(this);
            	break;
                /*
            case R.id.menu_changedpi:
            	Intent dpiintent = new Intent(Main.this,
      			      XTopDPIChanger.class);
            	startActivity(dpiintent);
            	break;*/
        }
        return true;
    }
    public static class AppInfoPageFragment extends Fragment {
    	void displayLicenseInfo()
    	{
    	}
    	TextView licenseInfo;
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
        	ViewGroup rootView = (ViewGroup) inflater.inflate(
                    R.layout.app_info_fragment, container, false);
        	try {
				pInfo = AppInfoPageFragment.this.getActivity().getPackageManager().getPackageInfo( AppInfoPageFragment.this.getActivity().getPackageName(), 0);
	        	((TextView)rootView.findViewById(R.id.txtVersionText)).setText("version: " + pInfo.versionName);
			} catch (NameNotFoundException e) {
			}

            //Typeface typefaceLight = FontManager.getThinFont(this.getActivity());
            Typeface typeface = FontManager.getCondensedFont(this.getActivity());
            ViewGroup layout = (ViewGroup)rootView.getChildAt(0);
            
        	for (int i = 0; i < layout.getChildCount(); i++)
        	{
        		TextView text = (layout.getChildAt(i) instanceof TextView ? (TextView)layout.getChildAt(i) : null);
        		if (text != null)
        			text.setTypeface(typeface);
        	}
        	
        	Button b = (Button)rootView.findViewById(R.id.btnUpdate);
        	b.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					UpdateChecker.CheckForUpdate(getActivity());
				}
        	});
        	
        	return rootView;
        }
    }
    public static class MainMenuPageFragment extends Fragment {
    	
        private void OpenURL(String url)
        {
        	Intent i = new Intent(Intent.ACTION_VIEW);
        	i.setData(Uri.parse(url));
        	startActivity(i);
        }
        
    	private void setButton() {
    		if (btnActivate != null)
    		{	
    			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getActivity());
    			btnActivate.setText(
    					(prefs.getBoolean("onetwodimenabling", false)) ?
    					R.string.btnOff_OneTwoDim :
    					R.string.btnOff);
    			btnActivate.setEnabled(!StandbyService.isRunning);
    		}
    	}

        @Override
        public void onPause() {
        	super.onPause();
        	try
        	{
        		MainMenuPageFragment.this.getActivity().unregisterReceiver(receiver);
        	}
        	catch(Exception ex) {}
            super.onPause();
        }

        
    	@Override
    	public void onResume()
    	{
    		super.onResume();
    		setButton();
    		try
    		{
    			IntentFilter filter = new IntentFilter();
            	filter.addAction(StandbyService.TOGGLE_INTENT);
            	this.getActivity().registerReceiver(receiver, filter);
            	//warning();
    		}
    		catch(Exception ex){}
    	}
    	
    	private BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
            	if (btnActivate != null)
            		btnActivate.setEnabled(true);
            }
        };

    	private void goHomeScreen()
    	{
    		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getActivity());
    		if (prefs.getBoolean("returnhome", false))
    		{
    			Intent startMain = new Intent(Intent.ACTION_MAIN);
    			startMain.addCategory(Intent.CATEGORY_HOME);
    			startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    			startActivity(startMain);
    		}
    	}
    	private Button btnActivate;
    	
        @SuppressLint("NewApi")
		@Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            ViewGroup rootView = (ViewGroup) inflater.inflate(
            		useMetro?R.layout.main_menu_layout:R.layout.main_menu_layout_alt, container, false);
            
        	//animFlippingWholePage.setTarget(rootView);
            btnActivate = (Button) rootView.findViewById(R.id.button1);
            btnActivate.setTypeface(typeface);
            btnActivate.setOnTouchListener(MetroButtonFlipListener);
            btnActivate.setOnClickListener(new OnClickListener() {
    			@Override
    			public void onClick(View v) {
    				Intent serviceIntent = new Intent();
    	            serviceIntent.setAction(StandbyService.ENABLE_INTENT);
    	            MainMenuPageFragment.this.getActivity().sendBroadcast(serviceIntent);
    	            goHomeScreen();
    	            btnActivate.setEnabled(false);
    			}}
            );
            
            setButton();
            Button bManual = (Button) rootView.findViewById(R.id.btnmanualbrightness);

            bManual.setTypeface(typeface);
            bManual.setOnTouchListener(MetroButtonFlipListener);
            bManual.setOnClickListener(new OnClickListener() {

    			@Override
    			public void onClick(View v) {
    				animFlippingWholePage.start();
					animFlippingWholePage.removeAllListeners();
					animFlippingWholePage.addListener(new AnimatorListener(){

						@Override
						public void onAnimationCancel(Animator arg0) {
						}

						@Override
						public void onAnimationEnd(Animator arg0) {
		    				Intent intent = new Intent(MainMenuPageFragment.this.getActivity(),
		    	      			      ManualBrightnessChangerActivity.class);
	    	            		startActivity(intent);
	    	            	animFlippingWholePage.reverse();
						}

						@Override
						public void onAnimationRepeat(Animator arg0) {
						}

						@Override
						public void onAnimationStart(Animator arg0) {
						}
					});
    			}}
            );
            Button btnRemoteControl = (Button) rootView.findViewById(R.id.btnRemote);
            btnRemoteControl.setTypeface(typeface);
            btnRemoteControl.setOnTouchListener(MetroButtonFlipListener);
            btnRemoteControl.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					ConnectivityManager connManager = (ConnectivityManager) getActivity().getSystemService(CONNECTIVITY_SERVICE);
					NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

					boolean mWifiConnected = mWifi.isConnected();
					if (!mWifiConnected)
					{
						WifiManager wifi = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
						java.lang.reflect.Method[] wmMethods = wifi.getClass().getDeclaredMethods();
						
						for(java.lang.reflect.Method method: wmMethods){
							if(method.getName().equals("isWifiApEnabled")) {
								try {
									mWifiConnected = (Boolean)method.invoke(wifi);
								}
								catch(Exception ex)
								{
									Logger.Log(getActivity(), ex);
									mWifiConnected = false;
								}
								break;
							}
						}
					}
					
					if (!mWifiConnected) {
						Builder builder = new AlertDialog.Builder(getActivity());
				    	builder
				    	.setTitle("Wifi needed!")
				    	.setMessage("Remote control currently only works with Wifi connection / Wifi tethering.\n\nDo you want to open wifi settings and connect to a wifi network?\n\n (Note: Receiver & controller must be on a same network / tethering hotspot)")
				    	.setIcon(android.R.drawable.ic_dialog_alert)
				    	.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				    	    public void onClick(DialogInterface dialog, int which) {
				    	    	getActivity().startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
				    	    }
				    	})
				    	.setNeutralButton("Proceed anyway", new DialogInterface.OnClickListener() {
				    	    public void onClick(DialogInterface dialog, int which) {
				    	    	Intent intent = new Intent(MainMenuPageFragment.this.getActivity(),
			    						ServerService.isRunning?RemoteServerActivity.class:
			    						(ClientService.isRunning?RemoteControllerActivity.class:
			    												RemoteControlSelectActivity.class));
		    	            		startActivity(intent);
		    	            	animFlippingWholePage.reverse();
				    	    }
				    	})
				    	.setNegativeButton("No", null)
				    	.show();	
						return;
					}
					
					animFlippingWholePage.start();
					animFlippingWholePage.removeAllListeners();
					animFlippingWholePage.addListener(new AnimatorListener(){

						@Override
						public void onAnimationCancel(Animator arg0) {
						}

						@Override
						public void onAnimationEnd(Animator arg0) {
		    				Intent intent = new Intent(MainMenuPageFragment.this.getActivity(),
		    						ServerService.isRunning?RemoteServerActivity.class:
		    						(ClientService.isRunning?RemoteControllerActivity.class:
		    												RemoteControlSelectActivity.class));
	    	            		startActivity(intent);
	    	            	animFlippingWholePage.reverse();
						}

						@Override
						public void onAnimationRepeat(Animator arg0) {
						}

						@Override
						public void onAnimationStart(Animator arg0) {
						}
					});
				}
            });
            
            Button btnOn = (Button) rootView.findViewById(R.id.btnOn);
            btnOn.setTypeface(typeface);
            btnOn.setOnTouchListener(MetroButtonFlipListener);
            btnOn.setOnClickListener(new OnClickListener() {

    			@Override
    			public void onClick(View v) {
    				Intent serviceIntent = new Intent();
    	            serviceIntent.setAction(StandbyService.TOGGLE_INTENT);
    	            MainMenuPageFragment.this.getActivity().sendBroadcast(serviceIntent);
    			}}
            );
            Button btnXda = (Button) rootView.findViewById(R.id.btnxda);
            btnXda.setTypeface(typeface);
            btnXda.setOnTouchListener(MetroButtonFlipListener);
            btnXda.setOnClickListener(new OnClickListener() {

    			@Override
    			public void onClick(View v) {
    				OpenURL("http://forum.xda-developers.com/showthread.php?p=32732821");
    		                                                                                                                                                                                                                       	}}
            );
            Button btnWebsite = (Button) rootView.findViewById(R.id.btnwebsite);
            btnWebsite.setTypeface(typeface);
            btnWebsite.setOnTouchListener(MetroButtonFlipListener);
            btnWebsite.setOnClickListener(new OnClickListener() {
    			@Override
    			public void onClick(View v) {
    				OpenURL("http://www.screenstandby.com");
    		                                                                                                                                                                                                                       	}}
            );

            TextView tv = (TextView) rootView.findViewById(R.id.txtVersion);
    		try {
    			pInfo = MainMenuPageFragment.this.getActivity().getPackageManager().getPackageInfo( MainMenuPageFragment.this.getActivity().getPackageName(), 0);
    	        tv.setText("version: " + pInfo.versionName);
    	        tv.setTypeface(typeface);
    		} catch (NameNotFoundException e) {
    		}
            return rootView;
        }
    }

    private class MainPagerAdapter extends FragmentStatePagerAdapter {
    	private AppInfoPageFragment currentAppInfo;
    	private MainMenuPageFragment currentMainMenu;
        public MainPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
        	switch (position)
        	{
        		case 0:
        			currentMainMenu = new MainMenuPageFragment();
        			return currentMainMenu;
        		case 1:
        			PreferenceListFragment prefListFragment = new PreferenceListFragment(R.xml.programpreferences);
        			return prefListFragment;
        		case 2:
        			currentAppInfo = new AppInfoPageFragment() ;
        			return currentAppInfo;
        	}
            return null;
        }
        public MainMenuPageFragment getCurrentMainFragment()
        {
        	return currentMainMenu;	
        }
        
        public AppInfoPageFragment getCurrentAppInfoFragment()
        {
        	return currentAppInfo;	
        }
        
        @Override
        public CharSequence getPageTitle (int position)
        {
        	switch (position)
        	{
        		case 0:
        			return (useMetro)?"screen standby":"Screen Standby";
        		case 1:
        			return (useMetro)?"settings":"Settings";
        		case 2:
        			return (useMetro)?"app info":"App info";
        	}
        	return "";
        }
        @Override
        public int getCount() {
            return NUM_PAGES;
        }
    }

	@Override
	public boolean onPreferenceClick(Preference arg0) {
		return false;
	}
	@Override
	public boolean onPreferenceChange(Preference arg0, Object arg1) {
		return false;
	}
	private SettingActivity currentSettingActivity;
	@Override
	public void onPreferenceAttached(PreferenceScreen root, int xmlId) {
		if(root != null)
			currentSettingActivity = new SettingActivity(this, root);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if (keyCode == KeyEvent.KEYCODE_BACK) {
	    	if (mPager.getCurrentItem() != 0)
	    	{
	    		mPager.setCurrentItem(0, true);
	        	return true;
	    	}
	    }
       return super.onKeyDown(keyCode, event);
	}
}

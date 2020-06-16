package com.livewallpaper.audio;

import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.Manifest;


public class PrefsActivity extends PreferenceActivity
	implements SharedPreferences.OnSharedPreferenceChangeListener {
	
	Preference button_reflection = null;
	Preference button_vertical = null;
	Preference button_circle = null;
	Preference button_nothing = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestRecordAudioPermission();

		getPreferenceManager().setSharedPreferencesName(AudioLiveWallpaperActivity.SHARED_PREFS_NAME);
        addPreferencesFromResource(R.xml.audio_settings);
        //getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        
        button_reflection = (Preference) findPreference("reflection");
        button_reflection.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			
			public boolean onPreferenceClick(Preference preference) {
				SharedPreferences perf = getSharedPreferences("MySettings", Context.MODE_PRIVATE);
				String value = perf.getString("color_selection", "none");
				AudioLiveWallpaperActivity.CurrentColor = value;
				AudioLiveWallpaperActivity.CurrentTheme = "reflection";
				Intent i = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
				i.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,new ComponentName(getApplicationContext(), AudioLiveWallpaperActivity.class));
				startActivity(i);
				return true;
			}
		});
        
        button_vertical = (Preference) findPreference("vertical");
        button_vertical.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			
			public boolean onPreferenceClick(Preference preference) {
				SharedPreferences perf = getSharedPreferences("MySettings", Context.MODE_PRIVATE);
				String value = perf.getString("color_selection", "none");
				AudioLiveWallpaperActivity.CurrentColor = value;
				AudioLiveWallpaperActivity.CurrentTheme = "vertical";
                Intent i = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
                i.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,new ComponentName(getApplicationContext(), AudioLiveWallpaperActivity.class));
                startActivity(i);
				//Intent i = new Intent( getApplicationContext(), AudioLiveWallpaperActivity.class);
                //startService(i);
				return true;
			}
		});
        
        button_circle = (Preference) findPreference("circle");
        button_circle.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			
			public boolean onPreferenceClick(Preference preference) {
				
				AudioLiveWallpaperActivity.CurrentTheme = "circle";
                Intent i = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
                i.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,new ComponentName(getApplicationContext(), AudioLiveWallpaperActivity.class));
                startActivity(i);
                //Intent i = new Intent( getApplicationContext(), AudioLiveWallpaperActivity.class);
                //startService(i);
				return true;
			}
		});
        
        button_nothing = (Preference) findPreference("nothing");
        button_nothing.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			
			public boolean onPreferenceClick(Preference preference) {
				
				AudioLiveWallpaperActivity.CurrentTheme = "nothing";
                Intent i = new Intent( getApplicationContext(), AudioLiveWallpaperActivity.class);
                startService(i);
				return true;
			}
		});
	}
	
	@Override
	public void onDestroy() {
		
		//getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
		super.onDestroy();
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		
		String value = sharedPreferences.getString("color_selection", "none");
		AudioLiveWallpaperActivity.CurrentColor = value;
		
		Log.d("PrefsActivity", value);
	}

    private void requestRecordAudioPermission() {
        //check API version, do nothing if API version < 23!
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion > android.os.Build.VERSION_CODES.LOLLIPOP){

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {

                    // Show an expanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.

                } else {

                    // No explanation needed, we can request the permission.

                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    Log.d("Activity", "Granted!");

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Log.d("Activity", "Denied!");
                    finish();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
}

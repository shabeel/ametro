/*
 * http://code.google.com/p/ametro/
 * Transport map viewer for Android platform
 * Copyright (C) 2009-2010 Roman.Golovanov@gmail.com and other
 * respective project committers (see project home page)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.ametro.activity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Locale;

import org.ametro.Constants;
import org.ametro.MapSettings;
import org.ametro.MapUri;
import org.ametro.R;
import org.ametro.adapter.MapListAdapter;
import org.ametro.model.City;
import org.ametro.model.Deserializer;
import org.ametro.other.FileGroupsDictionary;
import org.ametro.other.ProgressInfo;
import org.ametro.util.csv.CsvReader;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.AsyncTask.Status;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


public class BrowseLibrary extends Activity implements ExpandableListView.OnChildClickListener, LocationListener {

	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, MAIN_MENU_REFRESH, 0, R.string.menu_refresh).setIcon(android.R.drawable.ic_menu_rotate);
		menu.add(0, MAIN_MENU_LOCATION, 3, R.string.menu_location).setIcon(android.R.drawable.ic_menu_mylocation);
		menu.add(0, MAIN_MENU_IMPORT, 4, R.string.menu_import).setIcon(android.R.drawable.ic_menu_add);
		menu.add(0, MAIN_MENU_SETTINGS, 5, R.string.menu_settings).setIcon(android.R.drawable.ic_menu_preferences);
		menu.add(0, MAIN_MENU_ABOUT, 6, R.string.menu_about).setIcon(android.R.drawable.ic_menu_help);

		mMainMenuAllMaps = menu.add(0, MAIN_MENU_ALL_MAPS, 1, R.string.menu_all_maps).setIcon(android.R.drawable.ic_menu_mapmode).setVisible(false);
		mMainMenuMyMaps = menu.add(0, MAIN_MENU_MY_MAPS, 2, R.string.menu_my_maps).setIcon(android.R.drawable.ic_menu_myplaces);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MAIN_MENU_REFRESH:
			MapSettings.refreshMapList();
			beginIndexing();
			return true;
		case MAIN_MENU_ALL_MAPS:
			mMainMenuAllMaps.setVisible(false);
			mMainMenuMyMaps.setVisible(true);
			return true;
		case MAIN_MENU_MY_MAPS:
			mMainMenuAllMaps.setVisible(true);
			mMainMenuMyMaps.setVisible(false);
			return true;
		case MAIN_MENU_LOCATION:
			requestMyLocation();
			return true;
		case MAIN_MENU_IMPORT:
			startActivityForResult(new Intent(this, ImportPmz.class), REQUEST_IMPORT);
			return true;
		case MAIN_MENU_SETTINGS:
			startActivityForResult(new Intent(this, Settings.class), REQUEST_SETTINGS);
			return true;
		case MAIN_MENU_ABOUT:
			startActivity(new Intent(this, About.class));
			return true;

		}
		return super.onOptionsItemSelected(item);
	} 

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_IMPORT:
			beginIndexing();
			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		MapSettings.checkPrerequisite();
		beginIndexing();
		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
	}

	protected void onResume() {
		mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
		super.onResume();
	}

	protected void onPause() {
		mLocationManager.removeUpdates(this);
		super.onPause();
	}

	protected void onStop() {
		if (mIndexTask != null && mIndexTask.getStatus() != Status.FINISHED) {
			mIndexTask.cancel(false);
		}
		super.onStop();
	} 

	private void beginIndexing() {
		mIndexTask = new IndexTask();
		mIndexTask.execute();
	}

	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		String fileName = mAdapter.getFileName(groupPosition, childPosition);
		String mapName = fileName.replace(MapSettings.MAP_FILE_TYPE, "");
		Intent i = new Intent();
		i.setData(MapUri.create(mapName));
		setResult(RESULT_OK, i);
		finish();
		return true;
	}


	private void requestMyLocation(){
		mLocateTask = new LocateTask();
		mLocateTask.execute();
	}

	private LocationManager mLocationManager;
	private Location mLocation;

	private MapListAdapter mAdapter;
	private ExpandableListView mListView;
	private String mDefaultPackageFileName;

	private ProgressBar mProgressBar;
	private TextView mProgressTitle;
	private TextView mProgressText;
	private TextView mProgressCounter;

	private final int MAIN_MENU_REFRESH = 1;
	private final int MAIN_MENU_ALL_MAPS = 2;
	private final int MAIN_MENU_MY_MAPS = 3;
	private final int MAIN_MENU_LOCATION = 4;
	private final int MAIN_MENU_IMPORT = 5;
	private final int MAIN_MENU_SETTINGS = 6;
	private final int MAIN_MENU_ABOUT = 7;

	private MenuItem mMainMenuAllMaps;
	private MenuItem mMainMenuMyMaps;

	private IndexTask mIndexTask;
	private LocateTask mLocateTask;

	private final static int REQUEST_IMPORT = 1;
	private final static int REQUEST_SETTINGS = 2;


	private static class LocationInfo
	{
		String FileName;
		String CityName;
		String CountryName;
		double Latitude;
		double Longitude;
		double EW;
		double NS;
	}

	private class LocateTask extends AsyncTask<Void, ProgressInfo, LocationInfo>
	{
		private ProgressDialog dialog;

		protected LocationInfo doInBackground(Void... args) {
			// step 0: prepare!
			double latitude = 0, longitude = 0;

			if(mLocation!=null){
				latitude = mLocation.getLatitude();
				longitude = mLocation.getLongitude();
			}else{
				Location myLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
				if(myLocation == null ){
					myLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
				}
				if(myLocation != null){
					latitude = myLocation.getLatitude();
					longitude = myLocation.getLongitude();
				}else{
					return null;
				}
			}

			ArrayList<LocationInfo> locations = new ArrayList<LocationInfo>();
			Locale locale = Locale.getDefault();

			final int FileNameColumn = 0;
			final int CityColumn = (locale.equals(Locale.ENGLISH)) ? 2 : 1;
			final int CountryColumn = (locale.equals(Locale.ENGLISH)) ? 4 : 3;
			final int LatitudeColumn = 5;
			final int LongitudeColumn = 6;
			final int EWColumn = 7;
			final int NSColumn = 8;

			// step 1: load city data
			try{
				CsvReader reader = new CsvReader(new BufferedReader( new InputStreamReader( getAssets().open("cities.csv"), org.ametro.model.Serializer.ENCODING  )) );
				int idx = 0;
				while(reader.next()){
					idx++;
					if(idx == 1) { 
						continue; // skip header row
					}
					if(reader.getCount() < 7) { 
						continue; // skip empty or invalid rows
					}
					LocationInfo info = new LocationInfo();
					info.FileName = reader.getString(FileNameColumn);
					info.CityName = reader.getString(CityColumn);
					info.CountryName = reader.getString(CountryColumn);
					info.Latitude = reader.getDouble(LatitudeColumn);
					info.Longitude = reader.getDouble(LongitudeColumn);
					info.EW = reader.getNullableDouble(EWColumn, 50);
					info.NS = reader.getNullableDouble(NSColumn, 50);
					locations.add(info);
				}

			}catch(IOException ex){
				if(Log.isLoggable(Constants.LOG_TAG_MAIN, Log.DEBUG)){
					Log.d(Constants.LOG_TAG_MAIN,"Cannot read cities.csv cities.csv" , ex);
				}				
				return null;
			}

			// step 2: search coordinates
			Location infoLocation = new Location("gps");
			float[] distances = new float[3];
			for(LocationInfo info : locations){
				if(isCancelled()) {
					return null;
				}
				infoLocation.setLatitude(info.Latitude);
				infoLocation.setLongitude(info.Longitude);
				Location.distanceBetween(info.Latitude, info.Longitude, latitude, longitude, distances);
				if( distances[0] < info.EW || distances[0] < info.NS ){
					return info;
				}

			}
			LocationInfo emptyLocation = new LocationInfo();
			emptyLocation.Latitude = latitude;
			emptyLocation.Longitude = longitude;
			return emptyLocation;
		}

		protected void onPreExecute() {
			super.onPreExecute();
			dialog = ProgressDialog.show(BrowseLibrary.this, "Locate position", "Please wait...", true);
		}

		protected void onCancelled() {
			super.onCancelled();
			dialog.hide();
		}

		protected void onPostExecute(LocationInfo result) {
			dialog.hide();
			if(result!=null){
				if(result.FileName!=null){
					Toast.makeText(BrowseLibrary.this, "You are in " + result.CityName + " within " + result.CountryName, Toast.LENGTH_SHORT).show();
				}else{
					Toast.makeText(BrowseLibrary.this, "You are at " + result.Latitude + "," + result.Longitude, Toast.LENGTH_SHORT).show();
				}
			}else{
				Toast.makeText(BrowseLibrary.this, "Unknown location", Toast.LENGTH_SHORT).show();
			}
			super.onPostExecute(result);
		}
	}

	private class IndexTask extends AsyncTask<Void, ProgressInfo, FileGroupsDictionary> {
		private boolean mIsCanceled = false;
		private boolean mIsProgressVisible = false;

		private void scanModelFileContent(FileGroupsDictionary map, String fileName, String fullFileName) {
			try {
				City city = Deserializer.deserialize(new FileInputStream(MapSettings.MAPS_PATH + fileName), true);
				if (city.sourceVersion == MapSettings.getSourceVersion()) {
					map.putFile(city.countryName, city.cityName, fileName);
				}
			} catch (Exception e) {
				if(Log.isLoggable(Constants.LOG_TAG_MAIN, Log.DEBUG)){
					Log.d(Constants.LOG_TAG_MAIN, getString(R.string.log_map_indexing_failed) + fileName, e);
				}
			}
		}

		private FileGroupsDictionary scanMapDirectory(File dir) {
			FileGroupsDictionary map;
			ProgressInfo pi = new ProgressInfo(0, 0, null, getString(R.string.msg_loading_maps));
			publishProgress(pi);
			map = new FileGroupsDictionary();
			map.timestamp = dir.lastModified();
			String[] files = dir.list(new FilenameFilter() {
				public boolean accept(File f, String filename) {
					return filename.endsWith(MapSettings.MAP_FILE_TYPE);
				}
			});

			if (files != null) {
				final int count = files.length;
				pi.title = getString(R.string.msg_loading_maps);
				pi.maximum = count;

				for (int i = 0; i < count && !mIsCanceled; i++) {
					String fileName = files[i];
					pi.progress = i;
					pi.message = fileName;
					publishProgress(pi);
					String fullFileName = dir.getAbsolutePath() + '/' + files[i];
					scanModelFileContent(map, fileName, fullFileName);
				}
			}
			return map;
		}

		protected FileGroupsDictionary doInBackground(Void... params) {
			final String cacheFileName = MapSettings.ROOT_PATH + MapSettings.MAPS_LIST;
			final File dir = new File(MapSettings.MAPS_PATH);
			FileGroupsDictionary map = FileGroupsDictionary.read(cacheFileName);
			if (map == null || map.timestamp < dir.lastModified()) {
				map = scanMapDirectory(dir);
				FileGroupsDictionary.write(map, cacheFileName);
			}
			return map;

		}

		protected void onProgressUpdate(ProgressInfo... values) {
			if (!mIsProgressVisible) {
				mIsProgressVisible = true;
				setContentView(R.layout.import_pmz_progress);
				mProgressBar = (ProgressBar) findViewById(R.id.import_pmz_progress_bar);
				mProgressTitle = (TextView) findViewById(R.id.import_pmz_progress_title);
				mProgressText = (TextView) findViewById(R.id.import_pmz_progress_text);
				mProgressCounter = (TextView) findViewById(R.id.import_pmz_progress_counter);
			}
			ProgressInfo.ChangeProgress(
					values[0],
					mProgressBar,
					mProgressTitle,
					mProgressText,
					mProgressCounter,
					getString(R.string.template_progress_count)
			);
			super.onProgressUpdate(values);
		}

		protected void onCancelled() {
			mIsCanceled = true;
			super.onCancelled();
		}

		protected void onPreExecute() {
			mDefaultPackageFileName = null;
			Intent intent = getIntent();
			Uri uri = intent != null ? intent.getData() : null;
			if (uri != null) {
				mDefaultPackageFileName = MapUri.getMapName(uri) + MapSettings.MAP_FILE_TYPE;
			}
			setContentView(R.layout.global_wait);
			super.onPreExecute();
		}

		protected void onPostExecute(FileGroupsDictionary result) {
			if (result.getGroupCount() > 0) {
				setContentView(R.layout.browse_library_main);
				mAdapter = new MapListAdapter(BrowseLibrary.this, result);
				mListView = (ExpandableListView) findViewById(R.id.library_map_list);
				mListView.setOnChildClickListener(BrowseLibrary.this);
				mListView.setAdapter(mAdapter);
				if (mDefaultPackageFileName != null) {
					mAdapter.setSelectedFile(mDefaultPackageFileName);
					int groupPosition = mAdapter.getSelectedGroupPosition();
					int childPosition = mAdapter.getSelectChildPosition();
					if (groupPosition != -1) {
						mListView.expandGroup(groupPosition);
						if (childPosition != -1) {
							mListView.setSelectedChild(groupPosition, childPosition, true);
						}
					}
				}
			} else {
				setContentView(R.layout.browse_library_empty);
			}
			super.onPostExecute(result);
		}
	}

	public void onLocationChanged(Location location) {
		if(location!=null){
			mLocation = new Location(LocationManager.GPS_PROVIDER);
			mLocation.setLatitude(location.getLatitude());
			mLocation.setLongitude(location.getLongitude());
		}
	}

	public void onProviderDisabled(String provider) {
	}

	public void onProviderEnabled(String provider) {
	}

	public void onStatusChanged(String provider, int status, Bundle bundle) {
	}


}
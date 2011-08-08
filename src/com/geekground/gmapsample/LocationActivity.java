package com.geekground.gmapsample;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.GestureDetector.OnGestureListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class LocationActivity extends MapActivity implements OnGestureListener{

	private MapController mMapController;
	private MyLocationOverlay mMyLocation;
	private MapView mMapView;
	private Context mContext;
	private ImageButton mFavoriteButton;
	private PickUpLocationItemizedOverlay mItemizedOverlay = null;
	private ArrayList<FavoriteLocation> favoriteLocations = null;
	private ArrayAdapter<FavoriteLocation> favoriteLocationAdapter;
	private View mListFooterRow;
	private TextView address_bar_text;

	private SharedPreferences prefs;
	private static final String APPLICATION_PREFERENCES = "favorite_locations";
	
	private Address mCurrentMarkerAddress = null;

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;

		//get shared preference
		prefs = this.getSharedPreferences(APPLICATION_PREFERENCES, Context.MODE_PRIVATE);

		//set custom title and layout
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.location_activity);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_bar_location);

		//init views
		mFavoriteButton = (ImageButton)findViewById(R.id.favorite_button);
		address_bar_text = (TextView)findViewById(R.id.address_bar_text);

		//init map stuff
		mMapView = (MapView) findViewById(R.id.mapview);
		mMapView.setBuiltInZoomControls(true);
		mMapController = mMapView.getController();
		mMyLocation = new MyLocationOverlay(this, mMapView);
		
        // Set the drawable that will overlay map
        Drawable locationDrawable = getResources().getDrawable(R.drawable.map_marker);
        
        //Initialize itemized overlay
        mItemizedOverlay = new PickUpLocationItemizedOverlay(locationDrawable, mContext, mMapView);

		//get current location and adds my location overlay to map
		getCurrentLocation();
		
		// pull favorite locations from preferences
		loadLocations();

		//init buttons
		mFavoriteButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showFavoritesDialog();
			}
		});
		
	}

	private void getCurrentLocation() {
		mMyLocation.enableMyLocation();
		//run until there is a gps fix
		mMyLocation.runOnFirstFix(new Runnable() {
			public void run() {
				mMapController.setCenter(mMyLocation.getMyLocation());

				// add my location overlay to map
				mMapView.getOverlays().add(mMyLocation);
				mMapController.setZoom(13);
				mMapController.animateTo(mMyLocation.getMyLocation());

			}
		});
	}
	
	private void loadLocations() {
		favoriteLocations = new ArrayList<FavoriteLocation>();

		try {
			//check to see if there is any stored favorite locations
			JSONArray fLocJSONArray = getFavorites();

			//if there is any favorites
			if(fLocJSONArray != null){
				
				//loop thru each favorite location and add to list of favorite locations
				for (int i = 0; i < fLocJSONArray.length(); i++) {
					String name = fLocJSONArray.getJSONObject(i).getString("name");

					int lat = fLocJSONArray.getJSONObject(i).getInt("late6");
					int lng = fLocJSONArray.getJSONObject(i).getInt("lnge6");

					GeoPoint geoP = new GeoPoint(lat, lng);

					FavoriteLocation location = new FavoriteLocation(name, geoP);

					favoriteLocations.add(location);
				}
			}

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void showFavoritesDialog(){
		
		//build dialog and inflate layout to show list of favorite locations
		AlertDialog.Builder favoriteLocationDialogBuilder = new AlertDialog.Builder(mContext);

		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(mContext.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.favorite_location_dialog, null);
		
		//set dialog properties
		favoriteLocationDialogBuilder.setView(layout)
		.setTitle("Favorite locations")
		.setCancelable(true)
		.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		
		});

		final AlertDialog favoriteSelectionDialog = favoriteLocationDialogBuilder.create();
		favoriteSelectionDialog.show();
		
		//create a list to show the favorites	
		ListView list = (ListView)layout.findViewById(android.R.id.list);
		
        //if there are favorite locations create an array adapter
		if(!favoriteLocations.isEmpty()) {
            favoriteLocationAdapter = new FavoritePlacesAdapter(favoriteLocations);
        }
		
        //inflate footer row that allows a user to add a new favorite location
        mListFooterRow = getLayoutInflater().inflate(R.layout.favorite_location_footer_row, null); 

        //if they select footer row, allow them to add a new location
        mListFooterRow.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {  

            	favoriteSelectionDialog.dismiss();
            	addFavoriteLocation();
            }
        });
        
        //append footer row to list
        list.addFooterView(mListFooterRow);
        
        //if they select any other row, populate map with favorite place
        list.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> list, View view, int position, long id) {
            	FavoriteLocation favLocation = (FavoriteLocation) list.getItemAtPosition(position);
            	

            	favoriteSelectionDialog.dismiss();
            	addFavoriteToMap(favLocation);
            }
        });
        
        //set list adapter for favorite locations
        list.setAdapter(favoriteLocationAdapter);

	}
    
	private class ViewWrapper {
		//Wraps a view for easier inflation and manipulation
        View base;

        TextView locationName = null;

        ViewWrapper(View base) {
            this.base = base;
        }

        TextView getName() {
            if (locationName == null) {
            	locationName = (TextView) base.findViewById(R.id.TextViewLocationName);
            }
            return (locationName);
        }

    }
	
    private class FavoritePlacesAdapter extends ArrayAdapter<FavoriteLocation> {
    	FavoritePlacesAdapter(ArrayList<FavoriteLocation> location) {
            super(mContext, R.layout.favorite_location_row, location);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // set plans model to the current item given the position
            FavoriteLocation favLoc = getItem(position);

            // init view wrapper
            ViewWrapper wrapper = null;

            // set row to current view
            View row = convertView;

            if (row == null) {
                LayoutInflater inflater = getLayoutInflater();
                row = inflater.inflate(R.layout.favorite_location_row, parent, false);
                wrapper = new ViewWrapper(row);
                row.setTag(wrapper);

            } else {
                wrapper = (ViewWrapper) row.getTag();
            }

            // set values
            wrapper.getName().setText(favLoc.getName());

            return (row);
        }
    }

	private void addFavoriteLocation(){
		
		//if user has picked a location on the map, allow them to save it as a favorite
		if(mItemizedOverlay.size() > 0){
			
		//get currently selected location
		OverlayItem oi = mItemizedOverlay.getOverlay();
		final GeoPoint favPoint = oi.getPoint();
		
		//create a new dialog so they can enter the name of their favorite place
        AlertDialog.Builder saveFavoriteDialog = new AlertDialog.Builder(mContext);

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(mContext.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.save_location_dialog, null);
		
		final TextView name_text = (TextView) layout.findViewById(R.id.location_name_edit_text);

        saveFavoriteDialog.setView(layout)
        .setTitle("Save Favorite")
        .setCancelable(true)
        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        })
        .setPositiveButton("Save", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

        		FavoriteLocation favLoc = new FavoriteLocation(name_text.getText().toString(), favPoint);

        		favoriteLocations.add(favLoc);
        		saveLocations();
        		dialog.cancel();
            }
        }); 

        AlertDialog confirmAlert = saveFavoriteDialog.create();
        confirmAlert.show();
		} else {
			Toast.makeText(mContext, "No location selected", Toast.LENGTH_SHORT).show();
		}
	}

	private void saveLocations(){
		JSONArray fLocJSONArray = new JSONArray();
		
		/*loops through list of favorites in memory, 
		 * converts each to json, and adds them to jsonArray to be stored
		 */
		for (FavoriteLocation favLoc : favoriteLocations) {
			fLocJSONArray.put(favLoc.toJSON());
		}

		setFavorites(fLocJSONArray);
	}
	

	public void setFavorites(JSONArray favLocArray) {
		//update shared preferences with new locations
		Editor editor = prefs.edit();

		String favLocString = favLocArray.toString();
		editor.putString("FavLoc", favLocString);
		editor.commit();
	}

	public JSONArray getFavorites() {
		//check to see if there is any stored favorite locations and convert to JSONArray
		String favString = prefs.getString("FavLoc", null);
		if (favString != null) {
			try {
				//convert string to JSONArray
				JSONArray favLocArray = new JSONArray(favString);
				return favLocArray;
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	@Override
	protected void onResume() {
		super.onResume();
		mMyLocation.enableMyLocation();
	}

	@Override
	public void onPause() {
		super.onPause();
		mMyLocation.disableMyLocation();
	}

	private class FavoriteLocation {
		private String name;
		private GeoPoint geoPoint;

		public FavoriteLocation(String name, GeoPoint geoPoint) {
			//super 
			super();
			this.name = name;
			this.geoPoint = geoPoint;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public GeoPoint getGeoPoint() {
			return geoPoint;
		}

		public void setGeoPoint(GeoPoint geoPoint) {
			this.geoPoint = geoPoint;
		}

		public JSONObject toJSON(){
			JSONObject jsonObject = new JSONObject();

			try {
				jsonObject.put("name", this.name);
				jsonObject.put("late6", this.geoPoint.getLatitudeE6());
				jsonObject.put("lnge6", this.geoPoint.getLongitudeE6());

				return jsonObject;

			} catch (JSONException e) {
				e.printStackTrace();
			}

			return null;
		}
	}

	@Override
	public boolean onDown(MotionEvent e) {
		return false;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {
		
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {

	    int X = (int)e.getX();          
	    int Y = (int)e.getY();

	    //create geopoint from where user taps map
	    GeoPoint geoPoint = mMapView.getProjection().fromPixels(X, Y);
	    
	    clearAddAndGeoCode(geoPoint);
	    
		return false;
	}
	
	private void addFavoriteToMap(FavoriteLocation favLoc){
		
		GeoPoint geoPoint = favLoc.geoPoint;
		
		clearAddAndGeoCode(geoPoint);	
	}
	
	private void clearAddAndGeoCode(GeoPoint geoPoint){
		
		geoCoder(geoPoint);
        
        OverlayItem oItem = new OverlayItem(geoPoint, "Selected Location", "");
        
        //clear previous markers from itemized overlay
        mItemizedOverlay.clear();
        
        //add new overlay to itemized overlay
        mItemizedOverlay.addOverlay(oItem);
        
        //if the map does not contain itemized overlays
        if(!mMapView.getOverlays().contains(mItemizedOverlay)){
            
            //add itemized overlay to map
            mMapView.getOverlays().add(mItemizedOverlay);
        }

        // redraw map
        mMapView.invalidate();

        mMapController.animateTo(geoPoint);
	}
	
	
	private void geoCoder(GeoPoint geoPoint){
		
		Double currentLat = geoPoint.getLatitudeE6() / 1E6;
        Double currentLng = geoPoint.getLongitudeE6() / 1E6;
        
		// init new geocoder
        Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
        try {
            // get the markers current lat/lng, geocode it into an address
            List<Address> proxAddress = geocoder.getFromLocation(currentLat, currentLng, 1);
            
            // if the list of addresses has something
            if(proxAddress.size() > 0) {
                
                // set the first returned geocoded address
                mCurrentMarkerAddress = proxAddress.get(0);
                
                if(mCurrentMarkerAddress != null) {
                    // get part one of the address
                    String addressLineOne = mCurrentMarkerAddress.getAddressLine(0) == null ? "" : mCurrentMarkerAddress.getAddressLine(0);
                    
                    // get part two of the address
                    String addressLineTwo = mCurrentMarkerAddress.getAddressLine(1) == null ? "" : mCurrentMarkerAddress.getAddressLine(1);
                    
                    // set address text
                    String addressText =  addressLineOne + " " + addressLineTwo;

            		address_bar_text.setText(addressText);
                } else {
                    mCurrentMarkerAddress = null;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
	}
}
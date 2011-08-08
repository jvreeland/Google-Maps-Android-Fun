package com.geekground.gmapsample;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

public class PickUpLocationItemizedOverlay extends ItemizedOverlay {
	
	private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
	private Context mContext;
	private MapView mMapView;
	
    public PickUpLocationItemizedOverlay(Drawable defaultMarker, Context context, MapView mapView) {
        super(boundCenterBottom(defaultMarker));
        mContext = context;
        mMapView = mapView;
      }


	@Override
	protected OverlayItem createItem(int i) {
	  return mOverlays.get(i);
	}
	
	@Override
	public int size() {
	  return mOverlays.size();
	}
    public void clear() {
        mOverlays.clear();
        mMapView.removeAllViews();
        mMapView.postInvalidate();
        setLastFocusedIndex(-1);
        populate();
    } 
	
	
	public void addOverlay(OverlayItem overlay) {
		mOverlays.clear();
	    mOverlays.add(overlay);
        setLastFocusedIndex(-1);
        mMapView.postInvalidate();
	    populate();
	}
	
	public OverlayItem getOverlay(){
		OverlayItem item = mOverlays.get(0);
		return item;
	}
	
    @Override
    protected boolean onTap(int index) {
//      OverlayItem item = mOverlays.get(index);
//      AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
//      dialog.setTitle(item.getTitle());
//      dialog.setMessage(item.getSnippet());
//      dialog.show();
      return true;
    }

}

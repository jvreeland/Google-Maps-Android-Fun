package com.geekground.gmapsample;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;

import com.google.android.maps.MapView;

public class TouchableMapView extends MapView{
    private Context mContext;
    private GestureDetector mGestureDetector;
   
    public TouchableMapView(Context aContext, AttributeSet attrs) {
      super(aContext, attrs);
      mContext = aContext;
   
      mGestureDetector = new GestureDetector((OnGestureListener)mContext);
    }
   
    public boolean onTouchEvent(MotionEvent ev) {
        
        // disable pinch - http://code.google.com/p/android/issues/detail?id=13172
        if(ev.getPointerCount() > 1) {
            return true;
        }
        
      if(this.mGestureDetector.onTouchEvent(ev)){
    	  return true; 
      } else {
        return super.onTouchEvent(ev);
      }
    }
}
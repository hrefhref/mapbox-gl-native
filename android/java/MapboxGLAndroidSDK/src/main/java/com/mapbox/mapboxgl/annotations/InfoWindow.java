package com.mapbox.mapboxgl.annotations;

import android.content.Context;
import android.graphics.PointF;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.mapbox.mapboxgl.geometry.LatLng;
import com.mapbox.mapboxgl.views.MapView;

/**
 * A tooltip view
 */
public class InfoWindow {

    private Marker boundMarker;
    private MapView mMapView;
    private boolean mIsVisible;
    protected View mView;

    static int mTitleId = 0;
    static int mDescriptionId = 0;
    static int mSubDescriptionId = 0;
    static int mImageId = 0;

    public InfoWindow(int layoutResId, MapView mapView) {
        mMapView = mapView;
        mIsVisible = false;
        ViewGroup parent = (ViewGroup) mapView.getParent();
        Context context = mapView.getContext();
        LayoutInflater inflater =
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mView = inflater.inflate(layoutResId, parent, false);

        if (mTitleId == 0) {
            setResIds(mapView.getContext());
        }

        // default behavior: close it when clicking on the tooltip:
        setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_UP) {
                    close();
                }
                return true;
            }
        });
    }

    public InfoWindow(View view, MapView mapView) {
        mMapView = mapView;
        mIsVisible = false;
        ViewGroup parent = (ViewGroup) mapView.getParent();
        Context context = mapView.getContext();
        LayoutInflater inflater =
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mView = view;

        // default behavior: close it when clicking on the tooltip:
        setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_UP) {
                    close();
                }
                return true;
            }
        });
    }

    /**
     * open the window at the specified position.
     *
     * @param object   the graphical object on which is hooked the view
     * @param position to place the window on the map
     * @param offsetX  (&offsetY) the offset of the view to the position, in pixels.
     *                 This allows to offset the view from the object position.
     * @return this infowindow
     */
    public InfoWindow open(Marker object, LatLng position, int offsetX, int offsetY) {
        onOpen(object);
        MapView.LayoutParams lp = new MapView.LayoutParams(MapView.LayoutParams.WRAP_CONTENT, MapView.LayoutParams.WRAP_CONTENT);
        PointF coords = mMapView.toScreenLocation(position);

        double y = mMapView.getTopOffsetPixelsForAnnotationSymbol(object.sprite);

        // Flip y coordinate as Android view origin is upper left corner
        coords.y = mMapView.getHeight() - coords.y;
        lp.leftMargin = (int) coords.x - 1;
        lp.topMargin = (int) coords.y + (int) y;

        close(); //if it was already opened
        mMapView.addView(mView, lp);
        mIsVisible = true;
        return this;
    }

    /**
     * Close this InfoWindow if it is visible, otherwise don't do anything.
     *
     * @return this info window
     */
    public InfoWindow close() {
        if (mIsVisible) {
            mIsVisible = false;
            ((ViewGroup) mView.getParent()).removeView(mView);
//            this.boundMarker.blur();
            setBoundMarker(null);
            onClose();
        }
        return this;
    }

    /**
     * Returns the Android view. This allows to set its content.
     *
     * @return the Android view
     */
    public View getView() {
        return mView;
    }

    /**
     * Returns the mapView this InfoWindow is bound to
     *
     * @return the mapView
     */
    public MapView getMapView() {
        return mMapView;
    }

    /**
     * Constructs the view that is displayed when the InfoWindow opens.
     * This retrieves data from overlayItem and shows it in the tooltip.
     *
     * @param overlayItem the tapped overlay item
     */
    public void onOpen(Marker overlayItem) {
        String title = overlayItem.getTitle();
        ((TextView) mView.findViewById(mTitleId /*R.id.title*/)).setText(title);
        String snippet = overlayItem.getSnippet();
        ((TextView) mView.findViewById(mDescriptionId /*R.id.description*/)).setText(snippet);

/*
        //handle sub-description, hiding or showing the text view:
        TextView subDescText = (TextView) mView.findViewById(mSubDescriptionId);
        String subDesc = overlayItem.getSubDescription();
        if ("".equals(subDesc)) {
            subDescText.setVisibility(View.GONE);
        } else {
            subDescText.setText(subDesc);
            subDescText.setVisibility(View.VISIBLE);
        }
*/
    }

    public void onClose() {
        //by default, do nothing
    }

    public InfoWindow setBoundMarker(Marker aBoundMarker) {
        this.boundMarker = aBoundMarker;
        return this;
    }

    public Marker getBoundMarker() {
        return boundMarker;
    }

    /**
     * Given a context, set the resource ids for the layout
     * of the InfoWindow.
     *
     * @param context
     */
    private static void setResIds(Context context) {
        String packageName = context.getPackageName(); //get application package name
        mTitleId = context.getResources().getIdentifier("id/infowindow_title", null, packageName);
        mDescriptionId =
                context.getResources().getIdentifier("id/infowindow_description", null, packageName);
        mSubDescriptionId = context.getResources()
                .getIdentifier("id/infowindow_subdescription", null, packageName);
        mImageId = context.getResources().getIdentifier("id/infowindow_image", null, packageName);
    }

    /**
     * Use to override default touch events handling on InfoWindow (ie, close automatically)
     *
     * @param listener New View.OnTouchListener to use
     */
    public void setOnTouchListener(View.OnTouchListener listener) {
        mView.setOnTouchListener(listener);
    }
}

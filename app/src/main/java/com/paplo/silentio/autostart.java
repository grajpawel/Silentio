package com.paplo.silentio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.paplo.silentio.GeofenceService;
import com.paplo.silentio.provider.PlaceContract;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by grajp on 10.11.2017.
 */

public class autostart extends BroadcastReceiver implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = autostart.class.getSimpleName();

    private GoogleApiClient mClient;
    private Geofencing mGeofencing;
    private Context mContext;

    public void onReceive(final Context context, Intent arg1) {
        mContext = context;

        Log.i(autostart.class.getSimpleName(), ".autostart started");

        mClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .build();
        mClient.connect();



    }


    public void refreshPlacesData(Context context) {

        Log.d(TAG, "Places Refreshed");
        Uri uri = PlaceContract.PlaceEntry.CONTENT_URI;
        Cursor data = context.getContentResolver().query(
                uri,
                null,
                null,
                null,
                null);
        if (data == null || data.getCount() == 0) return;
        List<String> guids = new ArrayList<>();
        final List<String> radiusList = new ArrayList<>();
        while (data.moveToNext()) {
            guids.add(data.getString(data.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PLACE_ID)));
            radiusList.add(data.getString(data.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PLACE_RADIUS)));
            Log.d(TAG, "MainActivity radius: " + radiusList.get(0));

        }
        com.google.android.gms.common.api.PendingResult<PlaceBuffer> placeBufferPendingResult = Places.GeoDataApi.getPlaceById(mClient, guids.toArray(new String[guids.size()]));
        placeBufferPendingResult.setResultCallback(new ResultCallback<PlaceBuffer>() {
            @Override
            public void onResult(@NonNull PlaceBuffer places) {

                mGeofencing.updateGeofencesList(places, radiusList);
                mGeofencing.registerAllGeofences();
            }
        });
        data.close();


    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "Location services connected in boot.");
        mGeofencing = new Geofencing(mContext, mClient);
        refreshPlacesData(mContext);
        mGeofencing.registerAllGeofences();

    }

    @Override
    public void onConnectionSuspended(int i) {

        Log.i(TAG, "Location services connected in boot.");
        mGeofencing = new Geofencing(mContext, mClient);
        refreshPlacesData(mContext);
        mGeofencing.registerAllGeofences();

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        Log.i(TAG, "Location services connected in boot.");
        mGeofencing = new Geofencing(mContext, mClient);
        refreshPlacesData(mContext);
        mGeofencing.registerAllGeofences();

    }
}




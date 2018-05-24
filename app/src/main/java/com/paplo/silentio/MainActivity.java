package com.paplo.silentio;

import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.PendingResults;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.paplo.silentio.provider.PlaceContract;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public static final String TAG = MainActivity.class.getSimpleName();
    private static final int PERMISSIONS_REQUEST_FINE_LOCATION = 101;
    private static final int PLACE_PICKER_REQUEST = 1;
    private PlaceListAdapter mAdapter;
    private boolean mIsEnabled;
    private GoogleApiClient mClient;
    private Geofencing mGeofencing;
    private static  MainActivity ins;
    private String placeId;
    public static String address;
    public static String name;

    private int placeNum;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent intent = getIntent();
        String extra = intent.getStringExtra("intent");
        Log.d(TAG, "extratext: " + extra);
        ins = this;






        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        onLocationPermissionClicked();
        final SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.shared_preferences_key), MODE_PRIVATE);
        final Switch onOffSwitch = findViewById(R.id.enable_switch);
        final NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);


        mIsEnabled = sharedPreferences.getBoolean(getString(R.string.enable_app_key), false);

        setPlaceIconColor(mIsEnabled);
        if (nm != null) {
            if (Build.VERSION.SDK_INT >= 24 && !nm.isNotificationPolicyAccessGranted()) {
               Snackbar snackbar = Snackbar.make(findViewById(R.id.coordinator), R.string.need_notification_permission_message, Snackbar.LENGTH_LONG);
               snackbar.setAction(R.string.action_settings_label, new SettingsListener());
               snackbar.show();


            } else {

                onOffSwitch.setEnabled(true);
            }
        }
        onOffSwitch.setChecked(mIsEnabled);

        onOffSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (nm != null && Build.VERSION.SDK_INT >= 24 && !nm.isNotificationPolicyAccessGranted()) {
                    Snackbar snackbar = Snackbar.make(findViewById(R.id.coordinator), R.string.need_notification_permission_message, Snackbar.LENGTH_LONG);
                    snackbar.setAction(R.string.action_settings_label, new SettingsListener());
                    snackbar.show();
                    onOffSwitch.setChecked(false);
                } else {
                setPlaceIconColor(isChecked);

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean(getString(R.string.enable_app_key), isChecked);
                editor.apply();
                mIsEnabled = isChecked;


                if (isChecked) {
                    checkGeofencesToRegister(mGeofencing);
                    Log.d(TAG, "Switch is on");


                } else {

                    removePlaceIdFromPref();

                    }

                }



            }
        });

        mClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .enableAutoManage(this, this)
                .build();

        mGeofencing = new Geofencing(this, mClient);
        final SwipeRefreshLayout mSwipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (mIsEnabled) {
                    refreshRinger();
                    NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    if (mNotificationManager != null) {
                        mNotificationManager.cancel(0);
                    }

                    onComplete();
                    mSwipeRefreshLayout.setRefreshing(false);
                } else {
                    mSwipeRefreshLayout.setRefreshing(false);
                }

            }
        });



        if (extra != null){
            removePlaceIdFromPref();
            refreshRinger();
            onComplete();
        }

        reloadAdapter();


       setActivePlaceTextViews();
        Log.d(TAG, "address: " + MainActivity.address);




    }

    void refreshRinger() {
        checkGeofencesToUnregister(mGeofencing);
        reloadAdapter();
        refreshPlacesData();
        AudioManager audioManager = (AudioManager) MainActivity.this.getSystemService(Context.AUDIO_SERVICE);
        final NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (nm != null) {
                if (audioManager != null && nm.isNotificationPolicyAccessGranted()) {
                    audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                }
            }
        } else {
            if (audioManager != null){
                audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);

            }
        }
    }

    private void onComplete() {
        mAdapter.notifyDataSetChanged();
        checkGeofencesToRegister(mGeofencing);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        refreshPlacesData();
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onLocationPermissionClicked() {
        if (ActivityCompat.checkSelfPermission(MainActivity.this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_FINE_LOCATION);
        }
    }


    public void onAddPlaceButtonClicked(View view) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (nm != null && Build.VERSION.SDK_INT >= 24 && !nm.isNotificationPolicyAccessGranted()) {
            Snackbar snackbar = Snackbar.make(findViewById(R.id.coordinator), R.string.need_notification_permission_message, Snackbar.LENGTH_LONG);
            snackbar.setAction(R.string.action_settings_label, new SettingsListener());
            snackbar.show();
            return;
        }
        if (placeNum <= 100) {
            Intent intent = new Intent(this, PlacePickerActivity.class);
            startActivity(intent);
        } else {
            Snackbar snackbar = Snackbar.make(findViewById(R.id.coordinator), R.string.geofence_limit_reached, Snackbar.LENGTH_LONG);
            snackbar.show();
        }

    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {

        refreshPlacesData();

        setActivePlaceTextViews();
        Log.i(TAG, "Connection successful!");

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    public void refreshPlacesData() {

        Uri uri = PlaceContract.PlaceEntry.CONTENT_URI;
        Cursor data = getContentResolver().query(
                uri,
                null,
                null,
                null,
                null);
        if (data == null || data.getCount() == 0) return;
        List<String> guids = new ArrayList<>();
        final List<String> radiusList = new ArrayList<>();
        placeNum = data.getCount();

        while (data.moveToNext()) {

            guids.add(data.getString(data.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PLACE_ID)));
            radiusList.add(data.getString(data.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PLACE_RADIUS)));

        }
        PendingResult<PlaceBuffer> placeBufferPendingResult = Places.GeoDataApi.getPlaceById(mClient, guids.toArray(new String[guids.size()]));
        placeBufferPendingResult.setResultCallback(new ResultCallback<PlaceBuffer>() {
            @Override
            public void onResult(@NonNull PlaceBuffer places) {

                mAdapter.swapPlaces(places);
                mAdapter.notifyDataSetChanged();
                mGeofencing.updateGeofencesList(places, radiusList);
                if (mIsEnabled) {
                    checkGeofencesToRegister(mGeofencing);
                }
            }
        });
        data.close();


    }

    void checkGeofencesToRegister(Geofencing geofencing){
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (nm != null && Build.VERSION.SDK_INT >= 24 && !nm.isNotificationPolicyAccessGranted()) {
            Snackbar snackbar = Snackbar.make(findViewById(R.id.coordinator), R.string.need_notification_permission_message, Snackbar.LENGTH_LONG);
            snackbar.setAction(R.string.action_settings_label, new SettingsListener());
            snackbar.show();


        } else {
            geofencing.registerAllGeofences();
            Log.d(TAG, "Geofence registered");

        }
    }


    void checkGeofencesToUnregister(Geofencing geofencing){
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (nm != null && Build.VERSION.SDK_INT >= 24 && !nm.isNotificationPolicyAccessGranted()) {
            Snackbar snackbar = Snackbar.make(findViewById(R.id.coordinator), R.string.need_notification_permission_message, Snackbar.LENGTH_LONG);
            snackbar.setAction(R.string.action_settings_label, new SettingsListener());
            snackbar.show();


        } else {
            geofencing.unRegisterAllGeofences();
            Log.d(TAG, "Geofence unregistered");

        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
         if (requestCode == PLACE_PICKER_REQUEST && resultCode == RESULT_OK) {
            Place place = PlacePicker.getPlace(this, data);
            if (place == null) {
                return;
            }
            String placeID = place.getId();

            ContentValues contentValues = new ContentValues();
            contentValues.put(PlaceContract.PlaceEntry.COLUMN_PLACE_ID, placeID);
            getContentResolver().insert(PlaceContract.PlaceEntry.CONTENT_URI, contentValues);
            refreshPlacesData();


        }
    }
    public void reloadAdapter(){
        final RecyclerView mRecyclerView = findViewById(R.id.places_list_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new PlaceListAdapter(this, null);
        mRecyclerView.setAdapter(mAdapter);
        mGeofencing = new Geofencing(this, mClient);

    }

    @Override
    protected void onResume() {
        super.onResume();



    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    public void onGoogleLogoClicked(View view) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.google.com/policies"));
        startActivity(browserIntent);
    }



    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }


    public void onActivePlaceClicked(View view) {
        Intent detailIntent = new Intent(getApplicationContext(), DetailActivity.class);
        detailIntent.putExtra("placeId", placeId);
        TextView nameTextView = findViewById(R.id.active_place_name_text_view);
        TextView addressTextView = findViewById(R.id.active_place_address_text_view);
        detailIntent.putExtra("placeName", nameTextView.getText().toString());
        detailIntent.putExtra("placeAddress", addressTextView.getText().toString());
        startActivity(detailIntent);
    }


    public class SettingsListener implements View.OnClickListener{

        @Override
        public void onClick(View v) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        }
    }
    public void setPlaceIconColor(boolean isChecked){
        ImageView placeIcon = findViewById(R.id.place_icon);
        ImageView map = findViewById(R.id.map_icon);
        ColorMatrix matrix = new ColorMatrix();
        if (isChecked){
            matrix.setSaturation(1);  //0 means grayscale

        } else {
            matrix.setSaturation(0);  //0 means grayscale
        }
        ColorMatrixColorFilter cf = new ColorMatrixColorFilter(matrix);
        placeIcon.setColorFilter(cf);
        map.setColorFilter(cf);

    }



    public void updateLayout(final int geofencetransition, final String geofenceId){
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                placeId = geofenceId;
                //placeName = geofenceName;
                LinearLayout linearLayout = findViewById(R.id.activePlaceLinearLayout);

                if (geofencetransition == Geofence.GEOFENCE_TRANSITION_ENTER || geofencetransition == Geofence.GEOFENCE_TRANSITION_DWELL) {

                    linearLayout.setVisibility(View.VISIBLE);

                } else {
                    linearLayout.setVisibility(View.GONE);
                }
                setActivePlaceTextViews();



            }
        });

    }

    public static  MainActivity getIns(){
        return ins;
    }

    public void removePlaceIdFromPref(){

        LinearLayout activePlaceLinearLayout = findViewById(R.id.activePlaceLinearLayout);
        activePlaceLinearLayout.setVisibility(View.GONE);

        checkGeofencesToUnregister(mGeofencing);
        AudioManager audioManager = (AudioManager) MainActivity.this.getSystemService(Context.AUDIO_SERVICE);
        final SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.shared_preferences_key), MODE_PRIVATE);
        String activePlaceId = sharedPreferences.getString(getString(R.string.active_place_id), null);
        Log.d(TAG,"MainActivePlaceId: " + activePlaceId);
        if (audioManager != null && activePlaceId != null) {
            audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(getString(R.string.active_place_id), null);
        editor.apply();

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.cancel(0);
        }


    }

    void setActivePlaceTextViews(){
        TextView activeAddressTextView = findViewById(R.id.active_place_address_text_view);
        if (activeAddressTextView != null) {
            activeAddressTextView.setText(MainActivity.address);

            TextView activeNameTextView = findViewById(R.id.active_place_name_text_view);
            activeNameTextView.setText(MainActivity.name);

        }


    }






}






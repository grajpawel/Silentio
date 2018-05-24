package com.paplo.silentio;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBufferResponse;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.common.primitives.Booleans;
import com.google.maps.android.SphericalUtil;
import com.paplo.silentio.provider.PlaceContract;
import com.paplo.silentio.provider.PlaceDbHelper;

import java.util.Arrays;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

public class DetailActivity extends AppCompatActivity {

    public static final String TAG = DetailActivity.class.getSimpleName();
    public static SQLiteDatabase db;
    private static final int PLACE_PICKER_REQUEST = 1;
    public static String placeNameFromDatabase;
    public static String placeRadius;
    public static String placeId;
    public static String placeNameFromIntent;
    public static String placeAddress;
    public EditText radiusEditText;
    public EditText nameEditText;
    public TextView nameTextView;
    public TextView addressTextView;
    public FloatingActionButton acceptButton;
    public int radiusInt;
    public static String placeIdFromPicker;
    public static String placeNameFromEditText;
    public static String placeRadiusFromEditText;
    private static String userPlaceRinger;
    private TextView ringerModeTextView;
    private static boolean showNotifications;
    private static boolean unmuteAfterTime;
    private LinearLayout timeConstraintsLinearLayout;
    private long startTimeLong;
    private long endTimeLong;
    private boolean timeConstraints;
    private MenuItem menuItem;
    private String encodedDays;
    private Boolean[] dayUserFinalArray = new Boolean[7];
    private TextView dayTextView;
    public static long startLong;
    public static long endLong;
    public Place activePlace;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        placeIdFromPicker = placeNameFromEditText = placeRadiusFromEditText = null;
        setContentView(R.layout.activity_detail);
        acceptButton = findViewById(R.id.accept_place_button);
        radiusEditText = findViewById(R.id.radiusPicker);
        nameEditText = findViewById(R.id.name_edit_text);
        nameTextView = findViewById(R.id.name_text_view);
        addressTextView = findViewById(R.id.address_text_view);
        ringerModeTextView = findViewById(R.id.place_ringer_mode);
        timeConstraintsLinearLayout = findViewById(R.id.time_constraints_detail);

        Arrays.fill(dayUserFinalArray, Boolean.FALSE);


        radiusEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {


            }

            @Override
            public void afterTextChanged(Editable s) {
                String tempRadiusString = s.toString();
                if (!tempRadiusString.isEmpty()) {
                    try {
                        radiusInt = (int) Float.parseFloat(tempRadiusString);
                        if (radiusInt > 1000 || radiusInt < 50){
                            radiusEditText.setError(getString(R.string.pick_radius_error_text));
                            if (menuItem != null)
                                menuItem.setVisible(false);
                            acceptButton.setVisibility(View.INVISIBLE);
                        } else {
                            if (menuItem != null)
                                menuItem.setVisible(true);
                            acceptButton.setVisibility(View.VISIBLE);
                            placeRadiusFromEditText = s.toString();
                        }
                    }
                    catch (NumberFormatException nfe){
                        Log.e(TAG, "No int selected" + nfe.toString());
                    }
                } else {
                    radiusEditText.setError(getString(R.string.pick_radius_error_text));
                }

            }
        });

        nameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                placeNameFromEditText = s.toString();

            }
        });

        db = new PlaceDbHelper(DetailActivity.this).getReadableDatabase();
        Uri uri = PlaceContract.PlaceEntry.CONTENT_URI;

        Intent startIntent = getIntent();
        placeId = startIntent.getStringExtra("placeId");
        placeAddress = startIntent.getStringExtra("placeAddress");
        placeNameFromIntent = startIntent.getStringExtra("placeName");
        Cursor data = getContentResolver().query(
                uri,
                null,
                "placeID='"+placeId+"'",
                null,
                null);

        if (data != null) {
            if (data.moveToNext()) {
                placeNameFromDatabase = data.getString(data.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PLACE_NAME));
                placeRadius = data.getString(data.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PLACE_RADIUS));
                userPlaceRinger = data.getString(data.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PLACE_RINGER));
                showNotifications = (data.getInt(data.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PLACE_NOTIFICATIONS)) == 1);
                timeConstraints = (data.getInt(data.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PLACE_TIME_CONSTRAINTS)) == 1);
                unmuteAfterTime = (data.getInt(data.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PLACE_UNMUTE_AFTER_TIME)) == 1);
                startTimeLong = data.getLong(data.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PLACE_START_TIME));
                endTimeLong = data.getLong(data.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PLACE_END_TIME));
                encodedDays = data.getString(data.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PLACE_DAYS));
                Log.d(TAG, encodedDays);
                startLong = startTimeLong;
                endLong = endTimeLong;



                data.close();


            }
        }

        String[] placeDaysString = encodedDays.split("_,_");
        for (int i = 0; i < placeDaysString.length; i++){
            dayUserFinalArray[i] = Boolean.parseBoolean(placeDaysString[i]);
            Log.d(TAG, i + " " + dayUserFinalArray[i].toString());
        }


        if (!timeConstraints){
            timeConstraintsLinearLayout.setVisibility(View.GONE);
        }
        dayTextView = findViewById(R.id.active_day_text_view);
        TextView startTimeTextView = findViewById(R.id.start_time_text_view);
        TextView endTimeTextView = findViewById(R.id.end_time_text_view);
        CheckBox timeConstraintsCheckBox = findViewById(R.id.time_constraints_checkbox);
        timeConstraintsCheckBox.setChecked(timeConstraints);

        setDayTextView();



        CheckBox unmuteCheckBox = findViewById(R.id.unmute_checkbox);
        unmuteCheckBox.setChecked(unmuteAfterTime);




        StringBuilder sbStart = new StringBuilder(50);
        Formatter fStart = new Formatter(sbStart, Locale.getDefault());
        String startTimeString  = DateUtils.formatDateRange(this, fStart, startTimeLong, startTimeLong, DateUtils.FORMAT_SHOW_TIME, TimeZone.getDefault().toString()).toString();

        StringBuilder sbEnd = new StringBuilder(50);
        Formatter fEnd = new Formatter(sbEnd, Locale.getDefault());
        String endTimeString  = DateUtils.formatDateRange(this, fEnd, endTimeLong, endTimeLong, DateUtils.FORMAT_SHOW_TIME, TimeZone.getDefault().toString()).toString();
        startTimeTextView.setText(startTimeString);
        endTimeTextView.setText(endTimeString);
        if (userPlaceRinger.equals(getString(R.string.silent_pref))) {
            ringerModeTextView.setText(getString(R.string.silent_label));
        } else {
            ringerModeTextView.setText(getString(R.string.vibrations_label));
        }

        CheckBox ringerPermissions = findViewById(R.id.notification_checkbox);
        ringerPermissions.setChecked(showNotifications);




        radiusEditText.setText(placeRadius);
        addressTextView.setText(placeAddress);
        nameTextView.setText(placeNameFromIntent);
        if ( placeNameFromDatabase != null && placeNameFromDatabase.isEmpty()){
            nameEditText.setHint(R.string.pref_name_hint);
        } else {
            nameEditText.setText(placeNameFromDatabase);
        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_detail, menu);
        menuItem = menu.findItem(R.id.action_save);
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_delete) {
            db.delete(PlaceContract.PlaceEntry.TABLE_NAME, "placeID=?", new String[]{placeId});
            returnToMain();
            return true;
        } else if (id == R.id.action_save){
            onDoneButtonClicked(null);
        }

        return super.onOptionsItemSelected(item);
    }

    public void returnToMain(){
        Intent mainActivityIntent = new Intent(this, MainActivity.class);
        mainActivityIntent.putExtra("intent", "MainActvity");
        startActivity(mainActivityIntent);
    }

    public void onDoneButtonClicked(View view) {
        final SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.shared_preferences_key), MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(getString(R.string.pref_radius_key), radiusInt);
        editor.apply();

        String convertedString = TextUtils.join("_,_", dayUserFinalArray);


        ContentValues contentValues = new ContentValues();

        if (!placeId.equals(placeIdFromPicker) && placeIdFromPicker != null)
        contentValues.put(PlaceContract.PlaceEntry.COLUMN_PLACE_ID, placeIdFromPicker);

        if (!placeRadius.equals(placeRadiusFromEditText))
        contentValues.put(PlaceContract.PlaceEntry.COLUMN_PLACE_RADIUS, radiusInt);

        if (placeRadiusFromEditText != null)
        contentValues.put(PlaceContract.PlaceEntry.COLUMN_PLACE_NAME, placeNameFromEditText);
        contentValues.put(PlaceContract.PlaceEntry.COLUMN_PLACE_NOTIFICATIONS, showNotifications);
        contentValues.put(PlaceContract.PlaceEntry.COLUMN_PLACE_RINGER, userPlaceRinger);
        contentValues.put(PlaceContract.PlaceEntry.COLUMN_PLACE_TIME_CONSTRAINTS, timeConstraints);
        contentValues.put(PlaceContract.PlaceEntry.COLUMN_PLACE_UNMUTE_AFTER_TIME, unmuteAfterTime);
        contentValues.put(PlaceContract.PlaceEntry.COLUMN_PLACE_START_TIME, startTimeLong);
        contentValues.put(PlaceContract.PlaceEntry.COLUMN_PLACE_END_TIME, endTimeLong);
        contentValues.put(PlaceContract.PlaceEntry.COLUMN_PLACE_DAYS, convertedString);

        Log.d(TAG, "Converted string: " + convertedString);




        //getContentResolver().update(PlaceContract.PlaceEntry.CONTENT_URI, contentValues, "placeID='"+placeId+"'", null);
        db.update(PlaceContract.PlaceEntry.TABLE_NAME, contentValues, "placeID='"+placeId+"'", null);
        returnToMain();


    }

    public void onChangeLocationButtonClicked(View view) {

        if (!haveNetworkConnection()){
            Snackbar snackbar = Snackbar.make(findViewById(R.id.coordinatorDetail), R.string.no_internet, Snackbar.LENGTH_SHORT);
            snackbar.show();
        } else {
            try {
                GeoDataClient mGeoDataClient = Places.getGeoDataClient(this);
                mGeoDataClient.getPlaceById(placeId).addOnCompleteListener(new OnCompleteListener<PlaceBufferResponse>() {

                    @SuppressLint("RestrictedApi")
                    @Override
                    public void onComplete(@NonNull Task<PlaceBufferResponse> task) {
                        if (task.isSuccessful()) {
                            PlaceBufferResponse places = task.getResult();
                            if (activePlace == null) {
                                activePlace = places.get(0);
                            }
                            //places.release();;
                            Log.i(TAG, "Place found: " + activePlace.getName());
                            test();
                        } else {
                            Log.e(TAG, "Place not found.");
                        }
                    }
                });




            } catch (Exception e) {
                Log.e(TAG, String.format("PlacePicker Exception: %s", e.getMessage()));
            }
        }

    }

    void test(){
        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
        if (activePlace != null){
            builder.setLatLngBounds(toBounds(activePlace.getLatLng(), 200));
        }

        Intent i = null;
        try {
            i = builder.build(this);
        } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }
        startActivityForResult(i, PLACE_PICKER_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_PICKER_REQUEST && resultCode == RESULT_OK) {

            Place place = PlacePicker.getPlace(this, data);
            activePlace = place;
            nameTextView.setText(place.getName().toString());
            addressTextView.setText(Objects.requireNonNull(place.getAddress()).toString());
            placeIdFromPicker = place.getId();
        }


    }

    public void showDialog(View view) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.action)
                .setIcon(R.drawable.ic_vibration_black_24dp)
                .setItems(R.array.modes_array, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0){
                            userPlaceRinger = getString(R.string.silent_pref);
                            ringerModeTextView.setText(R.string.silent_label);

                        } else {
                            userPlaceRinger = getString(R.string.vibrations_pref);
                            ringerModeTextView.setText(R.string.vibrations_label);

                        }

                    }
                });
        builder.create();
        builder.show();
    }

    public void onNotificationCheckBoxClicked(View view) {
        CheckBox notifificationPermissions = findViewById(R.id.notification_checkbox);
        showNotifications = notifificationPermissions.isChecked();
    }

    public void onMuteCheckBoxClicked(View view) {
        CheckBox unmuteCheckBox = findViewById(R.id.unmute_checkbox);
        unmuteAfterTime = unmuteCheckBox.isChecked();

    }

    public void showEndTimePicker(View view) {
        DialogFragment endTimePickerFragment = new TimePickerFragment();
        endTimePickerFragment.show(getFragmentManager(), "endTimePickerDetail");
    }

    public void showStartTimePicker(View view) {
        DialogFragment endTimePickerFragment = new TimePickerFragment();
        endTimePickerFragment.show(getFragmentManager(), "startTimePickerDetail");
    }



    public void showDayPicker(View view) {


        boolean[] setChecked = Booleans.toArray(Arrays.asList(dayUserFinalArray));


        final AlertDialog dialog = new AlertDialog.Builder(this, R.style.DayPickerDialogTheme)

                .setTitle(R.string.active_days)
                .setIcon(R.drawable.ic_date_range_black_24dp)
                .setMultiChoiceItems(R.array.days_array, setChecked, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {

                    }
                })

                .setPositiveButton(R.string.action_accept, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setDayTextView();


                    }
                })
                .setNegativeButton(R.string.action_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })


                .create();



        dialog.getListView().setItemsCanFocus(false);
        dialog.getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        View testView = dialog.getListView().getChildAt(2);
        if (testView != null) {
            CheckedTextView testTextView = (CheckedTextView) testView;
            testTextView.setChecked(true);
        }
        dialog.getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                CheckedTextView checkedTextView = (CheckedTextView) view;
                dayUserFinalArray[position] = checkedTextView.isChecked();

            }
        });

        dialog.show();

    }



    public void setTimeTextViews(String tag, String time, long timeInMillis){
        if (tag.equals("endTimePickerDetail")){
            TextView endTimeTextView = findViewById(R.id.end_time_text_view);
            endTimeTextView.setText(time);
            endTimeLong = timeInMillis;
            endLong = timeInMillis;

        } else if (tag.equals("startTimePickerDetail")){
            TextView startTimeTextView = findViewById(R.id.start_time_text_view);
            startTimeTextView.setText(time);
            startTimeLong = timeInMillis;
            startLong = timeInMillis;

        }

    }

    public void onTimeConstraintsCheckBoxClicked(View view) {
        CheckBox TimeConstraintsCheckBox = findViewById(R.id.time_constraints_checkbox);
        timeConstraints = TimeConstraintsCheckBox.isChecked();
        if (TimeConstraintsCheckBox.isChecked()){
            timeConstraintsLinearLayout.setVisibility(View.VISIBLE);
        } else {
            timeConstraintsLinearLayout.setVisibility(View.GONE);
        }
    }

    public void setDayTextView(){

        StringBuilder dayString = new StringBuilder();
        String[] dayArray = getResources().getStringArray(R.array.days_array);
        int run = 0;



        for (int i = 0; i < dayUserFinalArray.length; i++){
            if (dayUserFinalArray[i]){
                dayString.append(dayArray[i].substring(0, 3)).append(", ");
                run ++;
            }
        }

        if (dayString.length() == 0){
            dayString = new StringBuilder(getString(R.string.never));
        } else {
            if (run == 7){
                dayString = new StringBuilder(getString(R.string.everyday));
            } else {
                dayString = new StringBuilder(dayString.substring(0, dayString.length() - 2));
            }
        }

        dayTextView.setText(dayString.toString());

    }

    private boolean haveNetworkConnection() {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;

        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = new NetworkInfo[0];
        if (cm != null) {
            netInfo = cm.getAllNetworkInfo();
        }
        for (NetworkInfo ni : netInfo) {
            if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                if (ni.isConnected())
                    haveConnectedWifi = true;
            if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                if (ni.isConnected())
                    haveConnectedMobile = true;
        }
        return haveConnectedWifi || haveConnectedMobile;
    }

    public LatLngBounds toBounds(LatLng center, double radiusInMeters) {
        double distanceFromCenterToCorner = radiusInMeters * Math.sqrt(2.0);
        LatLng southwestCorner =
                SphericalUtil.computeOffset(center, distanceFromCenterToCorner, 225.0);
        LatLng northeastCorner =
                SphericalUtil.computeOffset(center, distanceFromCenterToCorner, 45.0);
        return new LatLngBounds(southwestCorner, northeastCorner);
    }


}

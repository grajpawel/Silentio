package com.paplo.silentio;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.widget.LinearLayout;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.paplo.silentio.provider.PlaceContract;
import com.paplo.silentio.provider.PlaceDbHelper;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static android.content.Context.MODE_PRIVATE;



public class GeofenceBroadcastReceiver extends BroadcastReceiver{
    public static final String TAG = GeofenceBroadcastReceiver.class.getSimpleName();
    public static final String CHANNEL_ID = "channel_id";
    private String placeName;
    private String userPlaceRinger;
    private boolean showNotifications;
    private boolean timeConstraints;
    private boolean unmuteAfterTime;
    private long startTimeLong;
    private long endTimeLong;
    private String encodedDays;
    private Boolean[] placeDays = new Boolean[7];
    private String geofenceId;





    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Geofence activated");
        final SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.shared_preferences_key), MODE_PRIVATE);
        boolean isEnabled = sharedPreferences.getBoolean(context.getString(R.string.enable_app_key), false);
        if (isEnabled) {
            Uri uri = PlaceContract.PlaceEntry.CONTENT_URI;


            GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
            List<Geofence> geofenceList = geofencingEvent.getTriggeringGeofences();

            if (geofenceList == null)
                return;

            geofenceId = geofenceList.get(0).getRequestId();
            PlaceListAdapter.activeId = geofenceId;


            Cursor data = context.getContentResolver().query(
                    uri,
                    null,
                    "placeID='" + geofenceId + "'",
                    null,
                    null);
            if (data != null) {
                if (data.moveToNext()) {
                    placeName = data.getString(data.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PLACE_NAME));
                    userPlaceRinger = data.getString(data.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PLACE_RINGER));
                    showNotifications = (data.getInt(data.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PLACE_NOTIFICATIONS)) == 1);
                    timeConstraints = (data.getInt(data.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PLACE_TIME_CONSTRAINTS)) == 1);
                    unmuteAfterTime = (data.getInt(data.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PLACE_UNMUTE_AFTER_TIME)) == 1);
                    startTimeLong = data.getLong(data.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PLACE_START_TIME));
                    endTimeLong = data.getLong(data.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PLACE_END_TIME));
                    encodedDays = data.getString(data.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PLACE_DAYS));



                } else {
                    data.close();
                }
            } else {
                return;
            }

            List<Address> addresses;
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());


            try {

                addresses = geocoder.getFromLocation(geofencingEvent.getTriggeringLocation().getLatitude(), geofencingEvent.getTriggeringLocation().getLongitude(), 1);


                if (placeName == null || placeName.isEmpty()) {

                    MainActivity.name = addresses.get(0).getThoroughfare() + " " + addresses.get(0).getFeatureName();
                    MainActivity.address = addresses.get(0).getLocality() + ", " + addresses.get(0).getAdminArea() + ", " + addresses.get(0).getCountryName();
                } else {
                    MainActivity.address = addresses.get(0).getAddressLine(0);
                    MainActivity.name = placeName;

                }


            } catch (IOException e) {
                e.printStackTrace();
            }



            if (!encodedDays.isEmpty()) {
                String[] placeDaysString = encodedDays.split("_,_");
                for (int i = 0; i < placeDaysString.length; i++) {
                    placeDays[i] = Boolean.parseBoolean(placeDaysString[i]);
                }
            }

        /*

        Log.d(TAG, "Final ringer: " + userPlaceRinger);
        Log.d(TAG, "Final notifications: " + showNotifications);
        Log.d(TAG, "Final time constraints: " + timeConstraints);
        Log.d(TAG, "Final start time: " + startTimeLong);
        Log.d(TAG, "Final end time: " + endTimeLong);

        */

            Calendar rightNow = Calendar.getInstance();
            int currentDay = rightNow.get(Calendar.DAY_OF_WEEK) - 1;
            if (currentDay == 0)
                currentDay = 7;
            int currentHour = rightNow.get(Calendar.HOUR_OF_DAY);
            int currentMinute = rightNow.get(Calendar.MINUTE);
            long minuteInMillis = TimeUnit.MINUTES.toMillis(currentMinute);
            long hoursInMillis = TimeUnit.HOURS.toMillis(currentHour);
            long currentTimeInMillis = hoursInMillis + minuteInMillis;

            int geofenceTransition = geofencingEvent.getGeofenceTransition();



            if (timeConstraints) {

                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                Intent intent1 = new Intent(context, autostart.class);
                PendingIntent alarmStartIntent = PendingIntent.getBroadcast(context, 0, intent1, PendingIntent.FLAG_UPDATE_CURRENT);
                PendingIntent alarmEndIntent = PendingIntent.getBroadcast(context, 1, intent1, PendingIntent.FLAG_UPDATE_CURRENT);

                int hEnd = (int) ((endTimeLong / 1000) / 3600);
                int mEnd = (int) (((endTimeLong / 1000) / 60) % 60);
                int hStart = (int) ((startTimeLong / 1000) / 3600);
                int mStart = (int) (((startTimeLong / 1000) / 60) % 60);


                Calendar calendarStart = Calendar.getInstance();
                calendarStart.setTimeInMillis(System.currentTimeMillis());
                calendarStart.set(Calendar.HOUR_OF_DAY, hStart);
                calendarStart.set(Calendar.MINUTE, mStart);

                if (alarmManager != null) {
                    alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendarStart.getTimeInMillis() + TimeUnit.MINUTES.toMillis(1), AlarmManager.INTERVAL_DAY, alarmStartIntent);
                }


                if (unmuteAfterTime) {


                    Calendar calendarEnd = Calendar.getInstance();
                    calendarEnd.setTimeInMillis(System.currentTimeMillis());
                    calendarEnd.set(Calendar.HOUR_OF_DAY, hEnd);
                    calendarEnd.set(Calendar.MINUTE, mEnd);

                    if (alarmManager != null) {
                        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendarEnd.getTimeInMillis() + TimeUnit.MINUTES.toMillis(1), AlarmManager.INTERVAL_DAY, alarmEndIntent);
                    }


                }

                if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER || geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL) {// check if user entered geofence


                    if (placeDays[currentDay - 1]) {
                        if (endTimeLong - startTimeLong > 0) {
                            if (currentTimeInMillis >= startTimeLong && currentTimeInMillis <= endTimeLong) {
                                getGeofenceRinger(context, geofenceTransition);
                            } else {
                                if (unmuteAfterTime) {
                                    getGeofenceRinger(context, Geofence.GEOFENCE_TRANSITION_EXIT);
                                }
                            }
                        } else if (endTimeLong - startTimeLong <= 0) {
                            if (currentTimeInMillis >= startTimeLong || currentTimeInMillis <= endTimeLong) {
                                getGeofenceRinger(context, geofenceTransition);
                            } else {
                                if (unmuteAfterTime) {
                                    getGeofenceRinger(context, Geofence.GEOFENCE_TRANSITION_EXIT);
                                }
                            }
                        }
                    }
                } else {
                    getGeofenceRinger(context, geofenceTransition);
                }
            } else {
                getGeofenceRinger(context, geofenceTransition);
            }


            if (geofencingEvent.hasError()) {
                Log.e(TAG, "Error: " + geofencingEvent.getErrorCode());

            }
        }
    }



    private void getGeofenceRinger(Context context, int geofenceTransition){

        final SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.shared_preferences_key), MODE_PRIVATE);
        String activePlaceId = sharedPreferences.getString(context.getString(R.string.active_place_id), null);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        try {
            MainActivity.getIns().updateLayout(geofenceTransition, geofenceId);
        } catch (Exception e){
            Log.e(TAG, "exception" + e.toString());
        }

        Log.d(TAG, "active place id: " + activePlaceId);

        Log.d(TAG, "geofence id: " + geofenceId);



        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER || geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL){

            if (showNotifications && !Objects.equals(activePlaceId, geofenceId)) {
                Log.d(TAG, "Notification on");
                sendNotification(context, geofenceTransition, placeName);
            } else if (!showNotifications){
                clearNotification(context);
            }

            editor.putString(context.getString(R.string.active_place_id), geofenceId);



            if (userPlaceRinger.equals(context.getString(R.string.silent_pref))){
                setRingerMode(context, AudioManager.RINGER_MODE_SILENT);

            } else if (userPlaceRinger.equals(context.getString(R.string.vibrations_pref))){
                setRingerMode(context, AudioManager.RINGER_MODE_VIBRATE);
            }
        }


        else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT && Objects.equals(activePlaceId, geofenceId)){
            if (showNotifications)
                sendNotification(context, geofenceTransition, placeName);


            editor.putString(context.getString(R.string.active_place_id), null);

            setRingerMode(context, AudioManager.RINGER_MODE_NORMAL);
        }
        else {
            Log.e(TAG, "Unknown transition : &d"+ geofenceTransition);
            return;
        }

        editor.apply();


    }
    private void clearNotification(Context context){
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (mNotificationManager != null) {
            mNotificationManager.cancel(0);
        }

    }

    private void sendNotification(Context context, int geofenceTransition, String placeName) {



        Intent notificationIntent = new Intent(context, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);

        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(notificationIntent);



        PendingIntent notificationPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(context);

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER || geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL){
            if (userPlaceRinger.equals(context.getString(R.string.silent_pref))){
                builder.setSmallIcon(R.drawable.ic_notifications_off_black_24dp)
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_notifications_off_black_24dp));
            } else if (userPlaceRinger.equals(context.getString(R.string.vibrations_pref))){
                builder.setSmallIcon(R.drawable.ic_vibration_black_24dp)
                        .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_vibration_black_24dp));
            }

            if (placeName != null && !placeName.isEmpty()) {
                builder.setContentTitle(context.getString(R.string.silent_mode_activated_with_name) + " " + placeName);
            } else {
                builder.setContentTitle(context.getString(R.string.silent_mode_activated_without_name));
            }
        } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT){
            builder.setSmallIcon(R.drawable.ic_notifications_active_black_24dp)
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_notifications_active_black_24dp))
                    .setContentTitle(context.getString(R.string.back_to_normal));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID);
        }
        builder.setWhen(System.currentTimeMillis())
                .setShowWhen(true)
                .setContentText(context.getString(R.string.touch_to_relaunch))
                .setContentIntent(notificationPendingIntent)
                .setAutoCancel(true);

        NotificationManager mNotificationMenager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ) {
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, context.getString(R.string.channel_label), NotificationManager.IMPORTANCE_LOW);
            if (mNotificationMenager != null) {
                mNotificationMenager.createNotificationChannel(notificationChannel);
            }
        }

        if (mNotificationMenager != null) {
            mNotificationMenager.notify(0, builder.build());
        }
    }

    private void setRingerMode(Context context, int mode) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        // Check for DND permissions for API 24+
        if (nm != null && (Build.VERSION.SDK_INT < 24 || nm.isNotificationPolicyAccessGranted())) {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                audioManager.setRingerMode(mode);
            }
        }
    }
}

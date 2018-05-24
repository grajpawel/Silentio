package com.paplo.silentio.provider;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by grajp_000 on 13.10.2017.
 */

public class PlaceContract {

    static final String AUTHORITY = "com.paplo.silentio";
    static final Uri BASE_CONTENT_URI = Uri.parse("content://" + AUTHORITY);
    static final String PATH_PLACES = "places";

    public static final class PlaceEntry implements BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_PLACES).build();
        public static final String TABLE_NAME = "places";
        public static final String COLUMN_PLACE_ID = "placeID";
        public static final String COLUMN_PLACE_RADIUS = "placeRadius";
        public static final String COLUMN_PLACE_NAME = "placeName";
        public static final String COLUMN_PLACE_RINGER = "placeRinger";
        public static final String COLUMN_PLACE_NOTIFICATIONS = "placeNotifications";
        public static final String COLUMN_PLACE_TIME_CONSTRAINTS = "placeTimeConstraints";
        public static final String COLUMN_PLACE_UNMUTE_AFTER_TIME = "unmuteAfterTime";
        public static final String COLUMN_PLACE_DAYS = "placeDays";
        public static final String COLUMN_PLACE_START_TIME = "placeStartTime";
        public static final String COLUMN_PLACE_END_TIME = "placeEndTime";


    }
}

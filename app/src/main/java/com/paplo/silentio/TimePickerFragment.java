package com.paplo.silentio;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.util.TimeUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TimePicker;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Created by grajp on 23.11.2017.
 */

public class TimePickerFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener {



    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        int hour;
        int minute;



        if (getTag().contains("endTimePickerDetail")){
            hour = (int) TimeUnit.MILLISECONDS.toHours(DetailActivity.endLong);
            long hourInMillis = TimeUnit.HOURS.toMillis(hour);
            minute = (int) TimeUnit.MILLISECONDS.toMinutes(DetailActivity.endLong - hourInMillis);

        } else if (getTag().contains("startTimePickerDetail")){
            hour = (int) TimeUnit.MILLISECONDS.toHours(DetailActivity.startLong);
            long hourInMillis = TimeUnit.HOURS.toMillis(hour);
            minute = (int) TimeUnit.MILLISECONDS.toMinutes(DetailActivity.startLong - hourInMillis);

        }else if (getTag().contains("startTimePicker") && PlacePickerActivity.startLong != 0){

            hour = (int) TimeUnit.MILLISECONDS.toHours(PlacePickerActivity.startLong);
            long hourInMillis = TimeUnit.HOURS.toMillis(hour);
            minute = (int) TimeUnit.MILLISECONDS.toMinutes(PlacePickerActivity.startLong - hourInMillis);

        } else if (getTag().contains("endTimePicker") && PlacePickerActivity.startLong != 0){

            hour = (int) TimeUnit.MILLISECONDS.toHours(PlacePickerActivity.endLong);
            long hourInMillis = TimeUnit.HOURS.toMillis(hour);
            minute = (int) TimeUnit.MILLISECONDS.toMinutes(PlacePickerActivity.endLong - hourInMillis);

        } else {
            final Calendar c = Calendar.getInstance();
            hour = c.get(Calendar.HOUR_OF_DAY);
            minute = c.get(Calendar.MINUTE);
        }

        return new TimePickerDialog(getActivity(), this, hour, minute, android.text.format.DateFormat.is24HourFormat(getActivity()));

    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        long minuteInMillis = TimeUnit.MINUTES.toMillis(minute);
        long hoursInMillis = TimeUnit.HOURS.toMillis(hourOfDay);
        long timeInMillis = hoursInMillis + minuteInMillis;


        StringBuilder sb = new StringBuilder(50);
        Formatter f = new Formatter(sb, Locale.getDefault());

        if (getTag().contains("Detail")){
            DetailActivity detailActivity = (DetailActivity) getActivity();
            String time  = DateUtils.formatDateRange(detailActivity.getBaseContext(), f, timeInMillis, timeInMillis, DateUtils.FORMAT_SHOW_TIME, TimeZone.getDefault().toString()).toString();
            detailActivity.setTimeTextViews(getTag(), time, timeInMillis);



        } else {
            PlacePickerActivity placePickerActivity = (PlacePickerActivity) getActivity();
            String time  = DateUtils.formatDateRange(placePickerActivity.getBaseContext(), f, timeInMillis, timeInMillis, DateUtils.FORMAT_SHOW_TIME, TimeZone.getDefault().toString()).toString();
            placePickerActivity.setTimeTextViews(getTag(), time, timeInMillis);
        }
        LinearLayout endTimeLinearLayout = getActivity().findViewById(R.id.end_time_linear_layout);
        endTimeLinearLayout.setVisibility(View.VISIBLE);


    }
}

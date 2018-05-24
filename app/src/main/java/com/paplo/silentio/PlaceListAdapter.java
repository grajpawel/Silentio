package com.paplo.silentio;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.location.places.PlaceBuffer;
import com.paplo.silentio.provider.PlaceContract;
import com.paplo.silentio.provider.PlaceDbHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static android.content.Context.MODE_PRIVATE;
import static com.paplo.silentio.DetailActivity.placeAddress;


public class PlaceListAdapter extends RecyclerView.Adapter<PlaceListAdapter.PlaceViewHolder> {
    private static String TAG = PlaceListAdapter.class.getSimpleName();
    private Context mContext;
    private PlaceBuffer mPlaces;
    private List<String> placeNames;
    private List<String> placeRingers;
    public static String activeId;
    public static String activePlaceName;
    public static String activePlaceAddress;
    private Geocoder geocoder;




    PlaceListAdapter(Context context, PlaceBuffer places){


        this.mContext = context;
        this.mPlaces = places;



    }

    @Override
    public PlaceListAdapter.PlaceViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
        Log.d(TAG, "View Created");

        LayoutInflater inflater = LayoutInflater.from(mContext);
        final View view = inflater.inflate(R.layout.item_place_card, parent, false);
        Uri uri = PlaceContract.PlaceEntry.CONTENT_URI;

        Cursor data = mContext.getContentResolver().query(
                uri,
                null,
                null,
                null,
                null);

        placeNames = new ArrayList<>();
        placeRingers = new ArrayList<>();

        if (data != null) {
            while (data.moveToNext()){
                placeNames.add(data.getString(data.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PLACE_NAME)));
                placeRingers.add(data.getString(data.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PLACE_RINGER)));
            }
            data.close();

        }


        return new PlaceViewHolder(view);
    }


    @Override
    public void onBindViewHolder(final PlaceViewHolder holder, final int position) {

        List<Address> addresses = null;
        geocoder = new Geocoder(mContext, Locale.getDefault());
        try {
            addresses = geocoder.getFromLocation(mPlaces.get(position).getLatLng().latitude, mPlaces.get(position).getLatLng().longitude, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String placeNameFromDataBase = placeNames.get(position);
        String placeName = null;
        String placeAddress = null;
        if (placeNameFromDataBase != null && !placeNameFromDataBase.isEmpty()){
            placeName = placeNameFromDataBase;
            placeAddress = mPlaces.get(position).getAddress().toString();

        } else {
            if (addresses != null) {
                if (addresses.get(0).getThoroughfare() != null) {
                    placeName = addresses.get(0).getThoroughfare() + " " + addresses.get(0).getFeatureName();
                } else {
                    placeName = addresses.get(0).getFeatureName();
                }
                if (addresses.get(0).getLocality() != null) {
                    placeAddress = addresses.get(0).getLocality() + ", " + addresses.get(0).getAdminArea() + ", " + addresses.get(0).getCountryName();
                } else {
                    placeAddress = addresses.get(0).getAdminArea() + ", " + addresses.get(0).getCountryName();

                }
            } else {
                if (placeNameFromDataBase != null && !placeNameFromDataBase.isEmpty()){
                    placeName = placeNames.get(position);
                } else {

                    placeName = mPlaces.get(position).getName().toString();
                }
                placeAddress = mPlaces.get(position).getAddress().toString();


            }
        }
        activePlaceAddress = placeAddress;
        activePlaceName = placeName;
        if (placeRingers.get(position).equals(mContext.getString(R.string.silent_pref))){
            holder.iconImageView.setImageResource(R.drawable.ic_volume_off_black_24dp);
        } else {
            holder.iconImageView.setImageResource(R.drawable.ic_vibration_black_24dp);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            holder.iconImageView.setColorFilter(mContext.getColor(R.color.colorAccent));
        }

        String placeId = mPlaces.get(position).getId();

        if (Objects.equals(activeId, placeId)){
            Log.d(TAG, "ID equals: " + placeId);
            Log.d(TAG, "Name equals: " + placeName);
            Log.d(TAG, "Address equals: " + placeAddress);
            MainActivity.address = placeAddress;
            MainActivity.name = placeName;

        }
        holder.nameTextView.setText(placeName);
        holder.addressTextView.setText(placeAddress);
        holder.nameTextView.setTag(R.string.tag_key, placeId);
        holder.itemView.setVisibility(View.VISIBLE);

        holder.itemView.setOnClickListener(new View.OnClickListener() {

        @Override
            public void onClick(View v) {

                String tag = holder.nameTextView.getTag(R.string.tag_key).toString();
                String placeName = holder.nameTextView.getText().toString();
                String placeAddress = holder.addressTextView.getText().toString();


            Intent detailIntent = new Intent(mContext, DetailActivity.class);
            detailIntent.putExtra("placeId", tag);
            detailIntent.putExtra("placeName", placeName);
            detailIntent.putExtra("placeAddress", placeAddress);
            mContext.startActivity(detailIntent);


        }
        });
    }

    void swapPlaces(PlaceBuffer newPlaces){
        Log.d(TAG, "View Swapped");

        mPlaces = newPlaces;
        if (mPlaces != null){
            this.notifyDataSetChanged();
        }
    }



    @Override
    public int getItemCount() {
        if(mPlaces==null) return 0;
        return mPlaces.getCount();
    }



    /**
     * PlaceViewHolder class for the recycler view item
     */
    class PlaceViewHolder extends RecyclerView.ViewHolder{

        TextView nameTextView;
        TextView addressTextView;
        ImageView iconImageView;

        PlaceViewHolder(View itemView) {

            super(itemView);
            Log.d(TAG, "PlaceViewHolder");

            itemView.setVisibility(View.VISIBLE);
            nameTextView = itemView.findViewById(R.id.name_text_view);
            addressTextView = itemView.findViewById(R.id.address_text_view);
            iconImageView = itemView.findViewById(R.id.place_image_view);

        }



    }



}

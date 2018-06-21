package com.kieronquinn.library.amazfitcommunication.location;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.huami.watch.transport.DataBundle;
import com.huami.watch.transport.SafeParcelable;

public class GeoData {
    public enum GeoEvent {
        LOCATION_CHANGED,
        STATUS_CHANGED,
        PROVIDER_ENABLED,
        PROVIDER_DISABLED
    }

    private GeoEvent event;
    private Location location;
    private String provider;
    private int status;

    private static final String TAG = GeoData.class.getSimpleName();

    private GeoData() {
    }

    public DataBundle toDataBundle() {
        DataBundle dataBundle = new DataBundle();
        dataBundle.putString("event", event.name());

        if (location != null) {
            dataBundle.putDouble("lat", location.getLatitude());
            dataBundle.putDouble("lng", location.getLongitude());
            dataBundle.putFloat("acc", location.getAccuracy());
            dataBundle.putString("location_provider", location.getProvider());
        }
        dataBundle.putString("provider", provider);
        dataBundle.putInt("status", status);
        return dataBundle;
    }

    public static GeoData fromDataBundle(DataBundle dataBundle) {
        GeoData geoData = new GeoData();
        geoData.event = GeoEvent.valueOf(dataBundle.getString("event"));
        geoData.provider = dataBundle.getString("provider");

        String locationProvider = dataBundle.getString("location_provider");
        if (locationProvider != null) {
            geoData.location = new Location(locationProvider);
            geoData.location.setLatitude(dataBundle.getDouble("lat"));
            geoData.location.setLongitude(dataBundle.getDouble("lng"));
            geoData.location.setAccuracy(dataBundle.getFloat("acc"));
        }

        geoData.status = dataBundle.getInt("status");
        return geoData;
    }

    public void applyTo(LocationListener listener) {
        switch (event) {
            case STATUS_CHANGED:
                listener.onStatusChanged(provider, status, null);
                break;
            case LOCATION_CHANGED:
                listener.onLocationChanged(location);
                break;
            case PROVIDER_DISABLED:
                listener.onProviderDisabled(provider);
                break;
            case PROVIDER_ENABLED:
                listener.onProviderDisabled(provider);
                break;
        }
    }

    public static class Listener implements LocationListener {
        private final Callback callback;
        private final String uuid;

        Listener(Callback callback, String uuid) {
            this.callback = callback;
            this.uuid = uuid;
        }

        @Override
        public void onLocationChanged(Location location) {
            Log.d(TAG, "onLocationChanged " + location);
            GeoData data = new GeoData();
            data.event = GeoEvent.LOCATION_CHANGED;
            data.location = location;
            callback.onGeoData(uuid, data);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.d(TAG, "onStatusChanged " + provider + ": " + status);
            GeoData data = new GeoData();
            data.event = GeoEvent.STATUS_CHANGED;
            data.provider = provider;
            data.status = status;
            //data.extras = extras;
            callback.onGeoData(uuid, data);
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.d(TAG, "onProviderEnabled " + provider);
            GeoData data = new GeoData();
            data.event = GeoEvent.PROVIDER_ENABLED;
            data.provider = provider;
            callback.onGeoData(uuid, data);
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.d(TAG, "onProviderDisabled " + provider);
            GeoData data = new GeoData();
            data.event = GeoEvent.PROVIDER_DISABLED;
            data.provider = provider;
            callback.onGeoData(uuid, data);
        }
    }

    public interface Callback {
        void onGeoData(String uuid, GeoData data);
    }

    private static class LocationWrapper implements SafeParcelable {

        private final Location location;

        LocationWrapper(Location location) {
            this.location = location;
        }

        @Override
        public int describeContents() {
            return location.describeContents();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            location.writeToParcel(dest, flags);
        }

        public static final Parcelable.Creator<LocationWrapper> CREATOR
                = new Parcelable.Creator<LocationWrapper>() {
            public LocationWrapper createFromParcel(Parcel in) {
                Log.d(getClass().getSimpleName(), "Creating LocationWrapper from Parcel");
                return new LocationWrapper(Location.CREATOR.createFromParcel(in));
            }

            public LocationWrapper[] newArray(int size) {
                return new LocationWrapper[size];
            }
        };

        public Location getLocation() {
            return location;
        }
    }
}

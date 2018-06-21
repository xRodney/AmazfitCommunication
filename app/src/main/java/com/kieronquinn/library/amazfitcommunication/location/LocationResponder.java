package com.kieronquinn.library.amazfitcommunication.location;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Looper;

import com.huami.watch.transport.DataBundle;
import com.huami.watch.transport.TransportDataItem;
import com.kieronquinn.library.amazfitcommunication.Transporter;
import com.kieronquinn.library.amazfitcommunication.common.CommonTransporterItem;

import java.util.HashMap;
import java.util.Map;


public class LocationResponder extends CommonTransporterItem implements GeoData.Callback {

    private final Map<String, LocationListener> locationListeners = new HashMap<>();

    private final LocationManager locationManager;
    private final Looper looper;

    public LocationResponder(Transporter transporter, LocationManager locationManager, Looper looper) {
        super(transporter, null);
        this.locationManager = locationManager;
        this.looper = looper;
    }

    @Override
    protected void disconnect(String uuid) {
        super.disconnect(uuid);
        LocationListener removed = locationListeners.remove(uuid);
        if (removed != null) {
            locationManager.removeUpdates(removed);
        }
    }

    @Override
    public void onDataReceived(TransportDataItem item) {

        String uuid = item.getData().getString("uuid");
        String action = item.getAction();

        switch (action) {
            case ProxyLocationManager.REQUEST_LOC_SUBSCRIBE:
                subscribe(item, uuid);
                break;

            case ProxyLocationManager.REQUEST_LOC_MORE:
                send(ProxyLocationManager.RESPONSE_LOC_ACK, uuidBundle(uuid), 2 * 60 * 1000);
                break;

            case ProxyLocationManager.REQUEST_LOC_UNSUBSCRIBE:
                disconnect(uuid);
                break;

        }
    }

    private void subscribe(TransportDataItem item, String uuid) {
        GeoSubscription subscription = GeoSubscription.fromDataBundle(item.getData());
        LocationListener locationListener = new GeoData.Listener(this, uuid);
        locationListeners.put(uuid, locationListener);
        locationManager.requestLocationUpdates(subscription.getProvider(), subscription.getMinTime(), subscription.getMinDistance(), locationListener, looper);

        Location lastKnownLocation = locationManager.getLastKnownLocation(subscription.getProvider());
        if (lastKnownLocation != null) {
            locationListener.onLocationChanged(lastKnownLocation);
        } else {
            send(ProxyLocationManager.RESPONSE_LOC_ACK, uuidBundle(uuid), 2 * 60 * 1000);
        }
    }

    @Override
    public void onGeoData(String uuid, GeoData data) {
        DataBundle dataBundle = data.toDataBundle();
        dataBundle.putString("uuid", uuid);
        send(ProxyLocationManager.RESPONSE_LOC_DATA, dataBundle, -1);
    }
}

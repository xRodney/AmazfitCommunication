package com.kieronquinn.library.amazfitcommunication.location;

import android.content.Context;
import android.location.LocationListener;
import android.util.Log;

import com.huami.watch.transport.TransportDataItem;
import com.kieronquinn.library.amazfitcommunication.common.CommonTransporterItem;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Kieron on 09/04/2018.
 */

public class ProxyLocationManager extends CommonTransporterItem {


    public static final String REQUEST_LOC_SUBSCRIBE = "com.huami.watch.companion.transport.amazfitcommunication.REQUEST_LOC_SUBSCRIBE";
    public static final String REQUEST_LOC_MORE = "com.huami.watch.companion.transport.amazfitcommunication.REQUEST_LOC_MORE";
    public static final String REQUEST_LOC_UNSUBSCRIBE = "com.huami.watch.companion.transport.amazfitcommunication.REQUEST_LOC_UNSUBSCRIBE";

    public static final String RESPONSE_LOC_ACK = "com.huami.watch.companion.transport.amazfitcommunication.RESPONSE_LOC_ACK";
    public static final String RESPONSE_LOC_DATA = "com.huami.watch.companion.transport.amazfitcommunication.RESPONSE_LOC_DATA";
    public static final String RESPONSE_LOC_RESET = "com.huami.watch.companion.transport.amazfitcommunication.RESPONSE_LOC_RESET";

    private final Map<String, LocationListener> locationListeners = new HashMap<>();
    private final Map<LocationListener, String> locationListenersInv = new HashMap<>();

    private static final String TAG = ProxyLocationManager.class.getSimpleName();

    public ProxyLocationManager(final Context context, Runnable disconnectCallback) {
        super(context, CHANNEL_ID, disconnectCallback);
    }

    public void requestLocationUpdates(String provider, long minTime, float minDistance, LocationListener locationListener) {
        GeoSubscription subscription = new GeoSubscription(provider, minTime, minDistance);
        String uuid = subscription.getUuid().toString();

        queueAndSend(REQUEST_LOC_SUBSCRIBE, subscription.toDataBundle(), 5000);
        locationListeners.put(uuid, locationListener);
        locationListenersInv.put(locationListener, uuid);
    }

    public void removeUpdates(LocationListener locationListener) {
        String uuid = locationListenersInv.get(locationListener);
        if (uuid != null) {
            queueAndSend(REQUEST_LOC_UNSUBSCRIBE, uuidBundle(uuid), -1);
            disconnect(uuid);
        }
    }

    @Override
    public void onDataReceived(TransportDataItem item) {

        String action = item.getAction();
        Log.d(TAG, "Received " + action);
        String uuid = item.getData().getString("uuid");

        switch (action) {
            case RESPONSE_LOC_ACK:
                unregisterTimeout(uuid);
                break;

            case RESPONSE_LOC_DATA:
                unregisterTimeout(uuid);
                LocationListener locationListener = locationListeners.get(uuid);
                if (locationListener != null) {
                    GeoData data = GeoData.fromDataBundle(item.getData());
                    data.applyTo(locationListener);
                }
                queueAndSend(REQUEST_LOC_MORE, uuidBundle(uuid), -1);
                break;

            case RESPONSE_LOC_RESET:
                disconnect(uuid);
                break;
        }
    }

    @Override
    protected void disconnect(String uuid) {
        super.disconnect(uuid);
        LocationListener removed = locationListeners.remove(uuid);
        if (removed != null) {
            locationListenersInv.remove(removed);
            if (locationListeners.isEmpty()) {
                disconnectFromTransporter();
            }
        }
    }
}

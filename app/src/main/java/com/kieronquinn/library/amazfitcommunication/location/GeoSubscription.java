package com.kieronquinn.library.amazfitcommunication.location;

import com.huami.watch.transport.DataBundle;

import java.util.UUID;

public class GeoSubscription {
    private final String provider;
    private final long minTime;
    private final float minDistance;
    private final UUID uuid;

    GeoSubscription(String provider, long minTime, float minDistance) {
        this(provider, minTime, minDistance, UUID.randomUUID());
    }

    private GeoSubscription(String provider, long minTime, float minDistance, UUID uuid) {
        this.provider = provider;
        this.minTime = minTime;
        this.minDistance = minDistance;
        this.uuid = uuid;
    }

    public DataBundle toDataBundle() {
        DataBundle dataBundle = new DataBundle();
        dataBundle.putString("provider", provider);
        dataBundle.putLong("minTime", minTime);
        dataBundle.putFloat("minDistance", minDistance);
        dataBundle.putString("uuid", uuid.toString());
        return dataBundle;
    }

    public static GeoSubscription fromDataBundle(DataBundle dataBundle) {
        return new GeoSubscription(
                dataBundle.getString("provider"),
                dataBundle.getLong("minTime"),
                dataBundle.getFloat("minDistance"),
                UUID.fromString(dataBundle.getString("uuid")));
    }

    public String getProvider() {
        return provider;
    }

    public long getMinTime() {
        return minTime;
    }

    public float getMinDistance() {
        return minDistance;
    }

    public UUID getUuid() {
        return uuid;
    }
}

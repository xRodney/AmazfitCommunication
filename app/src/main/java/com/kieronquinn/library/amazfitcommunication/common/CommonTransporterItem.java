package com.kieronquinn.library.amazfitcommunication.common;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.huami.watch.transport.DataBundle;
import com.kieronquinn.library.amazfitcommunication.Transporter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class CommonTransporterItem implements Transporter.DataListener, Transporter.ChannelListener {
    public static final String CHANNEL_ID = "com.kieronquinn.app.amazfitinternetcompanion";

    private final Transporter transporter;
    private final List<ActionAndBundle> outputQueue = new ArrayList<>();
    private final Map<String, Runnable> timers = new HashMap<>();
    private Runnable disconnectCallback;
    private boolean channelAvailable;
    private Handler timeoutHandler = new Handler();

    private static final String TAG = CommonTransporterItem.class.getSimpleName();

    public CommonTransporterItem(Transporter transporter, Runnable disconnectCallback) {
        this.transporter = transporter;
        this.disconnectCallback = disconnectCallback;
    }

    public CommonTransporterItem(Context context, String channelId, Runnable disconnectCallback) {
        this.disconnectCallback = disconnectCallback;
        transporter = Transporter.get(context, channelId);
        transporter.addDataListener(this);
        transporter.addChannelListener(this);
        transporter.connectTransportService();
    }

    protected void queueAndSend(String action, DataBundle bundle, int timeout) {
        synchronized (outputQueue) {
            if (channelAvailable) {
                send(action, bundle, timeout);
            } else {
                Log.d(TAG, "Queuing " + action);
                outputQueue.add(new ActionAndBundle(action, bundle, timeout));
            }
        }
    }

    /**
     * Send request
     */
    protected void send(String action, DataBundle dataBundle, int timeout) {
        final String uuid = dataBundle.getString("uuid");

        if (timeout > 0) {
            Runnable timer = new Runnable() {
                @Override
                public void run() {
                    disconnect(uuid);
                }
            };
            registerTimeout(uuid, timeout, timer);
        }
        Log.d(TAG, "Sending " + action);
        transporter.send(action, dataBundle);
    }

    protected void disconnect(String uuid) {
        if (this.disconnectCallback != null) {
            this.disconnectCallback.run();
        }
        unregisterTimeout(uuid);
    }

    protected void disconnectFromTransporter() {
        transporter.removeDataListener(this);
        transporter.removeChannelListener(this);
        transporter.disconnectTransportService();
    }

    @Override
    public void onChannelChanged(boolean isAvailable) {
        synchronized (outputQueue) {
            if (!this.channelAvailable && isAvailable) {
                for (ActionAndBundle actionAndBundle : outputQueue) {
                    send(actionAndBundle.action, actionAndBundle.dataBundle, actionAndBundle.timeout);
                }
                outputQueue.clear();
            }
            this.channelAvailable = isAvailable;
        }
    }

    protected void registerTimeout(final String uuid, int delayMillis, final Runnable action) {
        unregisterTimeout(uuid);   // remove an old timeout for the same conversation
        Runnable action1 = new Runnable() {
            @Override
            public void run() {
                timers.remove(uuid);
                action.run();
            }
        };
        timeoutHandler.postDelayed(action1, delayMillis);
        timers.put(uuid, action);
    }

    protected void unregisterTimeout(String uuid) {
        Runnable action = timers.remove(uuid);
        if (action != null) {
            timeoutHandler.removeCallbacks(action);
        }
    }

    protected DataBundle uuidBundle(String uuid) {
        DataBundle dataBundle = new DataBundle();
        dataBundle.putString("uuid", uuid);
        return dataBundle;
    }


    private static class ActionAndBundle {
        final String action;
        final DataBundle dataBundle;
        final int timeout;

        ActionAndBundle(String action, DataBundle dataBundle, int timeout) {
            this.action = action;
            this.dataBundle = dataBundle;
            this.timeout = timeout;
        }
    }
}

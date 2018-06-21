package com.kieronquinn.library.amazfitcommunication.internet;

import android.util.Log;

import com.huami.watch.transport.DataBundle;
import com.huami.watch.transport.TransportDataItem;
import com.kieronquinn.library.amazfitcommunication.Transporter;
import com.kieronquinn.library.amazfitcommunication.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class HTTPRequestResponder implements Transporter.DataListener {

    private final Transporter transporter;

    public HTTPRequestResponder(Transporter transporter) {
        this.transporter = transporter;
    }

    @Override
    public void onDataReceived(TransportDataItem item) {
        Log.d("AmazfitCompanion", "onDataReceived");
        if (item.getAction().equals("com.huami.watch.companion.transport.amazfitcommunication.HTTP_REQUEST")) {
            //Never try if it's a watch (someone made an error)
            if (Utils.isWatch()) return;
            //Send pingback immediately to let the app know it's being handled
            transporter.send("com.huami.watch.companion.transport.amazfitcommunication.HTTP_PINGBACK", item.getData());
            //Get data
            DataBundle dataBundle = item.getData();
            try {
                HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(dataBundle.getString("url")).openConnection();
                httpURLConnection.setInstanceFollowRedirects(dataBundle.getBoolean("followRedirects"));
                httpURLConnection.setRequestMethod(dataBundle.getString("requestMethod"));
                httpURLConnection.setUseCaches(dataBundle.getBoolean("useCaches"));
                httpURLConnection.setDoInput(dataBundle.getBoolean("doInput"));
                httpURLConnection.setDoOutput(dataBundle.getBoolean("doOutput"));
                try {
                    JSONArray headers = new JSONArray(dataBundle.getString("requestHeaders"));
                    for (int x = 0; x < headers.length(); x++) {
                        JSONObject header = headers.getJSONObject(x);
                        httpURLConnection.setRequestProperty(header.getString("key"), header.getString("value"));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                httpURLConnection.connect();
                if (httpURLConnection.getInputStream() != null) {
                    byte[] inputStream = toByteArray(httpURLConnection.getInputStream());
                    if (inputStream != null)
                        dataBundle.putByteArray("inputStream", inputStream);
                }
                if (httpURLConnection.getErrorStream() != null) {
                    byte[] errorStream = toByteArray(httpURLConnection.getErrorStream());
                    if (errorStream != null)
                        dataBundle.putByteArray("errorStream", errorStream);
                }
                dataBundle.putString("responseMessage", httpURLConnection.getResponseMessage());
                dataBundle.putInt("responseCode", httpURLConnection.getResponseCode());
                dataBundle.putString("responseHeaders", mapToJSON(httpURLConnection.getHeaderFields()).toString());
                //Return the data
                transporter.send("com.huami.watch.companion.transport.amazfitcommunication.HTTP_RESULT", dataBundle);
                httpURLConnection.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }



    private JSONArray mapToJSON(Map<String, List<String>> input) {
        JSONArray headers = new JSONArray();
        for (String key : input.keySet()) {
            JSONObject item = new JSONObject();
            try {
                item.put("key", key);
                List<String> items = input.get(key);
                JSONArray itemsArray = new JSONArray();
                for (String itemValue : items) {
                    itemsArray.put(itemValue);
                }
                item.put("value", itemsArray);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            headers.put(item);
        }
        return headers;
    }

    private static byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int n;
        byte[] buffer = new byte[1024 * 4];
        while ((n = input.read(buffer)) >= 0) {
            output.write(buffer, 0, n);
        }
        return output.toByteArray();
    }
}

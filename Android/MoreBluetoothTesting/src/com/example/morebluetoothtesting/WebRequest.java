package com.example.morebluetoothtesting;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.util.Log;

class WebRequest extends AsyncTask<String, Void, String> {

    protected String doInBackground(String... request) {
        DefaultHttpClient httpClient = new DefaultHttpClient(new BasicHttpParams());
        HttpPost httpPost = null;
        InputStream inputStream = null;
        String result = null;

        try {
            httpPost = new HttpPost(request[0]);

            JSONObject postData = new JSONObject();
            for (int i=1; i<request.length; i+=2)
                postData.put(request[i], request[i+1]);

            StringEntity postString = new StringEntity(postData.toString());
            postString.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));

            httpPost.setEntity(postString);

            HttpResponse response = httpClient.execute(httpPost);
            HttpEntity entity = response.getEntity();

            inputStream = entity.getContent();

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);
            StringBuilder builder = new StringBuilder();

            String line = null;
            while ((line = reader.readLine()) != null) {
                builder.append(line + "\n");
            }
            result = builder.toString();
        } catch (Exception e) { e.printStackTrace(); }

        try {
            inputStream.close();
        } catch (Exception e) { Log.e("Exception!", e.toString()); }

        return result;
    }

    protected void onPostExecute(String result) { }
}
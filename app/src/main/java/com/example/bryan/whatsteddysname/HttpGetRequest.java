package com.example.bryan.whatsteddysname;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by john_le on 4/20/18.
 */

public class HttpGetRequest extends AsyncTask<String, Void, String> {
    public static final String REQUEST_METHOD = "GET";

    @Override
    protected String doInBackground(String... params){
        String stringUrl = params[0];
        String result;
        String inputLine;

        try {
            // Create a URL object holding our url
            URL myUrl = new URL(stringUrl);

            // Create connection
            HttpURLConnection connection = (HttpURLConnection) myUrl.openConnection();

            // Set method
            connection.setRequestMethod(REQUEST_METHOD);

            connection.connect();

            //Create a new InputStreamReader
            InputStreamReader streamReader = new
                    InputStreamReader(connection.getInputStream());

            //Create a new buffered reader and String Builder
            BufferedReader reader = new BufferedReader(streamReader);
            StringBuilder stringBuilder = new StringBuilder();

            //Check if the line we are reading is not null
            while((inputLine = reader.readLine()) != null){
                stringBuilder.append(inputLine);
            }

            //Close our InputStream and Buffered reader
            reader.close();
            streamReader.close();

            //Set our result equal to our stringBuilder
            result = stringBuilder.toString();
        } catch(IOException i) {
            Log.d("URLFAILURE", i.getMessage());
            result = null;
        }

        return result;
    }

    @Override
    protected void onPostExecute(String result){
        super.onPostExecute(result);
    }
}

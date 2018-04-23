package com.example.bryan.whatsteddysname;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by john_le on 4/20/18.
 */

public class HttpGetRequest extends AsyncTask<String, Void, String> {
    public static final String REQUEST_METHOD = "GET";
    private Context context;
    private Context packageContext;
    private List<String> items;
    private ProgressDialog progressDialog;

    public HttpGetRequest(Context context,
                          Context packageContext,
                          ProgressDialog progressDialog,
                          List<String> items) {
        this.context = context;
        this.packageContext = packageContext;
        this.items = items;
        this.progressDialog = progressDialog;
    }
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
    protected void onPostExecute(final String result){
        super.onPostExecute(result);
        if(result != null
            && !result.equals("{\"message\": \"Endpoint request timed out\"}")
            &&  !result.contains("stackTrace"))  {
            String actualResult = result.replace("\"", "");
            List<String> resultList = new ArrayList<String>();

            for(String json : items) {
                try {
                    JSONObject obj = new JSONObject(json);
                    if(obj.getString("s3Location").equals(actualResult)) {
                        resultList.add(json);
                    }
                } catch(JSONException j) {
                    Log.d("JSONEXCEPTION", j.getMessage());
                }
            }

            progressDialog.cancel();

            Intent intent = new Intent(packageContext, SearchResultsActivity.class);

            intent.putStringArrayListExtra("results", (ArrayList<String>) resultList);

            context.startActivity(intent);
        } else {
            progressDialog.cancel();
            Toast.makeText(context, "Search Failed", Toast.LENGTH_LONG).show();
        }

    }
}

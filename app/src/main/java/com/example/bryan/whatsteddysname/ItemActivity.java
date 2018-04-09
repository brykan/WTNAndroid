package com.example.bryan.whatsteddysname;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.AWSStartupHandler;
import com.amazonaws.mobile.client.AWSStartupResult;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3Client;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class ItemActivity extends AppCompatActivity {
    private JSONObject item;
    private Button deleteItemBtn;
    private Button editNameBtn;
    private Button editDesBtn;
    private EditText nameField;
    private EditText desField;
    private ImageView imgField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item);

        nameField = (EditText) findViewById(R.id.view_item_name_field);
        desField = (EditText) findViewById(R.id.view_item_des_field);
        imgField = (ImageView) findViewById(R.id.view_item_img);

        try {
            item = new JSONObject(getIntent().getStringExtra("ITEM"));

            nameField.setText(item.getString("itemName"), TextView.BufferType.EDITABLE);
            desField.setText(item.getString("itemDescription"), TextView.BufferType.EDITABLE);

            File imgFile = new File(item.getString("localPhotoPath"));

            if(imgFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                BitmapDrawable obj = new BitmapDrawable(getResources(), bitmap);

                imgField.setBackground(obj);
            } else {
                downloadImage(item, imgField);
            }
        } catch(JSONException e) {
            Log.d("JSONEXCEPTION", e.getMessage());
        }
    }

    public void downloadImage(final JSONObject item, final ImageView itemImg) {
        AWSMobileClient.getInstance().initialize(this, new AWSStartupHandler() {
            @Override
            public void onComplete(AWSStartupResult awsStartupResult) {
                downloadWithTransferUtility(item, itemImg);
            }
        }).execute();
    }

    public void downloadWithTransferUtility(final JSONObject item, final ImageView itemImg) {

        TransferUtility transferUtility =
                TransferUtility.builder()
                        .context(getApplicationContext())
                        .awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
                        .s3Client(new AmazonS3Client(AWSMobileClient.getInstance().getCredentialsProvider()))
                        .build();
        try {
            TransferObserver downloadObserver =
                    transferUtility.download(
                            item.getString("s3Location"),
                            new File(item.getString("localPhotoPath")));

            // Attach a listener to the observer to get state update and progress notifications
            downloadObserver.setTransferListener(new TransferListener() {
                @Override
                public void onStateChanged(int id, TransferState state) {
                    if (TransferState.COMPLETED == state) {
                        // Handle a completed upload.
                        try {
                            File imgFile = new File(item.getString("localPhotoPath"));
                            Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                            BitmapDrawable obj = new BitmapDrawable(getResources(), bitmap);

                            itemImg.setBackground(obj);
                        } catch(JSONException e) {
                            Log.d("JSONEXCEPTION", e.getMessage());
                        }
                    }
                }

                @Override
                public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                    float percentDonef = ((float) bytesCurrent / (float) bytesTotal) * 100;
                    int percentDone = (int) percentDonef;

                    Log.d("ItemList", "   ID:" + id + "   bytesCurrent: " + bytesCurrent + "   bytesTotal: " + bytesTotal + " " + percentDone + "%");
                }

                @Override
                public void onError(int id, Exception ex) {
                    Log.d("ItemList #" + id, ex.getMessage());
                }

            });
        } catch (JSONException e) {
            Log.d("JSONEXCEPTION", e.getMessage());
        }
    }
}

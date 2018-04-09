package com.example.bryan.whatsteddysname;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
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
    static final int RESULT_DELETE_ITEM = 4;
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

        imgField = (ImageView) findViewById(R.id.view_item_img);

        nameField = (EditText) findViewById(R.id.view_item_name_field);
        nameField.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if(i == KeyEvent.KEYCODE_ENTER) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(nameField.getWindowToken(), 0);

                    nameField.setFocusable(false);
                    nameField.setFocusableInTouchMode(false);
                    return true;
                } else {
                    return false;
                }
            }
        });

        desField = (EditText) findViewById(R.id.view_item_des_field);
        desField.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if(i == KeyEvent.KEYCODE_BACK) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(desField.getWindowToken(), 0);

                    desField.setFocusable(false);
                    desField.setFocusableInTouchMode(false);
                    return true;
                } else {
                    return false;
                }
            }
        });

        editNameBtn = (Button) findViewById(R.id.edit_name_button);
        editNameBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nameField.setFocusable(true);
                nameField.setFocusableInTouchMode(true);
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

                nameField.requestFocus();
                nameField.setSelection(nameField.getText().length());
                imm.showSoftInput(nameField, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        editDesBtn = (Button) findViewById(R.id.edit_des_button);
        editDesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                desField.setFocusable(true);
                desField.setFocusableInTouchMode(true);
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

                desField.requestFocus();
                desField.setSelection(desField.getText().length());
                imm.showSoftInput(desField, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        final DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        //Yes button clicked
                        Intent output = new Intent();

                        output.putExtra("itemIndex", getIntent().getIntExtra("ITEMINDEX", -1));
                        setResult(RESULT_DELETE_ITEM, output);
                        dialog.dismiss();
                        finish();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        dialog.dismiss();
                        break;
                }
            }
        };

        deleteItemBtn = (Button) findViewById(R.id.delete_item_btn);
        deleteItemBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                builder.setMessage("Are you sure you want to delete?")
                        .setPositiveButton("Yes", dialogClickListener)
                        .setNegativeButton("No", dialogClickListener)
                        .show();

            }
        });

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

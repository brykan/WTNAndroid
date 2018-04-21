package com.example.bryan.whatsteddysname;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.TextInputEditText;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.AWSStartupHandler;
import com.amazonaws.mobile.client.AWSStartupResult;
import com.amazonaws.services.s3.AmazonS3Client;
import com.example.bryan.whatsteddysname.aws.AWSLoginModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.amazonaws.mobileconnectors.s3.transferutility.*;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;


public class AddItemActivity extends AppCompatActivity {
    private ImageButton addImgBtn;
    private Button addItemBtn;
    private TextInputEditText addItemName;
    private TextInputEditText addItemDes;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    private String currentPhotoPath;
    private String currentGrayPhotoPath;
    private JSONObject item;
    private ProgressDialog progressDialog;
    private String itemTimeStamp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item);

        addImgBtn = (ImageButton) findViewById(R.id.addImg);
        addImgBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });

        addItemBtn = (Button) findViewById(R.id.add_item_btn);
        addItemBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addItem();
            }
        });

        addItemName = (TextInputEditText) findViewById(R.id.add_item_name);
        addItemDes = (TextInputEditText) findViewById(R.id.add_item_des);
        Drawable draw = addItemBtn.getBackground();

        item = new JSONObject();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            try {
                Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), Uri.parse("file://" + currentPhotoPath));
                BitmapDrawable obj = new BitmapDrawable(getResources(), imageBitmap);
                addImgBtn.setBackground(obj);
                addImgBtn.setImageResource(android.R.color.transparent);
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo will go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException e) {
                Log.d("IOEXCEPTION", e.getMessage());
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(
                        this,
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create image file name
        Intent intent = getIntent();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        itemTimeStamp = timeStamp;
        String imageFileName =  intent.getStringExtra("USER_ID")+ "_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir);
        currentPhotoPath = image.getAbsolutePath();
        try {
            item.put("localPhotoPath", currentPhotoPath);
        } catch (JSONException e) {
            Log.d("JSONEXCEPTION", e.getMessage());
        }
        return image;
    }

    private File createGrayImage() throws IOException {
        // Create image file name
        Intent intent = getIntent();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        itemTimeStamp = timeStamp;
        String imageFileName =  "gray_" + intent.getStringExtra("USER_ID")+ "_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir);
        currentGrayPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void addItem() {
        progressDialog = new ProgressDialog(AddItemActivity.this,
                R.style.Theme_AppCompat_DayNight_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Adding Item...");
        progressDialog.show();

        addGrayScaleImage(item);

        AWSMobileClient.getInstance().initialize(this, new AWSStartupHandler() {
           @Override
            public void onComplete(AWSStartupResult awsStartupResult) {
               uploadWithTransferUtility();
           }
        }).execute();
    }

    public void uploadWithTransferUtility() {
        String itemName = addItemName.getText().toString();
        String itemDes = addItemDes.getText().toString();
        Boolean valid = true;

        if (addImgBtn.getDrawable().getConstantState()
                .equals(getResources().getDrawable(android.R.drawable.ic_menu_camera).getConstantState())) {
            Toast.makeText(getBaseContext(),  "Must select an image.", Toast.LENGTH_LONG).show();
            valid = false;
        }

        if (itemName.isEmpty()) {
            addItemName.setError("Name must not be empty!");
            valid = false;
        }

        if(!valid) {
            progressDialog.cancel();
            return;
        }

        String fileLocation = "public/" + getIntent().getStringExtra("USER_ID") + "/" + itemTimeStamp + ".jpg";
        String grayFileLocation = "public/" + getIntent().getStringExtra("USER_ID") + "/gray_" + itemTimeStamp + ".jpg";


        try {
            item.put("s3Location", fileLocation);
            item.put("itemName", itemName);
            item.put("itemDescription", itemDes);
        } catch (JSONException e) {
            Log.d("JSONEXCEPTION", e.getMessage());
        }

        uploadPhoto(currentPhotoPath, fileLocation, false);
        uploadPhoto(currentGrayPhotoPath, grayFileLocation, true);
    }

    public void uploadPhoto(final String path, String fileLocation, final Boolean end) {
        TransferUtility transferUtility =
                TransferUtility.builder()
                        .context(getApplicationContext())
                        .awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
                        .s3Client(new AmazonS3Client(AWSMobileClient.getInstance().getCredentialsProvider()))
                        .build();

        TransferObserver uploadObserver =
                transferUtility.upload(
                        fileLocation,
                        new File(path));

        uploadObserver.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                if (TransferState.COMPLETED == state) {
                    // Handle complete upload
                    if(end) {
                        progressDialog.cancel();
                        Intent output = new Intent();

                        output.putExtra("itemResult", item.toString());
                        setResult(RESULT_OK, output);
                        finish();
                    }
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                float percentDonef = ((float) bytesCurrent / (float) bytesTotal) * 100;
                int percentDone = (int) percentDonef;

                Log.d("UPLOADINGTOS3", "bytesCurrent: " + bytesCurrent + " bytesTotal: " + bytesTotal + " " + percentDone + "%");
            }

            @Override
            public void onError(int id, Exception ex) {
                Log.d("UPLOADERROR", ex.getMessage());
            }
        });
    }

    public void addGrayScaleImage(JSONObject item) {
        // constant factors
        final double GS_RED = 0.299;
        final double GS_GREEN = 0.587;
        final double GS_BLUE = 0.114;
        // pixel information
        int A, R, G, B;
        int pixel;

        try {
            File input = new File(item.getString("localPhotoPath"));

            if(input.exists()) {
                Bitmap src = BitmapFactory.decodeFile(input.getAbsolutePath());
                Bitmap out = Bitmap.createBitmap(src.getWidth(), src.getHeight(), src.getConfig());
                // get image size
                int width = src.getWidth();
                int height = src.getHeight();

                // scan through every single pixel
                for(int x = 0; x < width; ++x) {
                    for(int y = 0; y < height; ++y) {
                        // get one pixel color
                        pixel = src.getPixel(x, y);
                        // retrieve color of all channels
                        A = Color.alpha(pixel);
                        R = Color.red(pixel);
                        G = Color.green(pixel);
                        B = Color.blue(pixel);
                        // take conversion up to one single value
                        R = G = B = (int)(GS_RED * R + GS_GREEN * G + GS_BLUE * B);
                        // set new pixel color to output bitmap
                        out.setPixel(x, y, Color.argb(A, R, G, B));
                    }
                }

                File outFile = null;
                try {
                    outFile = createGrayImage();

                    if(outFile != null) {
                        FileOutputStream outStream = new FileOutputStream(outFile);
                        out.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
                        outStream.flush();
                        outStream.close();
                    }
                } catch (IOException e) {
                    Log.d("IOEXCEPTION", e.getMessage());
                }
            }
        } catch(JSONException e) {
            Log.d("JSONEXCEPTION", e.getMessage());
        }
    }
}

package com.example.bryan.whatsteddysname.activities;

import android.content.Intent;
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

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.bryan.whatsteddysname.asynctasks.AddItemRequest;
import com.example.bryan.whatsteddysname.R;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;


public class AddItemActivity extends AppCompatActivity {
    private ImageButton addImgBtn;
    private Button addItemBtn;
    private TextInputEditText addItemName;
    private TextInputEditText addItemDes;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    private String currentPhotoPath;
    private JSONObject item;
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

        item = new JSONObject();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
                File imgFile = new File(currentPhotoPath);
                RequestOptions options = new RequestOptions()
                        .placeholder(R.drawable.placeholder)
                        .centerInside();

                addImgBtn.setBackgroundResource(0);
                Glide.with(this).load(imgFile).apply(options).into(addImgBtn);
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

    private void addItem() {
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
            return;
        }

        AddItemRequest request =
                new AddItemRequest(
                        AddItemActivity.this,
                        item,
                        itemName,
                        itemDes,
                        getIntent().getStringExtra("USER_ID"),
                        itemTimeStamp,
                        currentPhotoPath,
                        getApplicationContext());

        request.execute();
    }
}

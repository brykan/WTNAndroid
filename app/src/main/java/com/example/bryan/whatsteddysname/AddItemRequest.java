package com.example.bryan.whatsteddysname;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;

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
import java.io.FileOutputStream;
import java.io.IOException;

import static android.app.Activity.RESULT_OK;

/**
 * Created by john_le on 4/21/18.
 */

public class AddItemRequest extends AsyncTask<Void, Void, Void> {
    private ProgressDialog progressDialog;
    private JSONObject item;
    private String itemName;
    private String itemDes;
    private Context context;
    private Context appContext;
    private String user_id;
    private String timestamp;
    private String photoPath;
    private String grayPhotoPath;
    private Boolean finish = false;

    AddItemRequest(
            Context context,
            JSONObject item,
            String itemName,
            String itemDes,
            String user_id,
            String timestamp,
            String photoPath,
            Context appContext) {
        this.item = item;
        this.itemName = itemName;
        this.itemDes = itemDes;
        this.context = context;
        this.user_id = user_id;
        this.timestamp = timestamp;
        this.photoPath = photoPath;
        this.appContext = appContext;
    }

    @Override
    protected void onPreExecute() {
    //Show progress Dialog here
        progressDialog = new ProgressDialog(this.context,
                R.style.Theme_AppCompat_DayNight_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Adding Item...");
        progressDialog.show();
    }

    @Override
    protected void onPostExecute(Void result) {
        //Update UI here if needed
        //Dismiss Progress Dialog here
        super.onPostExecute(result);

        Intent output = new Intent();

        Log.d("ITEMRESULT", item.toString());
        output.putExtra("itemResult", item.toString());
        ((Activity) context).setResult(RESULT_OK, output);
        progressDialog.cancel();
        ((Activity) context).finish();
    }

    @Override
    protected Void doInBackground(Void ...params) {
        //Do you heavy task here
        final String fileLocation = "public/" + user_id + "/" + timestamp + ".jpg";
        final String grayFileLocation = "public/" + user_id + "/gray_" + timestamp + ".jpg";

        addGrayScaleImage(item);

        try {
            item.put("s3Location", fileLocation);
            item.put("itemName", itemName);
            item.put("itemDescription", itemDes);
        } catch (JSONException e) {
            Log.d("JSONEXCEPTION", e.getMessage());
        }

        AWSMobileClient.getInstance().initialize(context, new AWSStartupHandler() {
            @Override
            public void onComplete(AWSStartupResult awsStartupResult) {
                uploadPhoto(photoPath, fileLocation, false);
                uploadPhoto(grayPhotoPath, grayFileLocation, true);
            }
        }).execute();

        while(!finish) {}
        return null;
    }

    public void uploadPhoto(final String path, String fileLocation, final Boolean end) {
        TransferUtility transferUtility =
                TransferUtility.builder()
                        .context(appContext)
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
                        finish = true;
                    }
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                float percentDonef = ((float) bytesCurrent / (float) bytesTotal) * 100;
                int percentDone = (int) percentDonef;

                Log.d("UPLOADINGTOS3", "bytesCurrent: " + bytesCurrent +
                        " bytesTotal: " + bytesTotal + " " + percentDone + "%");
            }

            @Override
            public void onError(int id, Exception ex) {
                Log.d("UPLOADERROR", ex.getMessage());
            }
        });
    }

    public void addGrayScaleImage(JSONObject item) {
        try {
            File input = new File(item.getString("localPhotoPath"));

            if(input.exists()) {
                Bitmap src = BitmapFactory.decodeFile(input.getPath());
                Bitmap out = Bitmap.createBitmap(src.getWidth(), src.getHeight(), src.getConfig());
                out = rotateImage(out, 180);

                Canvas canvas = new Canvas(out);
                Paint paint = new Paint();
                ColorMatrix colorMatrix = new ColorMatrix();

                colorMatrix.setSaturation(0); //value of 0 maps the color to gray-scale
                ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
                paint.setColorFilter(filter);
                canvas.drawBitmap(src, 0, 0, paint);

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

    private File createGrayImage() throws IOException {
        // Create image file name

        String imageFileName =  "gray_" + user_id + "_" + timestamp + "_";
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir);
        grayPhotoPath = image.getAbsolutePath();

        return image;
    }

    public Bitmap rotateImageIfRequired(Bitmap img) {
        Uri uri = Uri.parse("file://" + grayPhotoPath);
        if (uri.getScheme().equals("content")) {
            String[] projection = {MediaStore.Images.ImageColumns.ORIENTATION};
            Cursor c = context.getContentResolver().query(uri, projection, null, null, null);
            if (c.moveToFirst()) {
                final int rotation = c.getInt(0);
                c.close();
                return rotateImage(img, rotation);
            }
            return img;
        } else {
            try {
                ExifInterface ei = new ExifInterface(uri.getPath());
                int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        return rotateImage(img, 90);
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        return rotateImage(img, 180);
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        return rotateImage(img, 270);
                    default:
                        return img;
                }
            } catch (IOException e) {
                Log.d("EXIFERROR", e.getMessage());
            }
            return img;
        }
    }

    public Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        return rotatedImg;
    }
}

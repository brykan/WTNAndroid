package com.example.bryan.whatsteddysname;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by john_le on 4/22/18.
 */

public class InitiateMLSearchTask extends AsyncTask<Void, Void, String> {
    private Context context;
    private String inputPath;
    private String userId;
    private String resultPath;
    private Context applicationContext;
    private List<String> itemList;
    private Context packageContext;
    private ProgressDialog progressDialog;

    public InitiateMLSearchTask(
            Context context,
            Context applicationContext,
            Context packageContext,
            ProgressDialog progressDialog,
            String inputPath,
            String userId,
            List<String> itemList) {
        this.context = context;
        this.applicationContext = applicationContext;
        this.packageContext = packageContext;
        this.inputPath = inputPath;
        this.userId = userId;
        this.itemList = itemList;
        this.progressDialog = progressDialog;
    }

    @Override
    protected void onPostExecute(final String s) {
        super.onPostExecute(s);

        AWSMobileClient.getInstance().initialize(context, new AWSStartupHandler() {
            @Override
            public void onComplete(AWSStartupResult awsStartupResult) {
                uploadWithTransferUtility(s);
            }
        }).execute();
    }

    @Override
    protected String doInBackground(Void... voids) {
        File input = new File(inputPath);

        if(input.exists()) {
            WeakReference<Bitmap> src = new WeakReference<Bitmap>(BitmapFactory.decodeFile(input.getPath()));
            src = rotateImageIfRequired(src);
            WeakReference<Bitmap> out =
                    new WeakReference<Bitmap>(Bitmap.createBitmap(src.get().getWidth(), src.get().getHeight(), src.get().getConfig()));

            Canvas canvas = new Canvas(out.get());
            Paint paint = new Paint();
            ColorMatrix colorMatrix = new ColorMatrix();

            colorMatrix.setSaturation(0); //value of 0 maps the color to gray-scale
            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
            paint.setColorFilter(filter);
            canvas.drawBitmap(src.get(), 0, 0, paint);

            File outFile = null;
            try {
                outFile = createSearchImage();

                if(outFile != null) {
                    FileOutputStream outStream = new FileOutputStream(outFile);
                    out.get().compress(Bitmap.CompressFormat.JPEG, 0, outStream);
                    outStream.flush();
                    outStream.close();

                    if(input.delete()) {
                        return resultPath;
                    }
                }
            } catch (IOException e) {
                Log.d("IOEXCEPTION", e.getMessage());
            }
        }
        return resultPath;
    }

    public File createSearchImage() throws IOException {
        // Create image file name
        String imageFileName = userId + "_";
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir);
        resultPath = image.getAbsolutePath();

        return image;
    }

    public WeakReference<Bitmap> rotateImageIfRequired(WeakReference<Bitmap> img) {
        Uri uri = Uri.fromFile(new File(inputPath));
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

    public WeakReference<Bitmap> rotateImage(WeakReference<Bitmap> img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        return new WeakReference<Bitmap>(Bitmap.createBitmap(img.get(), 0, 0, img.get().getWidth(), img.get().getHeight(), matrix, true));
    }

    public void uploadWithTransferUtility(final String actualSearchPhotoPath) {
        String fileLocation = "public/search-images/" + userId + ".jpg";

        TransferUtility transferUtility =
                TransferUtility.builder()
                        .context(applicationContext)
                        .awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
                        .s3Client(new AmazonS3Client(AWSMobileClient.getInstance().getCredentialsProvider()))
                        .build();

        TransferObserver uploadObserver =
                transferUtility.upload(
                        fileLocation,
                        new File(actualSearchPhotoPath));

        uploadObserver.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                if (TransferState.COMPLETED == state) {
                    // Handle complete upload
                    File photo = new File(actualSearchPhotoPath);

                    if(photo.delete()) {
                        beginSearch();
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

    public void beginSearch() {
        String mlUrl = "https://mv33lyux8f.execute-api.us-east-1.amazonaws.com/prod/classify?user="
                + userId;
        new HttpGetRequest(context, packageContext, progressDialog, itemList).execute(mlUrl);
    }
}

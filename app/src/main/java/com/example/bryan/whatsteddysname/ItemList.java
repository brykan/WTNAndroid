package com.example.bryan.whatsteddysname;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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
import java.util.List;

public class ItemList extends ArrayAdapter<String> {
    private final Activity context;
    private final List<String> items;

    public ItemList(Activity context, List<String> items) {
        super(context, R.layout.single_item, items);
        this.context = context;
        this.items = items;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View rowView = inflater.inflate(R.layout.single_item, null, true);
        TextView itemName = (TextView) rowView.findViewById(R.id.item_name);
        final ImageView itemImg = (ImageView) rowView.findViewById(R.id.item_img);

        try {
            final JSONObject item = new JSONObject(items.get(position));
            String name = item.getString("itemName");

            itemName.setText(name);

           File imgFile = new File(item.getString("localPhotoPath"));

            if(imgFile.exists()) {

                Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());

                itemImg.setImageBitmap(bitmap);
            } else {
                dowloadData(item, itemImg);
            }
        } catch(JSONException e) {
            Log.d("JSONEXCEPTION", e.getMessage());
        }

        return rowView;
    }

    public void dowloadData(final JSONObject item, final ImageView itemImg) {
        AWSMobileClient.getInstance().initialize(this.context, new AWSStartupHandler() {
            @Override
            public void onComplete(AWSStartupResult awsStartupResult) {
                downloadWithTransferUtility(item, itemImg);
            }
        }).execute();
    }

    public void downloadWithTransferUtility(final JSONObject item, final ImageView itemImg) {

        TransferUtility transferUtility =
                TransferUtility.builder()
                        .context(this.context)
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

                            itemImg.setImageBitmap(bitmap);

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

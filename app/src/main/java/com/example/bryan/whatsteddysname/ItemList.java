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
        ImageView itemImg = (ImageView) rowView.findViewById(R.id.item_img);

        try {
            JSONObject item = new JSONObject(items.get(position));
            String name = item.getString("itemName");

            itemName.setText(name);

           File imgFile = new File(item.getString("localPhotoPath"));

           Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());

           itemImg.setImageBitmap(bitmap);
        } catch(JSONException e) {
            Log.d("JSONEXCEPTION", e.getMessage());
        }

        return rowView;
    }
}

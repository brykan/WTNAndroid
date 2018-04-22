package com.example.bryan.whatsteddysname;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.FileProvider;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.amazonaws.mobile.auth.core.IdentityManager;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.AWSStartupHandler;
import com.amazonaws.mobile.client.AWSStartupResult;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBQueryExpression;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.s3.AmazonS3Client;
import com.example.bryan.whatsteddysname.aws.AWSLoginModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.example.bryan.whatsteddysname.AddItemActivity.REQUEST_IMAGE_CAPTURE;

public class CollectionActivity extends AppCompatActivity {
    private DynamoDBMapper dynamoDBMapper;
    private WTNUsersDO user;
    private FloatingActionButton addBtn;
    static final int REQUEST_ADD_ITEM = 2;
    static final int REQUEST_VIEW_ITEM = 3;
    private ListView itemList;
    private ItemList adapter;
    private EditText searchBar;
    private Button cameraSearch;
    private String preSearchPhotoPath;
    private String actualSearchPhotoPath;
    private ProgressDialog searchDialog;
    private SwipeRefreshLayout swipeLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collection);

        // Instantiate a AmazonDynamoDBMapperClient
        try {
            AmazonDynamoDBClient dynamoDBClient = new AmazonDynamoDBClient(AWSMobileClient.getInstance().getCredentialsProvider());
            this.dynamoDBMapper = DynamoDBMapper.builder()
                    .dynamoDBClient(dynamoDBClient)
                    .awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
                    .build();
        } catch (Exception e) {
            Log.d(e.getClass().getName(), e.getMessage(), e);
        }

        Thread thread = getUser();

        try {
            thread.join();
        } catch (Exception e) {
            Log.d(e.getClass().getName(), e.getMessage(), e);
        }

        addBtn = (FloatingActionButton) findViewById(R.id.addItem);
        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CollectionActivity.this, AddItemActivity.class);
                intent.putExtra("USER_ID", user.getUserId());
                startActivityForResult(intent, REQUEST_ADD_ITEM);
            }
        });

        itemList = (ListView) findViewById(R.id.itemList);

        searchBar = (EditText) findViewById(R.id.searchBar);
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                adapter.getFilter().filter(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        cameraSearch = (Button) findViewById(R.id.cameraSearch);
        cameraSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchTakePictureIntent();
            }
        });

        final List<String> items = user.getItems();

        adapter = new ItemList(this, items);

        itemList.setAdapter(adapter);

        itemList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                List <String> items = user.getItems();
                Intent intent = new Intent(CollectionActivity.this, ItemActivity.class);

                intent.putExtra("ITEM", items.get(position));
                intent.putExtra("ITEMINDEX", position);
                startActivityForResult(intent, REQUEST_VIEW_ITEM);
            }
        });

        swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        swipeLayout.setColorSchemeResources(android.R.color.holo_blue_light);
        swipeLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                WTNUsersDO userItem = dynamoDBMapper.load(
                                        WTNUsersDO.class,
                                        user.getUserId(),
                                        user.getUsername());
                                final List<String> updated = userItem.getItems();
                                
                                if(!user.getItems().equals(updated)) {
                                    user.setItems(updated);
                                    runOnUiThread(new Runnable() {
                                        public void run() {
                                            // runs on UI thread
                                            adapter = new ItemList(CollectionActivity.this, updated);
                                            itemList.setAdapter(adapter);
                                            swipeLayout.setRefreshing(false);
                                        }
                                    });
                                }
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        // runs on UI thread
                                        swipeLayout.setRefreshing(false);
                                    }
                                });
                            }
                        }).start();
                    }
                }
        );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.collection, menu); //Menu Resource
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if(menuItem.getItemId() == R.id.sign_out) {
            final DialogInterface.OnClickListener deleteItemListner = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which){
                        case DialogInterface.BUTTON_POSITIVE:
                            //Yes button clicked
                            signOut();
                            break;

                        case DialogInterface.BUTTON_NEGATIVE:
                            //No button clicked
                            dialog.dismiss();
                            break;
                    }
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(CollectionActivity.this);
            AlertDialog dialog = builder.setMessage("Are you sure you want to sign out?")
                    .setPositiveButton("Yes", deleteItemListner)
                    .setNegativeButton("No", deleteItemListner)
                    .create();
            dialog.show();
            Button buttonPositive = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            buttonPositive.setTextColor(Color.parseColor("#DC143C"));

            return true;
        }
        return super.onOptionsItemSelected(menuItem);
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
        String imageFileName =  "pre_" + user.getUserId() + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir);
        preSearchPhotoPath = image.getAbsolutePath();

        return image;
    }

    private File createSearchImage() throws IOException {
        // Create image file name
        String imageFileName = user.getUserId() + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir);
        actualSearchPhotoPath = image.getAbsolutePath();

        return image;
    }

    public Thread getUser() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                WTNUsersDO hashUser = new WTNUsersDO();
                hashUser.setUsername(AWSLoginModel.getUserName(CollectionActivity.this));

                DynamoDBQueryExpression<WTNUsersDO> queryExpression = new DynamoDBQueryExpression<WTNUsersDO>()
                        .withIndexName("GetUser")
                        .withHashKeyValues(hashUser)
                        .withConsistentRead(false);

                List<WTNUsersDO> iList = dynamoDBMapper.query(WTNUsersDO.class, queryExpression);

                if(iList.size() > 0) {
                    user = iList.get(0);
                }
            }
        });

        thread.start();
        return thread;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ADD_ITEM && resultCode == RESULT_OK && data != null) {
            List<String> items = user.getItems();

            items.add(data.getStringExtra("itemResult"));
            user.setItems(items);
            updateUser(user);
            adapter.notifyDataSetChanged();
        } else if(requestCode == REQUEST_VIEW_ITEM) {
            if(resultCode == ItemActivity.RESULT_DELETE_ITEM) {
                List<String> items = user.getItems();
                int position = data.getIntExtra("itemIndex", -1);

                if (position != -1) {
                    items.remove(position);

                    user.setItems(items);
                    updateUser(user);
                    adapter.notifyDataSetChanged();
                }
            } else if(resultCode == ItemActivity.RESULT_UPDATE_ITEM) {
                List<String> items = user.getItems();
                String updatedItem = data.getStringExtra("updatedItem");
                int position = data.getIntExtra("itemIndex", -1);

                if (position != -1) {
                    items.set(position, updatedItem);

                    user.setItems(items);
                    updateUser(user);
                    adapter.notifyDataSetChanged();
                }
            }
        } else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            searchDialog = new ProgressDialog(CollectionActivity.this,
                    R.style.Theme_AppCompat_DayNight_Dialog);
            searchDialog.setIndeterminate(true);
            searchDialog.setCancelable(false);
            searchDialog.setMessage("Searching...");
            searchDialog.show();

            createGrayScale();

            AWSMobileClient.getInstance().initialize(this, new AWSStartupHandler() {
                @Override
                public void onComplete(AWSStartupResult awsStartupResult) {
                    uploadWithTransferUtility();
                }
            }).execute();
        }
    }

    @Override
    protected void onResume() { super.onResume(); }

    public void updateUser(final WTNUsersDO user) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                dynamoDBMapper.save(user);
                // Item updated
            }
        }).start();
    }

    public void createGrayScale() {
        // constant factors
        final double GS_RED = 0.299;
        final double GS_GREEN = 0.587;
        final double GS_BLUE = 0.114;
        // pixel information
        int A, R, G, B;
        int pixel;

        File input = new File(preSearchPhotoPath);

        if(input.exists()) {
            Bitmap src = BitmapFactory.decodeFile(input.getPath());
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
                outFile = createSearchImage();

                if(outFile != null) {
                    FileOutputStream outStream = new FileOutputStream(outFile);
                    out.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
                    outStream.flush();
                    outStream.close();

                    if(input.delete()) {
                        return;
                    }
                }
            } catch (IOException e) {
                Log.d("IOEXCEPTION", e.getMessage());
            }
        }
    }

    public void uploadWithTransferUtility() {
        String fileLocation = "public/search-images/" + user.getUserId() + ".jpg";

        TransferUtility transferUtility =
                TransferUtility.builder()
                        .context(getApplicationContext())
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
        String mlUrl = "https://mv33lyux8f.execute-api.us-east-1.amazonaws.com/prod/classify?user=example-user";
                //+ user.getUserId();
        String result;
        ArrayList<String> resultList = new ArrayList<String>();

        HttpGetRequest getRequest = new HttpGetRequest();

        try {
            result = getRequest.execute(mlUrl).get();

            if(result != null) {
                List<String> items = user.getItems();
                for(int i = 0; i < items.size(); i++) {
                    try {
                        JSONObject item = new JSONObject(items.get(i));

                        if(item.getString("s3Location") == result) {
                            resultList.add(items.get(i));
                        }
                    } catch(JSONException e) {
                        Log.d("JSONEXCEPTION", e.getMessage());
                    }
                }

                searchDialog.cancel();

                Intent intent = new Intent(CollectionActivity.this, SearchResultsActivity.class);

                intent.putStringArrayListExtra("results", (ArrayList<String>) resultList);

                startActivity(intent);
            }
        } catch(InterruptedException i) {
            Log.d("INTERRUPTED", i.getMessage());
        } catch(ExecutionException e) {
            Log.d("EXECUTION", e.getMessage());
        }
    }

    public void signOut() {
        IdentityManager.getDefaultIdentityManager().signOut();

        Toast.makeText(getBaseContext(), "Logged Out", Toast.LENGTH_LONG).show();
        startActivity(new Intent(CollectionActivity.this, LoginActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
        finish();
    }
}

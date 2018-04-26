package com.example.bryan.whatsteddysname.activities;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
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
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.s3.AmazonS3Client;
import com.example.bryan.whatsteddysname.asynctasks.InitiateMLSearchTask;
import com.example.bryan.whatsteddysname.R;
import com.example.bryan.whatsteddysname.aws.AWSLoginModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.List;
import com.example.bryan.whatsteddysname.aws.WTNUsersDO;

import static com.example.bryan.whatsteddysname.activities.AddItemActivity.REQUEST_IMAGE_CAPTURE;

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
                if(charSequence != null) {
                    adapter.getFilter().filter(charSequence.toString());
                }
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
                String item = adapter.getItem(position);
                String search = searchBar.getText().toString();
                int index = position;
                Intent intent = new Intent(CollectionActivity.this, ItemActivity.class);

                if(search.length() > 0) {
                    for (String itm: items) {
                        if (itm.equals(item)) {
                            index = items.indexOf(itm);
                        }
                    }
                }
                intent.putExtra("ITEM", adapter.getItem(position));
                intent.putExtra("ITEMINDEX", index);
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
            final DialogInterface.OnClickListener signOutListener = new DialogInterface.OnClickListener() {
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
                    .setPositiveButton("Yes", signOutListener)
                    .setNegativeButton("No", signOutListener)
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
                    String search = searchBar.getText().toString();
                    try {
                        final AmazonS3Client s3Client =
                                new AmazonS3Client(
                                        AWSMobileClient.getInstance().getCredentialsProvider());
                        final JSONObject configuration =
                                AWSMobileClient
                                        .getInstance()
                                        .getConfiguration()
                                        .optJsonObject("S3TransferUtility");
                        final JSONObject json = new JSONObject(items.get(position));

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                s3Client.deleteObject(
                                        configuration.getString("Bucket"),
                                        json.getString("s3Location"));
                            } catch(JSONException j) {
                                Log.d("JSONEXCEPTION", j.getMessage());
                            }
                            }
                        }).start();

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    s3Client.deleteObject(
                                            configuration.getString("Bucket"),
                                            json.getString("grayS3Location"));
                                } catch(JSONException j) {
                                    Log.d("JSONEXCEPTION", j.getMessage());
                                }
                            }
                        }).start();
                    } catch(JSONException j) {
                        Log.d("JSONEXCEPTION", j.getMessage());
                    }
                    
                    items.remove(position);

                    user.setItems(items);
                    updateUser(user);

                    if (search.length() > 0) {
                        searchBar.setText(null);
                    }

                    adapter.notifyDataSetChanged();
                }
            } else if(resultCode == ItemActivity.RESULT_UPDATE_ITEM) {
                List<String> items = user.getItems();
                String updatedItem = data.getStringExtra("updatedItem");
                int position = data.getIntExtra("itemIndex", -1);

                if (position != -1) {
                    String search = searchBar.getText().toString();
                    items.set(position, updatedItem);

                    user.setItems(items);
                    updateUser(user);

                    if (search.length() > 0) {
                        searchBar.setText(null);
                    }

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

            new InitiateMLSearchTask(
                    this,
                    getApplicationContext(),
                    CollectionActivity.this,
                    searchDialog,
                    preSearchPhotoPath,
                    user.getUserId(),
                    user.getItems()).execute();
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

    public void signOut() {
        IdentityManager.getDefaultIdentityManager().signOut();

        Toast.makeText(getBaseContext(), "Logged Out", Toast.LENGTH_LONG).show();
        startActivity(new Intent(CollectionActivity.this, LoginActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
        finish();
    }
}

package com.example.bryan.whatsteddysname;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBQueryExpression;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.example.bryan.whatsteddysname.aws.AWSLoginModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CollectionActivity extends AppCompatActivity {
    private DynamoDBMapper dynamoDBMapper;
    private WTNUsersDO user;
    private Button addBtn;
    static final int REQUEST_ADD_ITEM = 1;
    private ListView itemList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collection);

        // Initialize the Amazon Cognito credentials provider
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                this,
                "us-east-1:0aba8b06-2682-4d64-8131-86352213cb4a", // Identity pool ID
                Regions.US_EAST_1 // Region
        );

        // Instantiate a AmazonDynamoDBMapperClient
        try {
            AmazonDynamoDBClient dynamoDBClient = new AmazonDynamoDBClient(credentialsProvider);
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

        addBtn = (Button) findViewById(R.id.addItem);
        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CollectionActivity.this, AddItemActivity.class);
                intent.putExtra("USER_ID", user.getUserId());
                startActivityForResult(intent, REQUEST_ADD_ITEM);
            }
        });

        itemList = (ListView) findViewById(R.id.itemList);
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

                user = iList.get(0);
            }
        });

        thread.start();
        return thread;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ADD_ITEM && resultCode == RESULT_OK && data != null) {
            Log.d("ONACTIVITYRESULT", "DANE IN HERE");
            List<String> items = user.getItems();

            items.add(data.getStringExtra("itemResult"));
            user.setItems(items);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        List<String> items = user.getItems();

        Log.d("ONRESUMEE", user.getItems().toString());
        ItemList adapter = new ItemList(this, items);
        itemList.setAdapter(adapter);
    }
}

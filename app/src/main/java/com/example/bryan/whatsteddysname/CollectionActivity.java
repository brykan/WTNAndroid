package com.example.bryan.whatsteddysname;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

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
}

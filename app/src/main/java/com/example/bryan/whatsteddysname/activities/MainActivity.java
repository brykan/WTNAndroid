package com.example.bryan.whatsteddysname.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.example.bryan.whatsteddysname.R;

public class MainActivity extends AppCompatActivity {
    private Button logInBtn;
    private Button signUpBtn;
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action= intent.getStringExtra("action");
            if(action.equals("close")) {
                MainActivity.this.finish();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        logInBtn = (Button) findViewById(R.id.logInBtn);
        logInBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openActivity("Login");
            }
        });

        signUpBtn = (Button) findViewById(R.id.signUpBtn);
        signUpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openActivity("Signup");
            }
        });

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter("CloseMain"));
    }

    public void openActivity(String activityName) {
        Intent intent;
        switch (activityName) {
            case "Login":
                intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
                break;
            case "Signup":
                intent = new Intent(this, SignupActivity.class);
                startActivity(intent);
                break;
        }
    }
}

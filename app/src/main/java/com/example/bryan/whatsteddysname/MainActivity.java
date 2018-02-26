package com.example.bryan.whatsteddysname;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import com.amazonaws.mobile.client.AWSMobileClient;

public class MainActivity extends AppCompatActivity {
    private Button logInBtn;
    private Button signUpBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AWSMobileClient.getInstance().initialize(this).execute();

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
                openActivity("Sign-up");
            }
        });
    }

    public void openActivity(String activityName) {
        if(activityName == "Login") {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        } else if (activityName == "Sign-up") {
            Intent intent = new Intent(this, SignupActivity.class);
            startActivity(intent);
        }
    }
}

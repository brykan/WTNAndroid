package com.example.bryan.whatsteddysname;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.amazonaws.mobile.auth.core.IdentityManager;
import com.amazonaws.mobile.auth.core.StartupAuthResult;
import com.amazonaws.mobile.auth.core.StartupAuthResultHandler;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.AWSStartupHandler;
import com.amazonaws.mobile.client.AWSStartupResult;


public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        AWSMobileClient.getInstance().initialize(SplashActivity.this, new AWSStartupHandler() {
            @Override
            public void onComplete(AWSStartupResult awsStartupResult) {
                IdentityManager identityManager = IdentityManager.getDefaultIdentityManager();
                identityManager.resumeSession(SplashActivity.this, new StartupAuthResultHandler() {
                    @Override
                    public void onComplete(StartupAuthResult authResults) {
                        if (authResults.isUserSignedIn()) {
                            startActivity(new Intent(SplashActivity.this, CollectionActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                            finish();
                        } else {
                            startActivity(new Intent(SplashActivity.this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                            finish();
                        }
                    }
                }, 4000);
            }
        }).execute();
    }
}

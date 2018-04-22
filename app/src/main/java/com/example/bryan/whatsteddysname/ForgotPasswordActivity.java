package com.example.bryan.whatsteddysname;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.amazonaws.mobile.auth.core.IdentityManager;
import com.amazonaws.mobile.auth.core.StartupAuthResult;
import com.amazonaws.mobile.auth.core.StartupAuthResultHandler;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ForgotPasswordContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.ForgotPasswordHandler;
import com.example.bryan.whatsteddysname.aws.AWSLoginHandler;
import com.example.bryan.whatsteddysname.aws.AWSLoginModel;

import org.w3c.dom.Text;

public class ForgotPasswordActivity extends AppCompatActivity implements AWSLoginHandler {
    private AWSLoginModel awsLoginModel;
    private TextInputEditText codeEntered;
    private TextInputEditText newPassword;
    private TextInputEditText confirmNewPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        awsLoginModel = new AWSLoginModel(this, this);

        codeEntered = (TextInputEditText) findViewById(R.id.input_reset_code);
        newPassword = (TextInputEditText) findViewById(R.id.input_new_pass);
        confirmNewPassword = (TextInputEditText) findViewById(R.id.input_confirm_new_pass);

        Intent intent = getIntent();
        String username = intent.getStringExtra("username");

        CognitoUserPool userPool = awsLoginModel.getmCognitoUserPool(this);
        CognitoUser user = userPool.getUser(username);

        View view = findViewById(android.R.id.content);

        ForgotPasswordRequest request =
                new ForgotPasswordRequest(
                        user,
                        getBaseContext(),
                        ForgotPasswordActivity.this,
                        view,
                        codeEntered,
                        newPassword,
                        confirmNewPassword);

        request.execute();
    }

    @Override
    public void onRegisterSuccess() { /* Not Implemented here */}

    @Override
    public void onRegisterConfirmed() { /* Not Implemented here */}

    @Override
    public void onSignInSuccess() { /* Not Implemented here */}

    @Override
    public void onFailure(int process, Exception exception) { /* Not Implemented here */}
}

package com.example.bryan.whatsteddysname;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.example.bryan.whatsteddysname.aws.AWSLoginHandler;
import com.example.bryan.whatsteddysname.aws.AWSLoginModel;

public class LoginActivity extends AppCompatActivity implements AWSLoginHandler {
    private DynamoDBMapper dynamoDBMapper;
    private TextView linkToLogin;
    private Button loginBtn;
    private TextInputEditText userNameEntered;
    private TextInputEditText passwordEntered;
    private ProgressDialog progressDialog;
    private AWSLoginModel awsLoginModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // instantiating AWSLoginModel(context, callback)
        awsLoginModel = new AWSLoginModel(this, this);

        loginBtn = (Button) findViewById(R.id.btn_login);
        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });

        userNameEntered = (TextInputEditText) findViewById(R.id.login_username);
        passwordEntered = (TextInputEditText) findViewById(R.id.login_password);
    }

    @Override
    public void onRegisterSuccess() {
       // Not implemented here
    }

    @Override
    public void onRegisterConfirmed() {
        // Not implemented here
    }

    @Override
    public void onSignInSuccess() {
        progressDialog.cancel();
        Toast.makeText(getBaseContext(), "Logged In", Toast.LENGTH_LONG).show();
        LoginActivity.this.startActivity(new Intent(LoginActivity.this, CollectionActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }

    @Override
    public void onFailure(int process, Exception exception) {
        exception.printStackTrace();
        String whatProcess = "";
        switch (process) {
            case AWSLoginModel.PROCESS_SIGN_IN:
                whatProcess = "Sign In:";
                break;
            case AWSLoginModel.PROCESS_REGISTER:
                whatProcess = "Registration:";
                break;
            case AWSLoginModel.PROCESS_CONFIRM_REGISTRATION:
                whatProcess = "Registration Confirmation:";
                break;
        }
        Toast.makeText(getBaseContext(), whatProcess + exception.getMessage(), Toast.LENGTH_LONG).show();
    }

    public void login() {
        String username = userNameEntered.getText().toString();
        String password = passwordEntered.getText().toString();

        if (!validateLogin()) {
            Toast.makeText(getBaseContext(), "Login Failed", Toast.LENGTH_LONG).show();
            return;
        }

        progressDialog = new ProgressDialog(LoginActivity.this,
                R.style.Theme_AppCompat_DayNight_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Logging In...");
        progressDialog.show();

        // do sign in and handles on interface
        awsLoginModel.signInUser(username, password);
    }

    public boolean validateLogin() {
        boolean valid = true;
        String username = userNameEntered.getText().toString();
        String password = passwordEntered.getText().toString();

        if (username.isEmpty() || username.length() <= 3 || !(username.matches("[a-zA-Z].*"))) {
            userNameEntered.setError("Must be at least 4 characters and start with a letter.");
            valid = false;
        } else {
            userNameEntered.setError(null);
        }

        if (password.isEmpty() || password.length() < 8 || !password.matches(".*([a-zA-Z].*[0-9]|[0-9].*[a-zA-Z]).*")) {
            passwordEntered.setError("Must be at least 8 characters and contain letters and numbers.");
            valid = false;
        } else {
            passwordEntered.setError(null);
        }

        return valid;
    }
}

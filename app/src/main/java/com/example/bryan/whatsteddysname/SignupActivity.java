package com.example.bryan.whatsteddysname;

import android.content.Intent;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.app.ProgressDialog;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;

public class SignupActivity extends AppCompatActivity {
    private DynamoDBMapper dynamoDBMapper;
    private TextView linkToLogin;
    private Button signUpBtn;
    private TextInputEditText userNameEntered;
    private TextInputEditText emailEntered;
    private TextInputEditText passwordEntered;
    private TextInputEditText confirmPasswordEntered;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        AWSConfiguration obj = AWSMobileClient.getInstance().getConfiguration();
        Log.d("SIGNUPACTIVITY", obj.optJsonObject("CognitoUserPool").toString());
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

        signUpBtn = (Button) findViewById(R.id.btn_signup);
        signUpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signup();
            }
        });

        linkToLogin = (TextView) findViewById(R.id.link_login);
        linkToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToLogin();
            }
        });
    }

    public void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    public void signup() {
        if (!validate()) {
            Toast.makeText(getBaseContext(), "Create Account Failed", Toast.LENGTH_LONG).show();
            return;
        }

        final ProgressDialog progressDialog = new ProgressDialog(SignupActivity.this,
                R.style.Theme_AppCompat_DayNight_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Creating Account...");
        progressDialog.show();


    }

    public boolean validate() {
        boolean valid = true;

        userNameEntered = (TextInputEditText) findViewById(R.id.input_username);
        String username = userNameEntered.getText().toString();

        passwordEntered = (TextInputEditText) findViewById(R.id.input_password);
        String password = passwordEntered.getText().toString();

        confirmPasswordEntered = (TextInputEditText) findViewById(R.id.input_confirm_password);
        String passwordConfirm = confirmPasswordEntered.getText().toString();

        emailEntered = (TextInputEditText) findViewById(R.id.input_email);
        String email = emailEntered.getText().toString();

        if (username.isEmpty() || username.length() <= 3 || !(username.matches("[a-zA-Z].*"))) {
            userNameEntered.setError("Must be at least 4 characters and start with a letter.");
            valid = false;
        } else {
            userNameEntered.setError(null);
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEntered.setError("Enter a valid email address.");
            valid = false;
        } else {
            emailEntered.setError(null);
        }

        if (password.isEmpty() || password.length() < 8 || !password.matches(".*([a-zA-Z].*[0-9]|[0-9].*[a-zA-Z]).*")) {
            passwordEntered.setError("Must be at least 8 characters and contain letters and numbers.");
            valid = false;
        } else {
            passwordEntered.setError(null);
        }

        if (passwordConfirm.isEmpty() || !passwordConfirm.equals(password)) {
            confirmPasswordEntered.setError("Passwords do not match.");
            valid = false;
        } else {
            confirmPasswordEntered.setError(null);
        }

        return valid;
    }
}

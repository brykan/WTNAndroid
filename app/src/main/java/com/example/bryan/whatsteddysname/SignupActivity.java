package com.example.bryan.whatsteddysname;

import android.app.Dialog;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.app.ProgressDialog;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.example.bryan.whatsteddysname.aws.AWSLoginHandler;
import com.example.bryan.whatsteddysname.aws.AWSLoginModel;

public class SignupActivity extends AppCompatActivity implements AWSLoginHandler {
    private DynamoDBMapper dynamoDBMapper;
    private TextView linkToLogin;
    private Button signUpBtn;
    private TextInputEditText userNameEntered;
    private TextInputEditText emailEntered;
    private TextInputEditText passwordEntered;
    private TextInputEditText confirmPasswordEntered;
    private EditText confirmCodeInput;
    private ProgressDialog progressDialog;
    private AWSLoginModel awsLoginModel;
    private AlertDialog confirmPrompt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

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

        // instantiating AWSLoginModel(context, callback)
        awsLoginModel = new AWSLoginModel(this, this);

        userNameEntered = (TextInputEditText) findViewById(R.id.input_username);
        passwordEntered = (TextInputEditText) findViewById(R.id.input_password);
        emailEntered = (TextInputEditText) findViewById(R.id.input_email);
        confirmPasswordEntered = (TextInputEditText) findViewById(R.id.input_confirm_password);

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

        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.confirm_prompt, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this, R.style.Theme_AppCompat_DayNight_Dialog_Alert);
        alertDialogBuilder.setView(promptsView);

        confirmCodeInput = (EditText) promptsView.findViewById(R.id.confirm_edittext);

        // set dialog message
        alertDialogBuilder.setCancelable(false)
                .setPositiveButton("Enter",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // do confirmation and handles on interface
                                awsLoginModel.confirmRegistration(confirmCodeInput.getText().toString());
                            }
                        });
        // create alert dialog
        confirmPrompt = alertDialogBuilder.create();
    }

    @Override
    public void onRegisterSuccess() {
        progressDialog.cancel();
        Toast.makeText(getBaseContext(), "Confirmation code sent to email", Toast.LENGTH_LONG).show();
        confirmPrompt.show();
    }

    @Override
    public void onRegisterConfirmed() {
        confirmPrompt.cancel();
        Toast.makeText(getBaseContext(), "Confirmation Success", Toast.LENGTH_LONG).show();
        SignupActivity.this.startActivity(new Intent(SignupActivity.this, LoginActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }

    @Override
    public void onSignInSuccess() {
        // To be implemented in LoginActivity
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

    public void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    public void signup() {
        if (!validate()) {
            Toast.makeText(getBaseContext(), "Create Account Failed", Toast.LENGTH_LONG).show();
            return;
        }

        progressDialog = new ProgressDialog(SignupActivity.this,
                R.style.Theme_AppCompat_DayNight_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Creating Account...");
        progressDialog.show();

        createAccount();
    }

    public void createAccount() {
        String username = userNameEntered.getText().toString();
        String password = passwordEntered.getText().toString();
        String email = emailEntered.getText().toString();

        awsLoginModel.registerUser(username, email, password);
    }

    public boolean validate() {
        boolean valid = true;
        String username = userNameEntered.getText().toString();
        String password = passwordEntered.getText().toString();
        String passwordConfirm = confirmPasswordEntered.getText().toString();
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

package com.example.bryan.whatsteddysname;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.mobile.auth.core.IdentityManager;
import com.amazonaws.mobile.auth.core.StartupAuthResult;
import com.amazonaws.mobile.auth.core.StartupAuthResultHandler;
import com.example.bryan.whatsteddysname.aws.AWSLoginHandler;
import com.example.bryan.whatsteddysname.aws.AWSLoginModel;

public class LoginActivity extends AppCompatActivity implements AWSLoginHandler {
    private TextView linktoForgotPassword;
    private Button loginBtn;
    private TextInputEditText userNameEntered;
    private TextInputEditText passwordEntered;
    private EditText usernameInput;
    private ProgressDialog progressDialog;
    private AlertDialog forgotPasswordPrompt;
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

        View forgotPrompt = getPromptView(R.layout.forgot_prompt);
        usernameInput = (EditText) forgotPrompt.findViewById(R.id.forgot_edittext);
        forgotPasswordPrompt = createPrompt(forgotPrompt, "Enter",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // do confirmation and handles on interface
                        String username = usernameInput.getText().toString();
                        if (username.isEmpty() || username.length() <= 3 || !(username.matches("[a-zA-Z].*"))) {
                            usernameInput.setError("Must be at least 4 characters and start with a letter.");
                        } else {
                            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
                            intent.putExtra("username", username);

                            Toast.makeText(getBaseContext(), "Confirmation code sent.", Toast.LENGTH_LONG).show();
                            startActivity(intent);
                        }
                    }
                });;

        linktoForgotPassword = (TextView) findViewById(R.id.forgot_password);
        linktoForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                forgotPasswordPrompt.show();
            }
        });

        userNameEntered = (TextInputEditText) findViewById(R.id.login_username);
        passwordEntered = (TextInputEditText) findViewById(R.id.login_password);
    }

    @Override
    public void onRegisterSuccess() { /* Not implemented here */ }

    @Override
    public void onRegisterConfirmed() { /* Not implemented here */ }

    @Override
    public void onSignInSuccess() {
        IdentityManager identityManager = IdentityManager.getDefaultIdentityManager();

        identityManager.resumeSession(LoginActivity.this, new StartupAuthResultHandler() {
            @Override
            public void onComplete(StartupAuthResult authResults) {
                if (authResults.isUserSignedIn()) {
                    progressDialog.cancel();
                    Toast.makeText(getBaseContext(), "Logged In", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(LoginActivity.this, CollectionActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                    finish();
                } else {
                    progressDialog.cancel();
                    Toast.makeText(getBaseContext(), "Login Failed", Toast.LENGTH_LONG).show();
                }
            }
        }, 0);
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
        progressDialog.cancel();
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

    public View getPromptView(int promptId) {
        LayoutInflater li = LayoutInflater.from(this);
        return li.inflate(promptId, null);
    }

    public AlertDialog createPrompt(
            View promptsView,
            String positiveText,
            DialogInterface.OnClickListener listener) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this, R.style.Theme_AppCompat_DayNight_Dialog_Alert);
        alertDialogBuilder.setView(promptsView);

        // set dialog message
        alertDialogBuilder.setPositiveButton(positiveText, listener);

        return alertDialogBuilder.create();
    }
}

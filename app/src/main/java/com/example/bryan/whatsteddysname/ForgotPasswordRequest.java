package com.example.bryan.whatsteddysname;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.design.widget.TextInputEditText;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ForgotPasswordContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.ForgotPasswordHandler;

import static com.amazonaws.mobile.auth.core.internal.util.ThreadUtils.runOnUiThread;

/**
 * Created by john_le on 4/22/18.
 */

public class ForgotPasswordRequest extends AsyncTask<Void, Void, Void>{
    private CognitoUser user;
    private Context context;
    private Context base;
    private View root;
    private Button resetPassBtn;
    private TextInputEditText codeEntered;
    private TextInputEditText newPassword;
    private TextInputEditText confirmNewPassword;

    public ForgotPasswordRequest(
            CognitoUser user,
            Context base,
            Context context,
            View root,
            TextInputEditText codeEntered,
            TextInputEditText newPassword,
            TextInputEditText confirmNewPassword) {
        this.user = user;
        this.context = context;
        this.base = base;
        this.root = root;
        this.codeEntered = codeEntered;
        this.newPassword = newPassword;
        this.confirmNewPassword = confirmNewPassword;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected Void doInBackground(Void... voids) {
        user.forgotPassword(new ForgotPasswordHandler() {
            @Override
            public void onSuccess() {
                runOnUiThread(new Runnable() {
                    public void run() {
                        // runs on UI thread
                        Toast.makeText(base, "Password reset.", Toast.LENGTH_LONG).show();
                    }
                });
                context.startActivity(new Intent(context, LoginActivity.class));
                ((Activity) context).finish();
            }

            @Override
            public void getResetCode(final ForgotPasswordContinuation continuation) {
                resetPassBtn = (Button) root.findViewById(R.id.resetPassBtn);
                resetPassBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(!valid()) {
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    // runs on UI thread
                                    Toast.makeText(base, "Reset Password Failed", Toast.LENGTH_LONG).show();
                                }
                            });
                            return;
                        }
                        String password = newPassword.getText().toString();
                        String code = codeEntered.getText().toString();

                        ForgotPasswordContinuationTask task =
                                new ForgotPasswordContinuationTask(
                                        continuation,
                                        password,
                                        code
                                );

                        task.execute();
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                e.printStackTrace();
            }
        });
        return null;
    }

    public Boolean valid() {
        String password = newPassword.getText().toString();
        String passwordConfirm = confirmNewPassword.getText().toString();
        Boolean valid = true;

        if (password.isEmpty() || password.length() < 8 || !password.matches(".*([a-zA-Z].*[0-9]|[0-9].*[a-zA-Z]).*")) {
            newPassword.setError("Must be at least 8 characters and contain letters and numbers.");
            valid = false;
        } else {
            newPassword.setError(null);
        }

        if (passwordConfirm.isEmpty() || !passwordConfirm.equals(password)) {
            confirmNewPassword.setError("Passwords do not match.");
            valid = false;
        } else {
            confirmNewPassword.setError(null);
        }

        return valid;
    }
}

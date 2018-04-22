package com.example.bryan.whatsteddysname;

import android.os.AsyncTask;

import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ForgotPasswordContinuation;

/**
 * Created by john_le on 4/22/18.
 */

public class ForgotPasswordContinuationTask extends AsyncTask<Void, Void, Void>{
    private ForgotPasswordContinuation continuation;
    private String password;
    private String code;

    public ForgotPasswordContinuationTask(
            ForgotPasswordContinuation continuation,
            String password,
            String code) {
        this.continuation = continuation;
        this.password = password;
        this.code = code;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        continuation.setPassword(password);

        continuation.setVerificationCode(code);

        continuation.continueTask();
        return null;
    }
}

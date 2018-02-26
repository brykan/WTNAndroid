package com.example.bryan.whatsteddysname.aws;

import android.content.Context;

import com.amazonaws.mobile.auth.core.IdentityManager;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserAttributes;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserCodeDeliveryDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GenericHandler;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GetDetailsHandler;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.SignUpHandler;
import com.amazonaws.regions.Regions;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * This represents a model for login operations on AWS Mobile Hub. It manages login operations
 * such as:
 * - Sign In
 * - Sign Up
 *
 */

public class AWSLoginModel {
    // constants
    public static final int PROCESS_SIGN_IN = 1;
    public static final int PROCESS_REGISTER = 2;
    public static final int PROCESS_CONFIRM_REGISTRATION = 3;

    // interface handler
    private AWSLoginHandler mCallback;

    // control variables
    private String userName, userPassword;
    private Context mContext;
    private CognitoUserPool mCognitoUserPool;
    private CognitoUser mCognitoUser;

    private final AuthenticationHandler authenticationHandler = new AuthenticationHandler() {
        @Override
        public void onSuccess(CognitoUserSession userSession, CognitoDevice newDevice) {
            // Get details of the logged user (in this case, only the e-mail)
            mCognitoUser.getDetailsInBackground(new GetDetailsHandler() {
                @Override
                public void onSuccess(CognitoUserDetails cognitoUserDetails) {

                }

                @Override
                public void onFailure(Exception exception) {
                    exception.printStackTrace();
                }
            });

            mCallback.onSignInSuccess();
        }

        @Override
        public void getAuthenticationDetails(AuthenticationContinuation authenticationContinuation, String userId) {
            final AuthenticationDetails authenticationDetails = new AuthenticationDetails(userName, userPassword, null);
            authenticationContinuation.setAuthenticationDetails(authenticationDetails);
            authenticationContinuation.continueTask();
            userPassword = "";
        }

        @Override
        public void getMFACode(MultiFactorAuthenticationContinuation continuation) {
            // Not implemented for this Model
        }

        @Override
        public void authenticationChallenge(ChallengeContinuation continuation) {
            // Not implemented for this Model
        }

        @Override
        public void onFailure(Exception exception) {
            mCallback.onFailure(PROCESS_SIGN_IN, exception);
        }
    };

    /**
     * Constructs the model for login functions in AWS Mobile Hub.
     *
     * @param context         REQUIRED: Android application context.
     * @param callback        REQUIRED: Callback handler for login operations.
     *
     */
    public AWSLoginModel(Context context, AWSLoginHandler callback) {
        mContext = context;
        IdentityManager identityManager = IdentityManager.getDefaultIdentityManager();
        try{
            JSONObject myJSON = identityManager.getConfiguration().optJsonObject("CognitoUserPool");
            final String COGNITO_POOL_ID = myJSON.getString("PoolId");
            final String COGNITO_CLIENT_ID = myJSON.getString("AppClientId");
            final String COGNITO_CLIENT_SECRET = myJSON.getString("AppClientSecret");
            final String REGION = myJSON.getString("Region");
            mCognitoUserPool = new CognitoUserPool(context, COGNITO_POOL_ID, COGNITO_CLIENT_ID, COGNITO_CLIENT_SECRET, Regions.fromName(REGION));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mCallback = callback;
    }

    /**
     * Registers new user to the AWS Cognito User Pool.
     *
     * This will trigger {@link AWSLoginHandler} interface defined when the constructor was called.
     *
     * @param userName         REQUIRED: Username to be registered. Must be unique in the User Pool.
     * @param userEmail        REQUIRED: E-mail to be registered. Must be unique in the User Pool.
     * @param userPassword     REQUIRED: Password of this new account.
     *
     */
    public void registerUser(String userName, String userEmail, String userPassword) {
        CognitoUserAttributes userAttributes = new CognitoUserAttributes();
        userAttributes.addAttribute("email", userEmail);

        final SignUpHandler signUpHandler = new SignUpHandler() {
            @Override
            public void onSuccess(CognitoUser user, boolean signUpConfirmationState, CognitoUserCodeDeliveryDetails cognitoUserCodeDeliveryDetails) {
                mCognitoUser = user;
                mCallback.onRegisterSuccess();
            }

            @Override
            public void onFailure(Exception exception) {
                mCallback.onFailure(PROCESS_REGISTER, exception);
            }
        };

        mCognitoUserPool.signUpInBackground(userName, userPassword, userAttributes, null, signUpHandler);
    }

    /**
     * Confirms registration of the new user in AWS Cognito User Pool.
     *
     * This will trigger {@link AWSLoginHandler} interface defined when the constructor was called.
     *
     * @param confirmationCode      REQUIRED: Code sent from AWS to the user.
     */
    public void confirmRegistration(String confirmationCode) {
        final GenericHandler confirmationHandler = new GenericHandler() {
            @Override
            public void onSuccess() {
                mCallback.onRegisterConfirmed();
            }

            @Override
            public void onFailure(Exception exception) {
                mCallback.onFailure(PROCESS_CONFIRM_REGISTRATION, exception);
            }
        };

        mCognitoUser.confirmSignUpInBackground(confirmationCode, false, confirmationHandler);
    }

    /**
     * Sign in process.
     *
     * This will trigger {@link AWSLoginHandler} interface defined when the constructor was called.
     *
     * @param userName               REQUIRED: Username.
     * @param userPassword           REQUIRED: Password.
     */
    public void signInUser(String userName, String userPassword) {
        this.userName = userName;
        this.userPassword = userPassword;

        mCognitoUser = mCognitoUserPool.getUser(userName);
        mCognitoUser.getSessionInBackground(authenticationHandler);
    }
}

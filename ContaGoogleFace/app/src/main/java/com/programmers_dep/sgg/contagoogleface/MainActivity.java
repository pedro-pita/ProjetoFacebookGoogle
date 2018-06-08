package com.programmers_dep.sgg.contagoogleface;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.ProfileTracker;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    //GOOGLE
    private static final String TAG_GOOGLE = "SignInActivity";
    private static final int RC_SIGN_IN = 9001;

    SignInButton signInButton;

    private GoogleSignInClient mGoogleSignInClient;

    //FACEBOOK
    private final String TAG_FACE = "FACEBOOK";

    private LoginButton loginBtn;

    private boolean Logged=false;

    private TextView userName, userStatus;
    CallbackManager callbackManager;
    private ProfileTracker profileTracker;
    private AccessTokenTracker accessTokenTracker;
    //private Profile profile;

    private static final List<String> PERMISSIONS = Arrays.asList("publish_actions");

    private static String message = "Testando o SDK do Facebook para Android!";

    //TODO Ao fechar e  reabrir a aplicação enquanto está loggado no face ele não põe os dados na tela

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.activity_main);

        // Views
        userStatus = findViewById(R.id.user_status);

        // Button listeners
        findViewById(R.id.sign_in_button).setOnClickListener(this);
        findViewById(R.id.sign_out_button).setOnClickListener(this);
        findViewById(R.id.disconnect_button).setOnClickListener(this);

        // [START configure_signin]
        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        // [END configure_signin]

        // [START build_client]
        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        // [END build_client]

        // [START customize_button]
        // Set the dimensions of the sign-in button.
        signInButton = findViewById(R.id.sign_in_button);
        signInButton.setSize(SignInButton.SIZE_STANDARD);
        signInButton.setColorScheme(SignInButton.COLOR_LIGHT);
        // [END customize_button]

        //FACEBOOK
        userName = (TextView) findViewById(R.id.username);
        loginBtn = (LoginButton) findViewById(R.id.fb_login_button);

        if(Logged=isLoggedIn()) {
            SharedPreferences login = getSharedPreferences("login_data", 0);
            String userName1 = login.getString("username","");
            String userLastName1 = login.getString("userlastname", "");

            userName.setText(userName1 + " " + userLastName1);
            userStatus.setText("Ativo");

            Log.d(TAG_FACE,"Already logged in");
            Log.d(TAG_FACE,"user "+userName1+" "+userLastName1);
            Log.d(TAG_FACE,userName.getText().toString());
            Log.d(TAG_FACE,userStatus.getText().toString());
        }else
            userStatus.setText("Inativo");

        loginBtn.setReadPermissions(Arrays.asList("public_profile", "email","user_friends"));
        callbackManager = CallbackManager.Factory.create();
        accessTokenTracker = new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(AccessToken oldAccessToken, AccessToken currentAccessToken) {

            }
        };

        loginBtn.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d(TAG_FACE,"Login sucess"+loginResult.getAccessToken().getUserId());
                Toast.makeText(getApplicationContext(), "Logging in...", Toast.LENGTH_SHORT).show();
                String accessToken = loginResult.getAccessToken().getToken();

                GraphRequest request = GraphRequest.newMeRequest(
                        loginResult.getAccessToken(),
                        new GraphRequest.GraphJSONObjectCallback() {
                            @Override
                            public void onCompleted(JSONObject jsonObject,
                                                    GraphResponse response) {
                                // Getting FB User Data
                                Bundle facebookData = getFacebookData(jsonObject);
                                String faceName = facebookData.getString("first_name")+" "+facebookData.getString("last_name");
                                if(faceName!=userName.getText().toString())
                                    userName.setText(faceName);
                            }
                        });
                Bundle parameters = new Bundle();
                parameters.putString("fields", "id,first_name,last_name");
                request.setParameters(parameters);
                request.executeAsync();
                Toast.makeText(getApplicationContext(),"Logged in",Toast.LENGTH_SHORT).show();
                userStatus.setText("Ativo");
                signInButton.setVisibility(View.GONE);
            }

            @Override
            public void onCancel() {
                userStatus.setText("Login cancel");
                Log.d(TAG_FACE, "Login canceled.");
                findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
                userName.setText("");
            }

            @Override
            public void onError(FacebookException exception) {
                exception.printStackTrace();
                Log.d(TAG_FACE, "Login attempt failed.");
                deleteAccessToken();
                signInButton.setVisibility(View.VISIBLE);
                userName.setText("");
                userStatus.setText("Inativo");
            }
        });
        if(!accessTokenTracker.isTracking()){
            userName.setText("");
        }

    }

    //GOOGLE
    @Override
    public void onStart() {
        super.onStart();

        // [START on_start_sign_in]
        // Check for existing Google Sign In account, if the user is already signed in
        // the GoogleSignInAccount will be non-null.
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        updateUI(account);
        // [END on_start_sign_in]
    }

    // [START onActivityResult]
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            //GOOGLE
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task =
                    GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }else{
            //FACEBOOK
            callbackManager.onActivityResult(requestCode, resultCode, data);
        }
    }
    // [END onActivityResult]

    // [START handleSignInResult]
    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            // Signed in successfully, show authenticated UI.
            updateUI(account);
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for
            // more information.
            Log.w(TAG_GOOGLE, "signInResult:failed code=" + e.getStatusCode());
            if (e.getStatusCode()==7)
                Toast.makeText(this,"Login Error: Check Network Connection",Toast.LENGTH_SHORT).show();
            updateUI(null);
        }
    }
    // [END handleSignInResult]

    // [START signIn]
    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
        userStatus.setText("Activo");
        loginBtn.setVisibility(View.GONE);
    }
    // [END signIn]

    // [START signOut]
    private void signOut() {
        mGoogleSignInClient.signOut()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        // [START_EXCLUDE]
                        updateUI(null);
                        // [END_EXCLUDE]
                    }
                });
    }
    // [END signOut]

    // [START revokeAccess]
    private void revokeAccess() {
        mGoogleSignInClient.revokeAccess()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        // [START_EXCLUDE]
                        updateUI(null);
                        // [END_EXCLUDE]
                    }
                });
    }
    // [END revokeAccess]

    private void updateUI(@Nullable GoogleSignInAccount account) {
        if (account != null) {
            SharedPreferences account_google = getSharedPreferences("login_data_google", 0);
            SharedPreferences.Editor editor = account_google.edit();
            editor.putString("username", account.getDisplayName());
            editor.commit();

            userName.setText(account_google.getString("username", ""));
            userStatus.setText("Ativo");

            //View.GONE - (Não é exibido e não ocupa o espaço em tela)
            findViewById(R.id.sign_in_button).setVisibility(View.GONE);
            findViewById(R.id.fb_login_button).setVisibility(View.GONE);
            findViewById(R.id.sign_out_and_disconnect).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
            findViewById(R.id.fb_login_button).setVisibility(View.VISIBLE);
            findViewById(R.id.sign_out_and_disconnect).setVisibility(View.GONE);
            userName.setText("");
            userStatus.setText("Inativo");
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                signIn();
                break;
            case R.id.sign_out_button:
                signOut();
                break;
            case R.id.disconnect_button:
                revokeAccess();
                break;
        }
    }

    //FACEBOOK
    private boolean isLoggedIn(){
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        boolean isLoggedIn = accessToken != null && !accessToken.isExpired();
        return isLoggedIn;
    }

    private void deleteAccessToken() {
        AccessTokenTracker accessTokenTracker = new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(
                    AccessToken oldAccessToken,
                    AccessToken currentAccessToken) {

                if (currentAccessToken == null){
                    //User logged out
                    LoginManager.getInstance().logOut();
                    findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
                    userName.setText("");
                    userStatus.setText("Inativo");
                }
            }
        };
    }

    private Bundle getFacebookData(JSONObject object) {
        Bundle bundle = new Bundle();
        try {
            String id = object.getString("id");
            URL profile_pic;
            try {
                profile_pic = new URL("https://graph.facebook.com/" + id
                        + "/picture?type=large");
                Log.i("profile_pic", profile_pic + "");
                //TODO set profile picture
                bundle.putString("profile_pic", profile_pic.toString());
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return null;
            }
            SharedPreferences account = getSharedPreferences("login_data", 0);
            SharedPreferences.Editor editor = account.edit();
            bundle.putString("idFacebook", id);
            if(object.has("first_name")) {
                bundle.putString("first_name", object.getString("first_name"));
                editor.putString("username", object.getString("first_name"));
            }
            if(object.has("last_name")) {
                bundle.putString("last_name", object.getString("last_name"));
                editor.putString("userlastname", object.getString("last_name"));
            }
            editor.commit();
        } catch (Exception e) {
            Log.d(TAG_FACE, "BUNDLE Exception : "+e.toString());
        }
        return bundle;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        accessTokenTracker.stopTracking();
        deleteAccessToken();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        accessTokenTracker.stopTracking();
        deleteAccessToken();
    }

}

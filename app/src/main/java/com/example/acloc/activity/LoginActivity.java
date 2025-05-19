package com.example.acloc.activity;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;

import com.example.acloc.MainActivity;
import com.example.acloc.api.LocationApiClient;
import com.example.acloc.model.User;
import com.example.acloc.service.AuthService;
import com.example.acloc.utility.DialogUtils;
import com.example.acloc.utility.Helper;
import com.example.acloc.utility.SharedPref;
import com.ieslamar.acloc.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonObject;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = LoginActivity.class.getSimpleName();
    private RelativeLayout rlLogin;
    private TextInputEditText etUsername, etPassword;
    private AppCompatButton btnLogin;
    private Context context;
    private TextView tvRegisterRedirect;
    private User entity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        checkForLoginStatusAndNavigate();
    }

    private void checkForLoginStatusAndNavigate() {
        boolean loginStatus = SharedPref.getIsLoggedIn(this);
        boolean isTokenValid = SharedPref.isAccessTokenValid(this);

        if (loginStatus && isTokenValid) {
            Helper.goTo(this, MainActivity.class);
            finish();
        } else {
            // Optionally force logout if token expired
            SharedPref.setIsLoggedIn(this, false);
            SharedPref.setAccessToken(this, ""); // clear token
            setContentView(R.layout.activity_login);
            initToolbar();
            initUI();
            initObj();
            initListeners();
        }
    }


    private void initToolbar() {
        try {
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                getSupportActionBar().setTitle(getString(R.string.LOGIN));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in LoginActivity", e);
        }
    }

    private void initUI() {
        rlLogin = findViewById(R.id.rlLogin);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegisterRedirect = findViewById(R.id.tvRegisterRedirect);
    }

    private void initListeners() {
        btnLogin.setOnClickListener(this);
        tvRegisterRedirect.setOnClickListener(this);
    }

    private void initObj() {
        context = this;
        entity = new User();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.tvRegisterRedirect) {
            onClickRegisterRedirect();
        } else if (id == R.id.btnLogin) {
            onClickBtnLogin();
        }
    }

    private void onClickRegisterRedirect() {
        Helper.goToAndFinish(LoginActivity.this, RegisterActivity.class);
    }

    private void onClickBtnLogin() {
        View[] views = {etUsername, etPassword};
        if (Helper.isEmptyFieldValidation(views)) {
            setInputDataToEntity();
            loginUserWithRetrofit();
        }
    }

    private void setInputDataToEntity() {
        entity.setUsername(Helper.getStringFromInput(etUsername));
        entity.setPassword(Helper.getStringFromInput(etPassword));
    }

    private void loginUserWithRetrofit() {
        DialogUtils.showLoadingDialog(context, getString(R.string.Please_wait));

        JsonObject jsonParam = new JsonObject();
        jsonParam.addProperty("username", entity.getUsername());
        jsonParam.addProperty("password", entity.getPassword());

        AuthService authService = LocationApiClient.getInstance().getAuthService();
        Call<JsonObject> call = authService.loginUser(jsonParam);

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                DialogUtils.dismissDialog();

                if (response.isSuccessful() && response.body() != null) {
                    JsonObject json = response.body();

                    if (json.has("user")) {
                        JsonObject userObj = json.getAsJsonObject("user");

                        // 1. Extract user data from _data
                        if (userObj.has("_data")) {
                            JsonObject data = userObj.getAsJsonObject("_data");

                            String uuid = data.has("uuid") ? data.get("uuid").getAsString() : "";
                            String username = data.has("username") ? data.get("username").getAsString() : "";
                            String email = data.has("email") ? data.get("email").getAsString() : "";
                            String role = data.has("role") ? data.get("role").getAsString() : "";

                            SharedPref.setUuid(context, uuid);
                            SharedPref.setUsername(context, username);
                            SharedPref.setUserEmail(context, email);
                            SharedPref.setRole(context, role);
                        }

                        // 2. Extract tokens
                        String accessToken = userObj.has("accessToken") ? userObj.get("accessToken").getAsString() : "";
                        String refreshToken = userObj.has("refreshToken") ? userObj.get("refreshToken").getAsString() : "";

                        if (!accessToken.isEmpty()) SharedPref.setAccessToken(context, accessToken);
                        if (!refreshToken.isEmpty())
                            SharedPref.setRefreshToken(context, refreshToken);

                        Helper.makeSnackBar(rlLogin, getString(R.string.Login_Successful));
                        SharedPref.setIsLoggedIn(context, true);
                        Helper.goToAndFinish(LoginActivity.this, MainActivity.class);
                    } else {
                        Helper.makeSnackBar(rlLogin, getString(R.string.Invalid_Credentials_Please_try_again));
                    }
                } else {
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Log.e(TAG, "Login Error: " + errorBody);
                            Helper.makeSnackBar(rlLogin, getString(R.string.Login_failed) + errorBody + " Try again");
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Login error reading response", e);
                        Helper.makeSnackBar(rlLogin, e.toString() + " Try again");
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                DialogUtils.dismissDialog();
                Log.e(TAG, "Login API call failed", t);
                Helper.makeSnackBar(rlLogin, "Server error: " + t.toString() + " Try again");
            }
        });
    }
}

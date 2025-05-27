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

import com.ieslamar.acloc.R;
import com.example.acloc.api.LocationApiClient;
import com.example.acloc.model.User;
import com.example.acloc.service.AuthService;
import com.example.acloc.utility.Constants;
import com.example.acloc.utility.DialogUtils;
import com.example.acloc.utility.Helper;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonObject;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = RegisterActivity.class.getSimpleName();
    private RelativeLayout rlRegister;
    private TextInputEditText etName, etEmail, etPassword;
    private AppCompatButton btnRegister;
    private TextView tvLoginRedirect;
    private Context context;
    private User entity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initToolbar();
        initUI();
        initObj();
        initListeners();
    }

    private void initToolbar() {
        try {
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                getSupportActionBar().setTitle(getString(R.string.REGISTER));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in RegisterActivity", e);
        }
    }

    private void initUI() {
        rlRegister = findViewById(R.id.rlRegister);
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvLoginRedirect = findViewById(R.id.tvLoginRedirect);
    }

    private void initListeners() {
        btnRegister.setOnClickListener(this);
        tvLoginRedirect.setOnClickListener(this);
    }

    private void initObj() {
        context = this;
        entity = new User();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.tvLoginRedirect) {
            onClickLoginRedirect();
        } else if (id == R.id.btnRegister) {
            onClickBtnRegister();
        }
    }

    private void onClickLoginRedirect() {
        Helper.goToAndFinish(RegisterActivity.this, LoginActivity.class);
    }

    private void onClickBtnRegister() {
        View[] views = {etName, etEmail, etPassword};
        if (Helper.isEmptyFieldValidation(context, views) && Helper.isEmailValid(context, etEmail) && Helper.isPasswordValid(context, etPassword)) {
            setInputDataToEntity();
            registerUser();
        }
    }

    private void setInputDataToEntity() {
        entity.setUsername(Helper.getStringFromInput(etName));
        entity.setEmail(Helper.getStringFromInput(etEmail));
        entity.setPassword(Helper.getStringFromInput(etPassword));
    }

    private void registerUser() {
        DialogUtils.showLoadingDialog(context, getString(R.string.Please_wait));

        Call<JsonObject> call = getJsonObjectCall();

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                DialogUtils.dismissDialog();

                if (response.isSuccessful() && response.body() != null) {
                    JsonObject json = response.body();

                    if (json.has("code") && json.get("code").getAsInt() == 409) {
                        Helper.makeSnackBar(rlRegister, getString(R.string.User_already_exists_Please_login));
                        return;
                    }

                    if (json.has("_data") && json.getAsJsonObject("_data").has("message")) {
                        Helper.makeSnackBar(rlRegister, getString(R.string.Registration_Successful));
                        Helper.goToAndFinish(RegisterActivity.this, LoginActivity.class);
                    } else {
                        Helper.makeSnackBar(rlRegister, getString(R.string.Invalid_Credentials_Please_try_again));
                    }
                } else {
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Log.e(TAG, "Error Response: " + errorBody);
                            Helper.makeSnackBar(rlRegister, "Server Error " + errorBody + " Try again");
                        } else {
                            Helper.makeSnackBar(rlRegister, "Server Error " + " Try again");
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading errorBody", e);
                        Helper.makeSnackBar(rlRegister, "API Failure: " + e.toString() + " Try again");
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                DialogUtils.dismissDialog();
                Log.e(TAG, "API Failure", t);
                Helper.makeSnackBar(rlRegister, "API Failure: " + t.toString() + " Try again");
            }
        });
    }

    private Call<JsonObject> getJsonObjectCall() {
        JsonObject jsonParam = new JsonObject();
        jsonParam.addProperty("username", entity.getUsername());
        jsonParam.addProperty("email", entity.getEmail());
        jsonParam.addProperty("password", entity.getPassword());
        jsonParam.addProperty("fk_role", Constants.ADMIN); //Default Admin for now (BUT acc to api it will be Viewer)

        AuthService authService = LocationApiClient.getInstance().getAuthService();
        Call<JsonObject> call = authService.registerUser(jsonParam);
        return call;
    }
}

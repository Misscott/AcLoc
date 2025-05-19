package com.example.acloc.dialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;

import com.ieslamar.acloc.R;
import com.example.acloc.api.LocationApiClient;
import com.example.acloc.service.UserService;
import com.example.acloc.model.User;
import com.example.acloc.utility.DialogUtils;
import com.example.acloc.utility.Helper;
import com.example.acloc.utility.SharedPref;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonObject;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AlertViewOrUpdateProfileDialog implements View.OnClickListener {
    private static final String TAG = AlertViewOrUpdateProfileDialog.class.getSimpleName();
    private View alertView;
    private TextInputEditText etUsername, etEmail;
    private AppCompatButton btnCancel, btnUpdate;
    private Dialog dialog;
    private Context context;
    private User entity;

    public AlertViewOrUpdateProfileDialog(Context context) {
        this.context = context;
        this.entity = new User();
    }

    public Dialog openProfileDialog() {
        try {
            try {
                LayoutInflater layoutInflater = ((Activity) context).getLayoutInflater();
                alertView = layoutInflater.inflate(R.layout.alert_dialog_profile, null);
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
                if (alertView.getParent() != null) {
                    ((ViewGroup) alertView.getParent()).removeView(alertView);
                }
                alertBuilder.setView(alertView);

                initUI();
                setListeners();
                dialog = alertBuilder.create();

                //get user profile
                getProfile();

            } catch (Exception e) {
                Log.e(TAG, "Error in AlertProfileDialog: ", e);
            }

            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error in AlertProfileDialog: ", e);
        }
        return dialog;
    }

    private void setDataToText() {
        try {
            if (entity != null) {
                etUsername.setText(entity.getUsername());
                etEmail.setText(entity.getEmail());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in setDataToText: ", e);
        }
    }

    private void getProfile() {
        entity.setUsername(SharedPref.getUserName(context));
        entity.setEmail(SharedPref.getUserEmail(context));
        setDataToText();
    }

    private void initUI() {
        etUsername = alertView.findViewById(R.id.etUsername);
        etEmail = alertView.findViewById(R.id.etEmail);
        btnCancel = alertView.findViewById(R.id.btnCancel);
        btnUpdate = alertView.findViewById(R.id.btnUpdate);
    }

    private void setListeners() {
        btnCancel.setOnClickListener(this);
        btnUpdate.setOnClickListener(this);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btnCancel) {
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
        } else if (id == R.id.btnUpdate) {
            onClickBtnUpdate();
        }
    }

    private void onClickBtnUpdate() {
        View[] views = {etUsername, etEmail};
        if (Helper.isEmptyFieldValidation(views) && Helper.isEmailValid(etEmail)) {
            setInputDataToEntity();
            String uuid = SharedPref.getUserUid(context);
            updateUserWithRetrofit(uuid, entity.getUsername(), entity.getEmail());
        }
    }

    private void setInputDataToEntity() {
        entity.setUsername(Helper.getStringFromInput(etUsername));
        entity.setEmail(Helper.getStringFromInput(etEmail));
    }

    private void updateUserWithRetrofit(String uuid, String username, String email) {
        DialogUtils.showLoadingDialog(context, context.getString(R.string.Updating));

        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("username", username);
        jsonBody.addProperty("email", email);

        String accessToken = SharedPref.getAccessToken(context);
        String bearerToken = "Bearer " + accessToken;

        UserService userService = LocationApiClient.getInstance().getUserService();
        Call<JsonObject> call = userService.updateUser(bearerToken, uuid, jsonBody);

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                DialogUtils.dismissDialog();

                if (response.isSuccessful() && response.body() != null) {
                    JsonObject responseBody = response.body();
                    if (responseBody.toString().contains("User modified")) {
                        Helper.makeSnackBar(alertView, context.getString(R.string.Profile_Updated_Successfully));
                        SharedPref.setUsername(context, username);
                        SharedPref.setUserEmail(context, email);
                    } else {
                        Helper.makeSnackBar(alertView, context.getString(R.string.Try_again_later));
                    }
                } else {
                    try {
                        if (response.errorBody() != null) {
                            String error = response.errorBody().string();
                            Log.e(TAG, "Update Failed: " + error);
                            Helper.makeSnackBar(alertView, context.getString(R.string.Update_Failed));
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error parsing error response", e);
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                DialogUtils.dismissDialog();
                Log.e(TAG, "Update API failed", t);
                Helper.makeSnackBar(alertView, context.getString(R.string.Something_went_wrong_Try_again));
            }
        });
    }
}

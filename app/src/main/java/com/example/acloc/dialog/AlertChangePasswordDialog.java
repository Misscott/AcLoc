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
import com.example.acloc.service.AuthService;
import com.example.acloc.service.UserService;
import com.example.acloc.utility.DialogUtils;
import com.example.acloc.utility.Helper;
import com.example.acloc.utility.SharedPref;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AlertChangePasswordDialog implements View.OnClickListener {
    private static final String TAG = AlertChangePasswordDialog.class.getSimpleName();
    private View alertView;
    private TextInputEditText etOldPassword, etNewPassword;
    private AppCompatButton btnCancel, btnUpdate;
    private Dialog dialog;
    private Context context;

    public AlertChangePasswordDialog(Context context) {
        this.context = context;
    }

    public Dialog openChangePasswordDialog() {
        try {
            try {
                LayoutInflater layoutInflater = ((Activity) context).getLayoutInflater();
                alertView = layoutInflater.inflate(R.layout.alert_dialog_change_password, null);
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
                if (alertView.getParent() != null) {
                    ((ViewGroup) alertView.getParent()).removeView(alertView);
                }
                alertBuilder.setView(alertView);

                initUI();
                setListeners();
                dialog = alertBuilder.create();

            } catch (Exception e) {
                Log.e(TAG, "Error in AlertChangePasswordDialog: ", e);
            }

            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error in AlertChangePasswordDialog: ", e);
        }
        return dialog;
    }

    private void initUI() {
        etOldPassword = alertView.findViewById(R.id.etOldPassword);
        etNewPassword = alertView.findViewById(R.id.etNewPassword);
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
        View[] views = {etOldPassword, etNewPassword};
        if (Helper.isEmptyFieldValidation(context, views) && Helper.isPasswordValid(context, etNewPassword)) {
            String username = SharedPref.getUserName(context);
            String oldPassword = Helper.getStringFromInput(etOldPassword);
            String newPassword = Helper.getStringFromInput(etNewPassword);
            String uuid = SharedPref.getUserUid(context);

            verifyOldPasswordRetrofit(uuid, username, oldPassword, newPassword);
        }
    }

    private void verifyOldPasswordRetrofit(String uuid, String username, String oldPassword, String newPassword) {
        DialogUtils.showLoadingDialog(context, context.getString(R.string.Verifying_old_password));

        JsonObject loginBody = new JsonObject();
        loginBody.addProperty("username", username);
        loginBody.addProperty("password", oldPassword);

        AuthService authService = LocationApiClient.getInstance().getAuthService();
        Call<JsonObject> call = authService.verifyOldPassword(loginBody);

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                DialogUtils.dismissDialog();

                if (response.isSuccessful() && response.body() != null && response.body().has("user")) {
                    // Proceed to change password
                    changePasswordRetrofit(uuid, newPassword);
                } else {
                    Helper.makeSnackBar(alertView, context.getString(R.string.Invalid_old_password));
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                DialogUtils.dismissDialog();
                Log.e(TAG, "Old password verification failed", t);
                Helper.makeSnackBar(alertView, context.getString(R.string.Network_error_Try_again));
            }
        });
    }

    private void changePasswordRetrofit(String uuid, String newPassword) {
        DialogUtils.showLoadingDialog(context, context.getString(R.string.Changing_password));

        JsonObject body = new JsonObject();
        body.addProperty("password", newPassword);

        String token = "Bearer " + SharedPref.getAccessToken(context);

        UserService userService = LocationApiClient.getInstance().getUserService();
        Call<JsonObject> call = userService.changePassword(token, uuid, body);

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                DialogUtils.dismissDialog();

                if (response.isSuccessful() && response.body() != null &&
                        response.body().toString().contains("User modified")) {
                    Helper.makeSnackBar(alertView, context.getString(R.string.Password_updated_successfully));
                } else {
                    Helper.makeSnackBar(alertView, context.getString(R.string.Password_update_failed));
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                DialogUtils.dismissDialog();
                Log.e(TAG, "Password change failed", t);
                Helper.makeSnackBar(alertView, context.getString(R.string.Network_error_Try_again));
            }
        });
    }
}

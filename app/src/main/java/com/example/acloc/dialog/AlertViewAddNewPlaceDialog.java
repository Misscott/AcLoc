package com.example.acloc.dialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;

import com.example.acloc.MainActivity;
import com.ieslamar.acloc.R;
import com.example.acloc.api.LocationApiClient;
import com.example.acloc.service.PlaceService;
import com.example.acloc.model.Place;
import com.example.acloc.utility.DialogUtils;
import com.example.acloc.utility.Helper;
import com.example.acloc.utility.SharedPref;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AlertViewAddNewPlaceDialog implements View.OnClickListener {
    private static final String TAG = AlertViewAddNewPlaceDialog.class.getSimpleName();
    private View alertView;
    private ImageView ivPlacePhoto;
    private TextInputEditText etPlaceName, etLatitude, etLongitude, etAddress, etPlaceDescription;
    private AppCompatButton btnSubmit;
    private Dialog dialog;
    private Context context;
    private double lat, lng;
    private String placeName, address;

    private Place entity;
    private String place_uuid;


    public AlertViewAddNewPlaceDialog(Context context, double lat, double lng, String placeName, String address, String description, String uuid) {
        this.context = context;
        this.lat = lat;
        this.lng = lng;
        this.placeName = placeName;
        this.address = address;
        this.entity = new Place();
        this.entity.setDescription(description);
        this.entity.setName(placeName);
        this.place_uuid = uuid;
    }

    public Dialog openPlaceDialog() {
        try {
            try {
                LayoutInflater layoutInflater = ((Activity) context).getLayoutInflater();
                alertView = layoutInflater.inflate(R.layout.alert_dialog_add_new_place, null);
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
                if (alertView.getParent() != null) {
                    ((ViewGroup) alertView.getParent()).removeView(alertView);
                }
                alertBuilder.setView(alertView);

                initUI();
                setListeners();
                setDataToText();

                dialog = alertBuilder.create();
            } catch (Exception e) {
                Log.e(TAG, "Error in AlertPlaceDialog: ", e);
            }

            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error in AlertPlaceDialog: ", e);
        }
        return dialog;
    }

    private void initUI() {
        ivPlacePhoto = alertView.findViewById(R.id.ivPlacePhoto);
        etPlaceName = alertView.findViewById(R.id.etPlaceName);
        etLatitude = alertView.findViewById(R.id.etLatitude);
        etLongitude = alertView.findViewById(R.id.etLongitude);
        etAddress = alertView.findViewById(R.id.etAddress);
        etPlaceDescription = alertView.findViewById(R.id.etPlaceDescription);
        btnSubmit = alertView.findViewById(R.id.btnSubmit);
    }

    private void setListeners() {
        btnSubmit.setOnClickListener(this);
    }

    private void setDataToText() {
        etPlaceName.setText(String.valueOf(placeName));
        etLatitude.setText(String.valueOf(lat));
        etLongitude.setText(String.valueOf(lng));
        etAddress.setText(String.valueOf(address));
        etPlaceDescription.setText(entity.getDescription());
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btnSubmit) {
            onClickBtnSubmit();
        }
    }

    private void onClickBtnSubmit() {
        View[] views = {etPlaceName, etLatitude, etLongitude, etAddress, etPlaceDescription};
        if (Helper.isEmptyFieldValidation(views)) {
            setInputDataToEntity();

            if (place_uuid != null && !place_uuid.isEmpty()) {
                // Update existing place
                updatePlaceRetrofit(place_uuid, entity.getName(), entity.getDescription(),
                        entity.getAddress(), entity.getLatitude(), entity.getLongitude(), entity.getCreatedBy());
            } else {
                // Insert new place
                insertPlaceRetrofit(entity.getName(), entity.getDescription(),
                        entity.getAddress(), entity.getLatitude(), entity.getLongitude(), entity.getCreatedBy());
            }
        }
    }

    private void setInputDataToEntity() {
        entity.setName(Helper.getStringFromInput(etPlaceName));
        entity.setDescription(Helper.getStringFromInput(etPlaceDescription));
        entity.setAddress(Helper.getStringFromInput(etAddress));
        entity.setLatitude(Helper.getStringFromInput(etLatitude));
        entity.setLongitude(Helper.getStringFromInput(etLongitude));
        entity.setCreatedBy(SharedPref.getUserUid(context));
        entity.setUuid(place_uuid);
    }

    private void insertPlaceRetrofit(String name, String description, String address,
                                     String latitude, String longitude, String createdBy) {
        DialogUtils.showLoadingDialog(context, "Please wait...");

        JsonObject placeBody = new JsonObject();
        placeBody.addProperty("name", name);
        placeBody.addProperty("description", description);
        placeBody.addProperty("address", address);
        placeBody.addProperty("latitude", latitude);
        placeBody.addProperty("longitude", longitude);
        placeBody.addProperty("createdBy", createdBy);

        String token = "Bearer " + SharedPref.getAccessToken(context);

        PlaceService placeService = LocationApiClient.getInstance().getPlaceService();
        Call<JsonObject> call = placeService.insertPlace(token, placeBody);

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                DialogUtils.dismissDialog();
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject responseBody = response.body();
                    JsonObject data = responseBody.getAsJsonObject("_data");
                    if (data != null && data.has("places")) {
                        JsonObject placeObject = data.getAsJsonArray("places").get(0).getAsJsonObject();
                        place_uuid = placeObject.get("uuid").getAsString();
                        Log.d(TAG, "Place UUID: " + place_uuid);
                        Helper.makeSnackBar(alertView, "Place inserted successfully!");
                        alertView.postDelayed(() -> {
                            Helper.goToAndFinish(alertView.getContext(), MainActivity.class);
                        }, 500);

                    } else {
                        Helper.makeSnackBar(alertView, "Failed to extract place UUID.");
                    }
                } else {
                    Helper.makeSnackBar(alertView, "Insert failed. Server error.");
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                DialogUtils.dismissDialog();
                Log.e(TAG, "Insert Place Error: ", t);
                Helper.makeSnackBar(alertView, context.getString(R.string.Network_error_Try_again));
            }
        });
    }

    private void updatePlaceRetrofit(String uuid, String name, String description, String address,
                                     String latitude, String longitude, String createdBy) {

        DialogUtils.showLoadingDialog(context, "Updating place...");

        JsonObject placeBody = new JsonObject();
        placeBody.addProperty("name", name);
        placeBody.addProperty("description", description);
        placeBody.addProperty("address", address);
        placeBody.addProperty("latitude", latitude);
        placeBody.addProperty("longitude", longitude);
        placeBody.addProperty("createdBy", createdBy);

        String token = "Bearer " + SharedPref.getAccessToken(context);

        PlaceService placeService = LocationApiClient.getInstance().getPlaceService();
        Call<JsonObject> call = placeService.updatePlace(token, uuid, placeBody);

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                DialogUtils.dismissDialog();
                if (response.isSuccessful()) {
                    Helper.makeSnackBar(alertView, "Place updated successfully!");
                    alertView.postDelayed(() -> {
                        Helper.goToAndFinish(alertView.getContext(), MainActivity.class);
                    }, 500);
                } else {
                    Helper.makeSnackBar(alertView, "Update failed. Server error.");
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                DialogUtils.dismissDialog();
                Log.e(TAG, "Update Place Error: ", t);
                Helper.makeSnackBar(alertView, context.getString(R.string.Network_error_Try_again));
            }
        });
    }
}

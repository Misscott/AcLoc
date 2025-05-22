package com.example.acloc.activity;

import static com.example.acloc.utility.Constants.BASE_URL;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;

import com.example.acloc.utility.UploadManager;
import com.ieslamar.acloc.R;
import com.example.acloc.api.LocationApiClient;
import com.example.acloc.model.Place;
import com.example.acloc.service.PlaceService;
import com.example.acloc.utility.Constants;
import com.example.acloc.utility.DialogUtils;
import com.example.acloc.utility.Helper;
import com.example.acloc.utility.SharedPref;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonObject;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddNewPlaceActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = AddNewPlaceActivity.class.getSimpleName();
    private RelativeLayout rlAddPlace;
    private ImageView ivPlacePhoto;
    private TextInputEditText etPlaceName, etLatitude, etLongitude, etAddress, etPlaceDescription;
    private AppCompatButton btnSubmit;
    private Dialog dialog;
    private Context context;
    private double lat, lng;
    private String placeName, address;

    private Place entity;
    private String place_uuid;
    private static final int PICK_IMAGE_REQUEST = 200;

    private Uri selectedImageUri;
    private String imageUrl;
    private String jsonString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_new_place);
        initToolbar();
        initUI();
        initObj();
        setListeners();
        loadIntentData();
        setDataToText();
    }

    private void initToolbar() {
        try {
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                getSupportActionBar().setTitle(getString(R.string.Add_Place));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in AddNewPlaceActivity", e);
        }
    }

    private void initUI() {
        rlAddPlace = findViewById(R.id.rlAddPlace);
        ivPlacePhoto = findViewById(R.id.ivPlacePhoto);
        etPlaceName = findViewById(R.id.etPlaceName);
        etLatitude = findViewById(R.id.etLatitude);
        etLongitude = findViewById(R.id.etLongitude);
        etAddress = findViewById(R.id.etAddress);
        etPlaceDescription = findViewById(R.id.etPlaceDescription);
        btnSubmit = findViewById(R.id.btnSubmit);
    }

    private void loadIntentData() {
        entity = (Place) getIntent().getSerializableExtra(Constants.PLACE);
        place_uuid = entity.getUuid(); // setting place uuid first
        jsonString = entity.getImage();
        if (entity != null) {
            Log.d(TAG,
                    "PLACE ENTITY DATA \n   " +
                            "Place uuid: " + entity.getUuid() + "\n " +
                            "Place name: " + entity.getName() + "\n " +
                            "Place address: " + entity.getAddress() + "\n " +
                            "Place created by: " + entity.getCreatedBy() + "\n " +
                            "Place latitude: " + entity.getLatitude() + "\n " +
                            "Place longitude: " + entity.getLongitude() + "\n " +
                            "Place description: " + entity.getDescription() + "\n " +
                            "Place photo: " + entity.getImage() + "\n "
            );
        }
    }

    private void setListeners() {
        ivPlacePhoto.setOnClickListener(this);
        btnSubmit.setOnClickListener(this);
    }

    private void initObj() {
        context = this;
        entity = new Place();
    }

    private void setDataToText() {
        etPlaceName.setText(entity.getName());
        etLatitude.setText(entity.getLatitude());
        etLongitude.setText(entity.getLongitude());
        etAddress.setText(entity.getAddress());
        etPlaceDescription.setText(entity.getDescription());

        if (entity.getImage() != null && !entity.getImage().isEmpty()) {
            String rawImg = entity.getImage();
            try {
                JSONArray array = new JSONArray(rawImg);
                String imageUrl = array.getString(0); // Get first element in the array
                Picasso.get()
                        .load(imageUrl)
                        .into(ivPlacePhoto, new com.squareup.picasso.Callback() {
                            @Override
                            public void onSuccess() {
                                Picasso.get().load(imageUrl).into(ivPlacePhoto);
                            }

                            @Override
                            public void onError(Exception e) {
                                // Error loading image 404 -- load default
                                Picasso.get().load(R.drawable.logo_add_location).into(ivPlacePhoto);
                            }
                        });
            } catch (JSONException e) {
                Log.e(TAG, "ERROR: " + e.toString());
                Picasso.get().load(R.drawable.logo_add_location).into(ivPlacePhoto);
            }
        } else { //if image is null
            Picasso.get().load(R.drawable.logo_add_location).into(ivPlacePhoto);
        }
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.ivPlacePhoto) {
            onClickIvPlacePhoto();
        } else if (id == R.id.btnSubmit) {
            onClickBtnSubmit();
        }
    }

    private void onClickIvPlacePhoto() {
        // Open the gallery to select an image
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*"); // Restrict to images only
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();

            // Upload the image
            uploadImageToServer(selectedImageUri);

            // Load image using Picasso
            Picasso.get().load(selectedImageUri).into(ivPlacePhoto);
        }
    }

    private void uploadImageToServer(Uri imageUri) {
        UploadManager.uploadImage(this, imageUri, new UploadManager.UploadCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject json = new JSONObject(response);
                    if (json.getBoolean("success")) {
                        String filename = json.getJSONObject("file").getString("filename");
                        imageUrl = BASE_URL + "public/" + filename;
                        jsonString = "[\"" + imageUrl + "\"]";
                        Helper.makeSnackBar(rlAddPlace, "Image uploaded successfully");
                        Log.d(TAG, "JsonString: " + jsonString);
                    } else {
                        Helper.makeSnackBar(rlAddPlace, "Upload failed");
                    }
                } catch (JSONException e) {
                    Helper.makeSnackBar(rlAddPlace, "Response parsing error");
                    Log.e(TAG, "Failed to parse JSON", e);
                }
            }
            @Override
            public void onError(String message) {
                Helper.makeSnackBar(rlAddPlace, "Upload failed: " + message);
                Log.e(TAG, "Upload error:" + message);
            }
        });
    }

    private void onClickBtnSubmit() {
        View[] views = {etPlaceName, etLatitude, etLongitude, etAddress, etPlaceDescription};
        if (Helper.isEmptyFieldValidation(views)) {
            setInputDataToEntity();

            if (place_uuid != null && !place_uuid.isEmpty()) {
                // Update existing place
                updatePlaceRetrofit(place_uuid, entity.getName(), entity.getDescription(),
                        entity.getAddress(), entity.getLatitude(), entity.getLongitude(), entity.getCreatedBy(), entity.getImage());
            } else {
                // Insert new place
                insertPlaceRetrofit(entity.getName(), entity.getDescription(),
                        entity.getAddress(), entity.getLatitude(), entity.getLongitude(), entity.getCreatedBy(), entity.getImage());
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
        entity.setImage(jsonString);
    }

    private void insertPlaceRetrofit(String name, String description, String address,
                                     String latitude, String longitude, String createdBy, String jsonString) {
        DialogUtils.showLoadingDialog(context, context.getString(R.string.Please_wait));

        JsonObject placeBody = new JsonObject();
        placeBody.addProperty("name", name);
        placeBody.addProperty("description", description);
        placeBody.addProperty("address", address);
        placeBody.addProperty("latitude", latitude);
        placeBody.addProperty("longitude", longitude);
        placeBody.addProperty("createdBy", createdBy);
        placeBody.addProperty("images", this.jsonString);

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
                        Helper.makeSnackBar(rlAddPlace, context.getString(R.string.Place_inserted_successfully));
                        rlAddPlace.postDelayed(() -> {
                            finish();
                        }, 500);

                    } else {
                        Helper.makeSnackBar(rlAddPlace, context.getString(R.string.Failed_to_extract_place_Try_again));
                    }
                } else {
                    Helper.makeSnackBar(rlAddPlace, context.getString(R.string.Insert_failed_Server_error));
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                DialogUtils.dismissDialog();
                Log.e(TAG, "Insert Place Error: ", t);
                Helper.makeSnackBar(rlAddPlace, context.getString(R.string.Network_error_Try_again));
            }
        });
    }

    private void updatePlaceRetrofit(String uuid, String name, String description, String address,
                                     String latitude, String longitude, String createdBy, String jsonString) {

        DialogUtils.showLoadingDialog(context, getString(R.string.Updating_place));

        JsonObject placeBody = new JsonObject();
        placeBody.addProperty("name", name);
        placeBody.addProperty("description", description);
        placeBody.addProperty("address", address);
        placeBody.addProperty("latitude", latitude);
        placeBody.addProperty("longitude", longitude);
        placeBody.addProperty("createdBy", createdBy);
        placeBody.addProperty("images", jsonString);

        String token = "Bearer " + SharedPref.getAccessToken(context);

        PlaceService placeService = LocationApiClient.getInstance().getPlaceService();
        Call<JsonObject> call = placeService.updatePlace(token, uuid, placeBody);

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                DialogUtils.dismissDialog();
                if (response.isSuccessful()) {
                    Helper.makeSnackBar(rlAddPlace, getString(R.string.Place_updated_successfully));

                    if (entity != null) {
                        entity.setUuid(uuid);
                        entity.setName(name);
                        entity.setDescription(description);
                        entity.setAddress(address);
                        entity.setLatitude(latitude);
                        entity.setLongitude(longitude);
                        entity.setCreatedBy(createdBy);
                        entity.setImage(jsonString);
                    }

                    rlAddPlace.postDelayed(() -> {
                        Helper.goToAndFinish(context, PlaceDetailActivity.class, Constants.PLACE, entity);
//                        finish();
                    }, 500);
                } else {
                    Helper.makeSnackBar(rlAddPlace, getString(R.string.Update_failed_Server_error_Try_again));
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                DialogUtils.dismissDialog();
                Log.e(TAG, "Update Place Error: ", t);
                Helper.makeSnackBar(rlAddPlace, context.getString(R.string.Network_error_Try_again));
            }
        });
    }
}

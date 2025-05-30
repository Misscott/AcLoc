package com.example.acloc.activity;

import static com.example.acloc.utility.Constants.BASE_URL;

import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.acloc.adapter.ReportTypeAdapter;
import com.example.acloc.api.LocationApiClient;
import com.example.acloc.model.Place;
import com.example.acloc.model.Report;
import com.example.acloc.model.ReportType;
import com.example.acloc.service.PlaceService;
import com.example.acloc.service.ReportService;
import com.example.acloc.service.ReportTypeService;
import com.example.acloc.utility.Constants;
import com.example.acloc.utility.DialogUtils;
import com.example.acloc.utility.Helper;
import com.example.acloc.utility.SharedPref;
import com.example.acloc.utility.UploadManager;
import com.google.android.flexbox.AlignItems;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ieslamar.acloc.R;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddReportActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String TAG = AddReportActivity.class.getSimpleName();
    private Context context;

    // UI Components
    private TextInputEditText etDescription;
    private TextView tvPlaceName, tvPlaceAddress;
    private ImageView ivReportPhoto, ivThumbsUp, ivThumbsAverage, ivThumbsDown;
    private MaterialButton btnSubmitReport;
    private RecyclerView rvReportTypes;

    // Data
    private Place placeEntity;
    private Report reportEntity;
    private String place_uuid;
    private int reportRating = 0;
    private String report_uuid;
    private boolean isEditMode = false;

    // Report Types - Now supporting multiple selection
    private ReportTypeAdapter reportTypeAdapter;
    private List<ReportType> reportTypesList = new ArrayList<>();
    private List<String> selectedReportTypeUuids = new ArrayList<>();

    // Multiple Images handling
    private static final int PICK_IMAGE_REQUEST = 100;
    private List<String> imageUrls = new ArrayList<>();
    private int currentImageIndex = 0;
    private String jsonString = "[]";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_report);

        initUI();
        resetThumbsColors();
        loadIntentData();
        initListener();
        initObj();
        loadReportTypes();
    }

    private void initUI() {
        // Text inputs
        etDescription = findViewById(R.id.etDescription);

        // Place info
        tvPlaceName = findViewById(R.id.tvPlaceName);
        tvPlaceAddress = findViewById(R.id.tvPlaceAddress);

        // Rating thumbs
        ivThumbsUp = findViewById(R.id.ivThumbsUp);
        ivThumbsAverage = findViewById(R.id.ivThumbsAverage);
        ivThumbsDown = findViewById(R.id.ivThumbsDown);

        // Photo and submit
        ivReportPhoto = findViewById(R.id.ivReportPhoto);
        btnSubmitReport = findViewById(R.id.btnSubmitReport);

        // Report types
        rvReportTypes = findViewById(R.id.rvReportTypes);

        // Setup RecyclerView for report types
        FlexboxLayoutManager layoutManager = new FlexboxLayoutManager(this);
        layoutManager.setFlexDirection(FlexDirection.ROW);
        layoutManager.setFlexWrap(FlexWrap.WRAP);
        layoutManager.setJustifyContent(JustifyContent.FLEX_START);
        layoutManager.setAlignItems(AlignItems.FLEX_START);
        rvReportTypes.setLayoutManager(layoutManager);

        reportTypeAdapter = new ReportTypeAdapter(this, reportTypesList);
        rvReportTypes.setAdapter(reportTypeAdapter);
        reportTypeAdapter.setOnReportTypeClickListener(new ReportTypeAdapter.OnReportTypeClickListener() {
            @Override
            public void onReportTypeClick(ReportType reportType, int position, boolean isSelected) {
                // Individual click handling if needed
                Log.d(TAG, "Report type " + reportType.getName() + " " + (isSelected ? "selected" : "deselected"));
            }

            @Override
            public void onSelectionChanged(List<ReportType> selectedReportTypes) {
                // Update selected UUIDs list
                selectedReportTypeUuids.clear();
                for (ReportType reportType : selectedReportTypes) {
                    selectedReportTypeUuids.add(reportType.getUuid());
                }
            }
        });
        rvReportTypes.setAdapter(reportTypeAdapter);
    }

    private void loadIntentData() {
        Intent intent = getIntent();

        // Get place data (if available)
        placeEntity = (Place) intent.getSerializableExtra(Constants.PLACE);
        if (placeEntity != null) {
            place_uuid = placeEntity.getUuid();
            populatePlaceInfo();
        }

        // Get report data (for editing existing reports)
        reportEntity = (Report) intent.getSerializableExtra(Constants.REPORT);
        if (reportEntity != null) {
            isEditMode = true;
            report_uuid = reportEntity.getUuid();
            place_uuid = reportEntity.getPlaceUuid();
            setDataToEditText();

            // If we don't have place entity but have place_uuid, fetch place data
            if (placeEntity == null && place_uuid != null) {
                fetchPlaceData(place_uuid);
            }
        }

        // Get place UUID directly (if passed without Place object)
        String passedPlaceUuid = intent.getStringExtra("place_uuid");
        if (passedPlaceUuid != null && place_uuid == null) {
            place_uuid = passedPlaceUuid;
            fetchPlaceData(place_uuid);
        }

        // Validate that we have the necessary data
        if (place_uuid == null || place_uuid.isEmpty()) {
            Helper.makeSnackBar(findViewById(android.R.id.content),
                    getString(R.string.error_no_place_selected));
            finish();
        }
    }

    private void fetchPlaceData(String placeUuid) {
        String token = "Bearer " + SharedPref.getAccessToken(this);
        PlaceService placeService = LocationApiClient.getInstance().getPlaceService();

        Call<JsonObject> call = placeService.getPlaceFromUuid(token, placeUuid);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    parsePlaceResponse(response.body());
                } else {
                    Log.e(TAG, "Failed to load place data: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Failed to load place data", t);
            }
        });
    }

    private void parsePlaceResponse(JsonObject responseBody) {
        JsonObject data = responseBody.getAsJsonObject("_data");
        if (data != null && data.has("places")) {
            JsonArray placesArray = data.getAsJsonArray("places");
            if (placesArray.size() > 0) {
                JsonObject placeObject = placesArray.get(0).getAsJsonObject();

                // Create place entity from response
                placeEntity = new Place();
                placeEntity.setUuid(placeObject.get("uuid").getAsString());
                placeEntity.setName(placeObject.get("name").getAsString());
                if (placeObject.has("address") && !placeObject.get("address").isJsonNull()) {
                    placeEntity.setAddress(placeObject.get("address").getAsString());
                }

                populatePlaceInfo();
            }
        }
    }

    private void populatePlaceInfo() {
        if (placeEntity != null) {
            tvPlaceName.setText(placeEntity.getName());
            if (placeEntity.getAddress() != null && !placeEntity.getAddress().isEmpty()) {
                tvPlaceAddress.setText(placeEntity.getAddress());
            } else {
                tvPlaceAddress.setText(getString(R.string.address_not_available));
            }
        }
    }

    private void setDataToEditText() {
        if (reportEntity != null) {
            etDescription.setText(reportEntity.getDescription());

            // Reset colors first
            resetThumbsColors();

            // Set rating using constants
            int rating = reportEntity.getReportRating();
            if (rating == Constants.BAD_RATING) { // 1
                onClickThumbsDown();
            } else if (rating == Constants.AVERAGE_RATING) { // 2
                onClickThumbsAverage();
            } else if (rating == Constants.GOOD_RATING) { // 3
                onClickThumbsUp();
            }

            // Load existing images
            loadReportImages();

            // Set place info from report if available
            if (reportEntity.getPlaceName() != null) {
                tvPlaceName.setText(reportEntity.getPlaceName());
            }

            // Set selected report types (multiple)
            selectedReportTypeUuids.clear();
            if (reportEntity.getReportTypeUuids() != null) {
                selectedReportTypeUuids.addAll(reportEntity.getReportTypeUuids());
            }

            // establish selection
            if (!reportTypesList.isEmpty()) {
                rvReportTypes.post(() -> {
                    reportTypeAdapter.setSelectedReportTypes(selectedReportTypeUuids);
                });
            }
        }
    }

    private void loadReportImages() {
        if (reportEntity.getImage() != null && !reportEntity.getImage().isEmpty()) {
            String rawImg = reportEntity.getImage();
            try {
                JSONArray array = new JSONArray(rawImg);
                imageUrls.clear();

                for (int i = 0; i < array.length(); i++) {
                    String imageUrl = array.getString(i);
                    imageUrls.add(imageUrl);
                }

                // Load first image in the ImageView
                if (!imageUrls.isEmpty()) {
                    String imageUrl = imageUrls.get(0);
                    Picasso.get()
                            .load(imageUrl)
                            .into(ivReportPhoto, new com.squareup.picasso.Callback() {
                                @Override
                                public void onSuccess() {
                                    Picasso.get().load(imageUrl).into(ivReportPhoto);
                                }

                                @Override
                                public void onError(Exception e) {
                                    Picasso.get().load(R.drawable.ic_add_light).into(ivReportPhoto);
                                }
                            });

                    jsonString = rawImg; // Keep original JSON string
                    updateImageCounter();
                }
            } catch (JSONException e) {
                Log.e(TAG, "ERROR: " + e.toString());
                Picasso.get().load(R.drawable.ic_add_light).into(ivReportPhoto);
            }
        } else {
            Picasso.get().load(R.drawable.ic_add_light).into(ivReportPhoto);
        }
    }

    private void updateImageCounter() {
        if (imageUrls.size() > 1) {
            Log.d(TAG, "Images: " + (currentImageIndex + 1) + " of " + imageUrls.size());
        }
    }

    private void loadReportTypes() {
        String token = "Bearer " + SharedPref.getAccessToken(this);
        ReportTypeService reportTypeService = LocationApiClient.getInstance().getReportTypeService();

        Call<JsonObject> call = reportTypeService.getReportTypes(token, null, null);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    parseReportTypesResponse(response.body());
                } else {
                    Log.e(TAG, "Failed to load report types: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Failed to load report types", t);
            }
        });
    }

    private void parseReportTypesResponse(JsonObject responseBody) {
        JsonObject data = responseBody.getAsJsonObject("_data");
        if (data != null && data.has("report_types")) {
            reportTypesList.clear();
            JsonArray reportTypesArray = data.getAsJsonArray("report_types");

            for (JsonElement element : reportTypesArray) {
                JsonObject typeObject = element.getAsJsonObject();
                ReportType reportType = new ReportType();
                reportType.setUuid(typeObject.get("uuid").getAsString());
                reportType.setName(typeObject.get("name").getAsString());
                reportTypesList.add(reportType);
            }

            Log.d(TAG, "Loaded " + reportTypesList.size() + " report types");

            reportTypeAdapter.notifyDataSetChanged();

            if (isEditMode && reportEntity != null && !reportEntity.getReportTypeUuids().isEmpty()) {
                rvReportTypes.post(() -> {
                    reportTypeAdapter.setSelectedReportTypes(reportEntity.getReportTypeUuids());
                });
            }
        }
    }

    private void initListener() {
        ivReportPhoto.setOnClickListener(this);
        ivThumbsUp.setOnClickListener(this);
        ivThumbsAverage.setOnClickListener(this);
        ivThumbsDown.setOnClickListener(this);
        btnSubmitReport.setOnClickListener(this);

        // Long click to cycle through multiple images
        ivReportPhoto.setOnLongClickListener(v -> {
            if (imageUrls.size() > 1) {
                currentImageIndex = (currentImageIndex + 1) % imageUrls.size();
                Picasso.get()
                        .load(imageUrls.get(currentImageIndex))
                        .placeholder(R.drawable.ic_add_light)
                        .error(R.drawable.ic_add_light)
                        .into(ivReportPhoto);
                updateImageCounter();
                Helper.makeSnackBar(findViewById(android.R.id.content),
                        "Imagen " + (currentImageIndex + 1) + " de " + imageUrls.size());
                return true;
            }
            return false;
        });
    }

    private void initObj() {
        context = this;
        if (reportEntity == null) {
            reportEntity = new Report();
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.ivReportPhoto) {
            onClickIvReportPhoto();
        } else if (id == R.id.ivThumbsUp) {
            onClickThumbsUp();
        } else if (id == R.id.ivThumbsAverage) {
            onClickThumbsAverage();
        } else if (id == R.id.ivThumbsDown) {
            onClickThumbsDown();
        } else if (id == R.id.btnSubmitReport) {
            onClickBtnSubmit();
        }
    }

    private void onClickIvReportPhoto() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {

            if (data.getClipData() != null) {
                // Multiple images selected
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    Uri imageUri = data.getClipData().getItemAt(i).getUri();
                    uploadImageToServer(imageUri);
                }
            } else if (data.getData() != null) {
                // Single image selected
                Uri selectedImageUri = data.getData();
                uploadImageToServer(selectedImageUri);
                Picasso.get().load(selectedImageUri).into(ivReportPhoto);
            }
        }
    }

    private void uploadImageToServer(Uri imageUri) {
        btnSubmitReport.setClickable(false);
        UploadManager.uploadImage(this, imageUri, new UploadManager.UploadCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject json = new JSONObject(response);
                    if (json.getBoolean("success")) {
                        String filename = json.getJSONObject("file").getString("filename");
                        String imageUrl = BASE_URL + "public/" + filename;

                        // Add to images list
                        imageUrls.add(imageUrl);

                        // Update JSON string
                        updateJsonString();

                        // Load first image if this is the first one
                        if (imageUrls.size() == 1) {
                            Picasso.get().load(imageUrl).into(ivReportPhoto);
                        }

                        updateImageCounter();
                        Helper.makeSnackBar(findViewById(android.R.id.content),
                                getString(R.string.image_uploaded_successfully) + " (" + imageUrls.size() + ")");
                    } else {
                        Helper.makeSnackBar(findViewById(android.R.id.content),
                                getString(R.string.upload_failed));
                    }
                } catch (JSONException e) {
                    Helper.makeSnackBar(findViewById(android.R.id.content),
                            getString(R.string.response_parsing_error));
                    Log.e(TAG, "Failed to parse JSON", e);
                }
                btnSubmitReport.setClickable(true);
            }

            @Override
            public void onError(String message) {
                Helper.makeSnackBar(findViewById(android.R.id.content),
                        "Upload failed: " + message);
                Log.e(TAG, "Upload error: " + message);
                btnSubmitReport.setClickable(true);
            }
        });
    }

    private void updateJsonString() {
        try {
            JSONArray jsonArray = new JSONArray();
            for (String imageUrl : imageUrls) {
                jsonArray.put(imageUrl);
            }
            jsonString = jsonArray.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error creating images JSON", e);
            jsonString = "[]";
        }
    }

    private void onClickThumbsUp() {
        resetThumbsColors();
        ivThumbsUp.setColorFilter(ContextCompat.getColor(this, R.color.green), PorterDuff.Mode.SRC_IN);
        reportRating = Constants.GOOD_RATING; // 3
    }

    private void onClickThumbsAverage() {
        resetThumbsColors();
        ivThumbsAverage.setColorFilter(ContextCompat.getColor(this, R.color.yellow), PorterDuff.Mode.SRC_IN);
        reportRating = Constants.AVERAGE_RATING; // 2
    }

    private void onClickThumbsDown() {
        resetThumbsColors();
        ivThumbsDown.setColorFilter(ContextCompat.getColor(this, R.color.red), PorterDuff.Mode.SRC_IN);
        reportRating = Constants.BAD_RATING; // 1
    }

    private void resetThumbsColors() {
        ivThumbsUp.setColorFilter(ContextCompat.getColor(this, android.R.color.transparent), PorterDuff.Mode.SRC_IN);
        ivThumbsAverage.setColorFilter(ContextCompat.getColor(this, android.R.color.transparent), PorterDuff.Mode.SRC_IN);
        ivThumbsDown.setColorFilter(ContextCompat.getColor(this, android.R.color.transparent), PorterDuff.Mode.SRC_IN);
    }

    private void onClickBtnSubmit() {
        View[] views = {etDescription};
        if (Helper.isEmptyFieldValidation(context, views) && isValidateRating()) {
            setInputDataToEntity();

            if (report_uuid != null && !report_uuid.isEmpty()) {
                // Update existing report
                updateReportRetrofit(
                        report_uuid,
                        place_uuid,
                        SharedPref.getUserUuid(context),
                        String.valueOf(reportEntity.getReportRating()),
                        reportEntity.getDescription(),
                        reportEntity.getCreatedBy(),
                        reportEntity.getImage(),
                        selectedReportTypeUuids // Now passing array
                );
            } else {
                // Create new report
                insertReportRetrofit(
                        place_uuid,
                        SharedPref.getUserUuid(context),
                        String.valueOf(reportEntity.getReportRating()),
                        reportEntity.getDescription(),
                        reportEntity.getCreatedBy(),
                        reportEntity.getImage(),
                        selectedReportTypeUuids // Now passing array
                );
            }
        }
    }

    private void setInputDataToEntity() {
        reportEntity.setDescription(Helper.getStringFromInput(etDescription));
        reportEntity.setReportRating(reportRating);
        reportEntity.setCreatedBy(SharedPref.getUserUuid(context));
        reportEntity.setImage(jsonString);
        reportEntity.setReportTypeUuids(new ArrayList<>(selectedReportTypeUuids)); // Set multiple UUIDs
        reportEntity.setPlaceUuid(place_uuid);
    }

    private boolean isValidateRating() {
        if (reportRating != 0) {
            return true;
        } else {
            Helper.makeSnackBar(findViewById(android.R.id.content), getString(R.string.Please_select_rating));
            return false;
        }
    }

    private void insertReportRetrofit(String placeUuid, String userUuid, String rating,
                                      String description, String createdBy, String jsonString,
                                      List<String> reportTypeUuids) {

        DialogUtils.showLoadingDialog(context, getString(R.string.Please_wait));

        JsonObject reportBody = new JsonObject();
        reportBody.addProperty("place_uuid", placeUuid);
        reportBody.addProperty("user_uuid", userUuid);
        reportBody.addProperty("rating", rating);
        reportBody.addProperty("description", description);
        reportBody.addProperty("createdBy", createdBy);
        reportBody.addProperty("images", jsonString);

        // Add report type UUIDs as array (if any selected)
        if (reportTypeUuids != null && !reportTypeUuids.isEmpty()) {
            JsonArray reportTypesArray = new JsonArray();
            for (String uuid : reportTypeUuids) {
                reportTypesArray.add(uuid);
            }
            reportBody.add("report_type_uuid", reportTypesArray);
        }

        String token = "Bearer " + SharedPref.getAccessToken(context);

        ReportService reportService = LocationApiClient.getInstance().getReportService();
        Call<JsonObject> call = reportService.insertReport(token, reportBody);

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                DialogUtils.dismissDialog();
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject responseBody = response.body();
                    JsonObject data = responseBody.getAsJsonObject("_data");
                    if (data != null && data.has("reports")) {
                        JsonObject reportObject = data.getAsJsonArray("reports").get(0).getAsJsonObject();
                        String report_uuid = reportObject.get("uuid").getAsString();
                        Log.d(TAG, "Report UUID: " + report_uuid);

                        String message = getString(R.string.Report_submitted_successfully);

                        Helper.makeSnackBar(findViewById(android.R.id.content), message);
                        findViewById(android.R.id.content).postDelayed(() -> {
                            setResult(RESULT_OK);
                            finish();
                        }, 500);
                    } else {
                        Log.d(TAG, "Failed to extract report UUID.");
                        Helper.makeSnackBar(findViewById(android.R.id.content), getString(R.string.Something_went_wrong_Try_again));
                    }
                } else {
                    Helper.makeSnackBar(findViewById(android.R.id.content), getString(R.string.Report_submission_failed_Try_again));
                    Log.e(TAG, "Insert Report Error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                DialogUtils.dismissDialog();
                Log.e(TAG, "Insert Report Failure: ", t);
                Helper.makeSnackBar(findViewById(android.R.id.content), context.getString(R.string.Network_error_Try_again));
            }
        });
    }

    private void updateReportRetrofit(String uuid, String placeUuid, String userUuid, String rating,
                                      String description, String createdBy, String jsonString,
                                      List<String> reportTypeUuids) {

        DialogUtils.showLoadingDialog(context, getString(R.string.Updating_report));

        JsonObject reportBody = new JsonObject();
        reportBody.addProperty("place_uuid", placeUuid);
        reportBody.addProperty("user_uuid", userUuid);
        reportBody.addProperty("rating", rating);
        reportBody.addProperty("description", description);
        reportBody.addProperty("createdBy", createdBy);
        reportBody.addProperty("images", jsonString);

        // Add report type UUIDs as array (if any selected)
        if (reportTypeUuids != null && !reportTypeUuids.isEmpty()) {
            JsonArray reportTypesArray = new JsonArray();
            for (String reportUuid : reportTypeUuids) {
                reportTypesArray.add(reportUuid);
            }
            reportBody.add("report_type_uuid", reportTypesArray);
        }

        String token = "Bearer " + SharedPref.getAccessToken(context);

        ReportService reportService = LocationApiClient.getInstance().getReportService();
        Call<JsonObject> call = reportService.updateReport(token, uuid, reportBody);

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                DialogUtils.dismissDialog();
                if (response.isSuccessful()) {
                    String message = getString(R.string.Report_updated_successfully);

                    Helper.makeSnackBar(findViewById(android.R.id.content), message);
                    findViewById(android.R.id.content).postDelayed(() -> {
                        setResult(RESULT_OK);
                        finish();
                    }, 500);
                } else {
                    Helper.makeSnackBar(findViewById(android.R.id.content), getString(R.string.Update_failed_Server_error_Try_again));
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                DialogUtils.dismissDialog();
                Log.e(TAG, "Update Report Error: ", t);
                Helper.makeSnackBar(findViewById(android.R.id.content), context.getString(R.string.Network_error_Try_again));
            }
        });
    }
}

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
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.example.acloc.api.LocationApiClient;
import com.example.acloc.model.Place;
import com.example.acloc.model.Report;
import com.example.acloc.service.ReportService;
import com.example.acloc.utility.Constants;
import com.example.acloc.utility.DialogUtils;
import com.example.acloc.utility.Helper;
import com.example.acloc.utility.SharedPref;
import com.example.acloc.utility.UploadManager;
import com.ieslamar.acloc.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonObject;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddReportActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String TAG = AddReportActivity.class.getSimpleName();
    private RelativeLayout rlAddReport;
    private TextInputEditText etDescription, etPlaceName;
    private ImageView ivReportPhoto;
    private ImageView ivThumbsUp, ivThumbsAverage, ivThumbsDown;
    private AppCompatButton btnSubmit;
    private Context context;
    private Place placeEntity;
    private Report reportEntity;
    private String report_type_uuid, place_uuid;
    private int reportRating;
    private String report_uuid;

    private static final int PICK_IMAGE_REQUEST = 100;

    private Uri selectedImageUri;
    private String imageUrl;
    private String jsonString;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_report);

        initToolbar();
        initUI();
        loadIntentData();
        initListener();
        initObj();
    }

    private void initToolbar() {
        try {
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                getSupportActionBar().setTitle(getString(R.string.Add_Report));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in AddReportActivity", e);
        }
    }

    private void initUI() {
        rlAddReport = findViewById(R.id.rlAddReport);
        ivReportPhoto = findViewById(R.id.ivReportPhoto);
        etPlaceName = findViewById(R.id.etPlaceName);
        etDescription = findViewById(R.id.etDescription);
        ivThumbsUp = findViewById(R.id.ivThumbsUp);
        ivThumbsAverage = findViewById(R.id.ivThumbsAverage);
        ivThumbsDown = findViewById(R.id.ivThumbsDown);
        btnSubmit = findViewById(R.id.btnSubmit);
    }

    private void loadIntentData() {
        placeEntity = (Place) getIntent().getSerializableExtra(Constants.PLACE);
        if (placeEntity != null) {
            place_uuid = placeEntity.getUuid();
            etPlaceName.setText(placeEntity.getName()); //Just to display place name in report
        }

        reportEntity = (Report) getIntent().getSerializableExtra(Constants.REPORT);
        if (reportEntity != null) {
            report_uuid = reportEntity.getUuid(); // setting place uuid first
            place_uuid = reportEntity.getPlaceUuid();
            Log.d(TAG, "" +
                    "place uuid: " + reportEntity.getPlaceUuid() +
                    "\n fkplace " + reportEntity.getFkPlace());
            setDataToEditText();
        }
    }

    private void setDataToEditText() {
        etPlaceName.setText(reportEntity.getPlaceName());
        etDescription.setText(reportEntity.getDescription());
        int rating = reportEntity.getReportRating();
        if (rating == 1) {
            ivThumbsDown.setColorFilter(ContextCompat.getColor(this, R.color.red), PorterDuff.Mode.SRC_IN);
            ivThumbsUp.setColorFilter(null); // reset the other
            ivThumbsAverage.setColorFilter(null);
            reportRating = Constants.BAD_RATING; //1
        } else if (rating == 2) {
            ivThumbsAverage.setColorFilter(ContextCompat.getColor(this, R.color.yellow), PorterDuff.Mode.SRC_IN);
            ivThumbsUp.setColorFilter(null); // reset the others
            ivThumbsDown.setColorFilter(null);
            reportRating = Constants.AVERAGE_RATING; //2
        } else if (rating == 3) {
            ivThumbsUp.setColorFilter(ContextCompat.getColor(this, R.color.green), PorterDuff.Mode.SRC_IN);
            ivThumbsDown.setColorFilter(null); // reset the other
            ivThumbsAverage.setColorFilter(null);
            reportRating = Constants.GOOD_RATING; //3
        }
        if (reportEntity.getImage() != null && !reportEntity.getImage().isEmpty()) {
            String rawImg = reportEntity.getImage();
            try {
                JSONArray array = new JSONArray(rawImg);
                String imageUrl = array.getString(0); // Get first element in the array
                Picasso.get()
                        .load(imageUrl)
                        .into(ivReportPhoto, new com.squareup.picasso.Callback() {
                            @Override
                            public void onSuccess() {
                                Picasso.get().load(imageUrl).into(ivReportPhoto);
                            }

                            @Override
                            public void onError(Exception e) {
                                // Error loading image 404 -- load default
                                Picasso.get().load(R.drawable.logo_add_location).into(ivReportPhoto);
                            }
                        });
            } catch (JSONException e) {
                Log.e(TAG, "ERROR: " + e.toString());
                Picasso.get().load(R.drawable.logo_add_location).into(ivReportPhoto);
            }
        } else { //if image is null
            Picasso.get().load(R.drawable.logo_add_location).into(ivReportPhoto);
        }
    }

    private void initListener() {
        ivThumbsUp.setOnClickListener(this);
        ivThumbsAverage.setOnClickListener(this);
        ivThumbsDown.setOnClickListener(this);
        ivReportPhoto.setOnClickListener(this);
        btnSubmit.setOnClickListener(this);
    }

    private void initObj() {
        context = this;
        reportEntity = new Report();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.ivThumbsUp) {
            onClickThumbsUp();
        } else if (id == R.id.ivThumbsAverage) {
            onClickThumbsAverage();
        } else if (id == R.id.ivThumbsDown) {
            onClickThumbsDown();
        } else if (id == R.id.ivReportPhoto) {
            onClickIvReportPhoto();
        } else if (id == R.id.btnSubmit) {
            onClickBtnSubmit();
        }
    }

    private void onClickIvReportPhoto() {
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
            Picasso.get().load(selectedImageUri).into(ivReportPhoto);

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
                        Helper.makeSnackBar(rlAddReport, getString(R.string.image_uploaded_successfully));
                        btnSubmit.setClickable(true);
                    } else {
                        Helper.makeSnackBar(rlAddReport, getString(R.string.upload_failed));
                        btnSubmit.setClickable(true);
                    }
                } catch (JSONException e) {
                    Helper.makeSnackBar(rlAddReport, getString(R.string.response_parsing_error));
                    btnSubmit.setClickable(true);
                    Log.e(TAG, "Failed to parse JSON", e);
                }
            }

            @Override
            public void onError(String message) {
                Helper.makeSnackBar(rlAddReport, "Upload failed: " + message);
                Log.e(TAG, "Upload error:" + message);
                btnSubmit.setClickable(true);
            }
        });
    }

    private void onClickThumbsUp() {
        ivThumbsUp.setColorFilter(ContextCompat.getColor(this, R.color.green), PorterDuff.Mode.SRC_IN);
        ivThumbsDown.setColorFilter(null); // reset the other
        ivThumbsAverage.setColorFilter(null);
        reportRating = Constants.GOOD_RATING; //3
    }

    private void onClickThumbsAverage() {
        ivThumbsAverage.setColorFilter(ContextCompat.getColor(this, R.color.yellow), PorterDuff.Mode.SRC_IN);
        ivThumbsUp.setColorFilter(null); // reset the others
        ivThumbsDown.setColorFilter(null);
        reportRating = Constants.AVERAGE_RATING; //2
    }

    private void onClickThumbsDown() {
        ivThumbsDown.setColorFilter(ContextCompat.getColor(this, R.color.red), PorterDuff.Mode.SRC_IN);
        ivThumbsUp.setColorFilter(null); // reset the other
        ivThumbsAverage.setColorFilter(null);
        reportRating = Constants.BAD_RATING; //1

    }

    private void onClickBtnSubmit() {
        View[] views = {etPlaceName, etDescription};
        if (Helper.isEmptyFieldValidation(context, views) && isValidateRating()) {
            setInputDataToEntity();

            if (report_uuid != null && !report_uuid.isEmpty()) {
                // Update existing report
                updateReportRetrofit(
                        report_uuid,
                        place_uuid,
                        SharedPref.getUserUid(context),
                        String.valueOf(reportEntity.getReportRating()),
                        reportEntity.getDescription(),
                        reportEntity.getCreatedBy(),
                        reportEntity.getImage()
                );
            } else {
                insertReportRetrofit(
                        placeEntity.getUuid(),
                        SharedPref.getUserUid(context),
                        String.valueOf(reportEntity.getReportRating()),
                        reportEntity.getDescription(),
                        reportEntity.getCreatedBy(),
                        reportEntity.getImage()
                );
            }
        }
    }

    private void setInputDataToEntity() {
        reportEntity.setDescription(Helper.getStringFromInput(etDescription));
        reportEntity.setReportRating(reportRating);
        reportEntity.setCreatedBy(SharedPref.getUserUid(context));
        reportEntity.setImage(jsonString);
    }

    private boolean isValidateRating() {
        if (reportRating != 0) {
            return true;
        } else {
            Helper.makeSnackBar(rlAddReport, getString(R.string.Please_select_rating));
            return false;
        }
    }

    private void insertReportRetrofit(String placeUuid, String userUuid, String rating,
                                      String description, String createdBy, String jsonString) {

        DialogUtils.showLoadingDialog(context, getString(R.string.Please_wait));

        JsonObject reportBody = new JsonObject();
        reportBody.addProperty("place_uuid", placeUuid);
        reportBody.addProperty("user_uuid", userUuid);
        reportBody.addProperty("rating", rating);
        reportBody.addProperty("description", description);
        reportBody.addProperty("createdBy", createdBy);
        reportBody.addProperty("images", jsonString);

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
                        Helper.makeSnackBar(rlAddReport, getString(R.string.Report_submitted_successfully));
                        rlAddReport.postDelayed(() -> {
                            finish();
                        }, 500);
                    } else {
                        Log.d(TAG, "Failed to extract report UUID.");
                        Helper.makeSnackBar(rlAddReport, getString(R.string.Something_went_wrong_Try_again));
                    }
                } else {
                    Helper.makeSnackBar(rlAddReport, getString(R.string.Report_submission_failed_Try_again));
                    Log.e(TAG, "Insert Report Error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                DialogUtils.dismissDialog();
                Log.e(TAG, "Insert Report Failure: ", t);
                Helper.makeSnackBar(rlAddReport, context.getString(R.string.Network_error_Try_again));
            }
        });
    }

    private void updateReportRetrofit(String uuid, String placeUuid, String userUuid, String rating,
                                      String description, String createdBy, String jsonString) {

        DialogUtils.showLoadingDialog(context, getString(R.string.Updating_report));

        JsonObject reportBody = new JsonObject();
        reportBody.addProperty("place_uuid", placeUuid);
        reportBody.addProperty("user_uuid", userUuid);
        reportBody.addProperty("rating", rating);
        reportBody.addProperty("description", description);
        reportBody.addProperty("createdBy", createdBy);
        reportBody.addProperty("images", jsonString);

        String token = "Bearer " + SharedPref.getAccessToken(context);

        // Using the new ReportService through LocationApiClient
        ReportService reportService = LocationApiClient.getInstance().getReportService();
        Call<JsonObject> call = reportService.updateReport(token, uuid, reportBody);

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                DialogUtils.dismissDialog();
                if (response.isSuccessful()) {
                    Helper.makeSnackBar(rlAddReport, getString(R.string.Report_updated_successfully));
                    rlAddReport.postDelayed(() -> {
                        finish(); //to go back to the previous activity
                    }, 500);
                } else {
                    Helper.makeSnackBar(rlAddReport, getString(R.string.Update_failed_Server_error_Try_again));
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                DialogUtils.dismissDialog();
                Log.e(TAG, "Update Place Error: ", t);
                Helper.makeSnackBar(rlAddReport, context.getString(R.string.Network_error_Try_again));
            }
        });
    }
}

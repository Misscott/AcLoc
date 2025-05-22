package com.example.acloc.activity;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import com.ieslamar.acloc.R;
import com.example.acloc.adapter.PlaceReportsAdapter;
import com.example.acloc.api.LocationApiClient;
import com.example.acloc.model.Place;
import com.example.acloc.model.Report;
import com.example.acloc.service.FavoriteService;
import com.example.acloc.service.ReportService;
import com.example.acloc.utility.Constants;
import com.example.acloc.utility.DialogUtils;
import com.example.acloc.utility.Helper;
import com.example.acloc.utility.SharedPref;
import com.google.android.material.textview.MaterialTextView;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlaceDetailActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String TAG = PlaceDetailActivity.class.getSimpleName();
    private RelativeLayout rlPlaceDetails;
    private MaterialTextView etPlaceName, etAddress, etPlaceDescription;
    private ImageView ivFavorite, ivEdit, ivPlacePhoto;
    private AppCompatButton btnSubmit;
    private Dialog dialog;
    private Context context;
    private Place placeEntity;
    private String place_uuid;
    private boolean isFavorite = false; // Track favorite current state default false
    private RecyclerView rvReports;
    private TextView tvNoData;
    private PlaceReportsAdapter adapter;
    private Report reportEntity;
    private List<Report> reportList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_detail);

        initToolbar();
        initUI();
        loadIntentData(); // load data
        initListener(); // set fav, edit btn On Clicks
        initObj();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) {
            setDataVisibility(false);
            adapter.clearReports(); // clear and notify
        }

        if (context != null) loadData();
    }


    private void setDataVisibility(boolean isDataAvailable) {
        if (isDataAvailable) {
            rvReports.setVisibility(View.VISIBLE);
            tvNoData.setVisibility(View.GONE);
        } else {
            tvNoData.setVisibility(View.VISIBLE);
            //            rvReports.setVisibility(View.GONE);

        }
    }

    private void initToolbar() {
        try {
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                getSupportActionBar().setTitle(getString(R.string.Place_Details));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in PlaceDetailActivity", e);
        }
    }

    private void initUI() {
        rlPlaceDetails = findViewById(R.id.rlPlaceDetails);
        etPlaceName = findViewById(R.id.etPlaceName);
        etPlaceDescription = findViewById(R.id.etPlaceDescription);
        etAddress = findViewById(R.id.etAddress);
        ivFavorite = findViewById(R.id.ivFavorite);
        ivPlacePhoto = findViewById(R.id.ivPlacePhoto);
        ivEdit = findViewById(R.id.ivEdit);
        btnSubmit = findViewById(R.id.btnSubmit);
        rvReports = findViewById(R.id.rvReports);
        tvNoData = findViewById(R.id.tvNoData);
    }

    private void initListener() {
        ivFavorite.setOnClickListener(this);
        ivEdit.setOnClickListener(this);
        btnSubmit.setOnClickListener(this);
    }

    private void initObj() {
        context = this;
        checkIfPlaceIsFavorite(SharedPref.getUserUid(context), place_uuid);
    }

    private void loadIntentData() {
        placeEntity = (Place) getIntent().getSerializableExtra(Constants.PLACE);
        if (placeEntity != null) {
            place_uuid = placeEntity.getUuid();
            etPlaceName.setText(placeEntity.getName());
            etAddress.setText(placeEntity.getAddress());
            etPlaceDescription.setText(placeEntity.getDescription());
            if (placeEntity.getImage() != null && !placeEntity.getImage().isEmpty()) {
                String rawImg = placeEntity.getImage();
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
            Log.d(TAG,
                    "Place uuid: " + placeEntity.getUuid() + "\n " +
                            "Place name: " + placeEntity.getName() + "\n " +
                            "Place name: " + placeEntity.getAddress() + "\n " +
                            "Place created by: " + placeEntity.getCreatedBy() + "\n " +
                            "Place latitude: " + placeEntity.getLatitude() + "\n " +
                            "Place longitude: " + placeEntity.getLongitude() + "\n " +
                            "Place description: " + placeEntity.getDescription() + "\n " +
                            "Place Photo: " + placeEntity.getImage() + "\n "
            );
        }
    }

    private void loadData() {
        try {
            if (reportList == null) {
                reportList = new ArrayList<>();
            }
            getReportsByPlaceUuid(placeEntity.getUuid());
        } catch (Exception e) {
            Log.e(TAG, "Error in PlaceDetailActivity", e);
            Helper.makeSnackBar(rlPlaceDetails, getString(R.string.Something_went_wrong_Try_again));
        }
    }

    private void setUpRecyclerView(List<Report> latestReports) {
        try {
            if (adapter != null) {
                adapter.updateReportsList(latestReports);
            } else {
                adapter = new PlaceReportsAdapter(context, latestReports);
                rvReports.setAdapter(adapter);
                rvReports.setLayoutManager(Helper.getVerticalManager(context));
                adapter.notifyDataSetChanged();
            }
            if (latestReports != null && !latestReports.isEmpty()) {
                setDataVisibility(true); // Data available
            } else {
                setDataVisibility(false); // No data
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in PlaceDetailActivity", e);
            Helper.showToast(context, getString(R.string.Something_went_wrong_Try_again));
            setDataVisibility(false);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.ivFavorite) {
            onClickFavorite();
        } else if (id == R.id.ivEdit) {
            onClickBtnEdit();
        } else if (id == R.id.btnSubmit) {
            onClickBtnSubmit();
        }
    }

    private void onClickBtnEdit() {
        Helper.goToAndFinish(context, AddNewPlaceActivity.class, Constants.PLACE, placeEntity);
    }

    private void onClickBtnSubmit() {
        Helper.goTo(context, AddReportActivity.class, Constants.PLACE, placeEntity);
    }

    private void onClickFavorite() {
        if (isFavorite) {
            removePlaceFromFavorites(SharedPref.getUserUid(context), place_uuid);
        } else {
            addPlaceToFavorites(SharedPref.getUserUid(context), place_uuid);
        }
    }

    private void addPlaceToFavorites(String userUuid, String placeUuid) {
        DialogUtils.showLoadingDialog(context, getString(R.string.Adding_to_favorites));

        JsonObject body = new JsonObject();
        body.addProperty("place_uuid", placeUuid);
        Log.d(TAG, "Request Body: " + body.toString());

        String token = "Bearer " + SharedPref.getAccessToken(context);

        FavoriteService favoriteService = LocationApiClient.getInstance().getFavoriteService();
        Call<JsonObject> call = favoriteService.restorePlaceToFavorites(token, userUuid, body);

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                DialogUtils.dismissDialog();
                if (response.isSuccessful()) {
                    Log.d(TAG, "Place added to fav!");
                    isFavorite = true; // Update state
                    updateFavoriteIcon(); // Update icon
                    Helper.makeSnackBar(rlPlaceDetails, getString(R.string.Place_added_to_favorites));
                } else {
                    Log.d(TAG, "Failed to add favorite!");
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Log.e(TAG, "Error Body: " + errorBody);

                            // Try restoring if it's a duplicate (409 Conflict)
                            if (response.code() == 409 && errorBody.contains("ER_DUP_ENTRY")) {
                                Log.d(TAG, "Duplicate entry detected, trying to restore...");
                                restorePlaceToFavorites(userUuid, placeUuid);
                                return;
                            }
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "ERROR: ", e);
                    }

                    Helper.makeSnackBar(rlPlaceDetails, getString(R.string.Failed_to_add_favorite_Server_error));
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                DialogUtils.dismissDialog();
                Log.e(TAG, "Add Favorite Error: ", t);
                Helper.makeSnackBar(rlPlaceDetails, context.getString(R.string.Network_error_Try_again));
            }
        });
    }

    private void restorePlaceToFavorites(String userUuid, String placeUuid) {
        DialogUtils.showLoadingDialog(context, getString(R.string.Restoring_favorite));
        JsonObject body = new JsonObject();
        body.addProperty("place_uuid", placeUuid);

        String token = "Bearer " + SharedPref.getAccessToken(context);

        FavoriteService favoriteService = LocationApiClient.getInstance().getFavoriteService();
        Call<JsonObject> call = favoriteService.restorePlaceToFavorites(token, userUuid, body);

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                DialogUtils.dismissDialog();
                if (response.isSuccessful()) {
                    Log.d(TAG, "Place restored to favorites!");
                    Helper.makeSnackBar(rlPlaceDetails, getString(R.string.Place_restored_to_favorites));
                } else {
                    Log.e(TAG, "Restore failed. Response code: " + response.code());
                    Helper.makeSnackBar(rlPlaceDetails, getString(R.string.Failed_to_restore_favorite));
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                DialogUtils.dismissDialog();
                Log.e(TAG, "Restore Favorite Error: ", t);
                Helper.makeSnackBar(rlPlaceDetails, context.getString(R.string.Network_error_Try_again));
            }
        });
    }

    private void removePlaceFromFavorites(String userUuid, String placeUuid) {
        DialogUtils.showLoadingDialog(context, getString(R.string.Removing_from_favorites));
        String token = "Bearer " + SharedPref.getAccessToken(context);

        FavoriteService favoriteService = LocationApiClient.getInstance().getFavoriteService();
        Call<Void> call = favoriteService.removePlaceFromFavorites(token, userUuid, placeUuid);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                DialogUtils.dismissDialog();
                if (response.isSuccessful()) {
                    Log.d(TAG, "Place removed from fav!");
                    isFavorite = false; //  Update state
                    updateFavoriteIcon(); //  Update icon
                    Helper.makeSnackBar(rlPlaceDetails, getString(R.string.Removed_from_favorites));
                } else {
                    Log.d(TAG, "Failed to remove from fav");
                    Helper.makeSnackBar(rlPlaceDetails, getString(R.string.Failed_to_remove_from_favorites));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                DialogUtils.dismissDialog();
                Log.e(TAG, "Remove Favorite Error: ", t);
                Helper.makeSnackBar(rlPlaceDetails, context.getString(R.string.Network_error_Try_again));
            }
        });
    }

    private void checkIfPlaceIsFavorite(String userUuid, String currentPlaceUuid) {
        DialogUtils.showLoadingDialog(context, getString(R.string.Checking_favorite_status));

        String token = "Bearer " + SharedPref.getAccessToken(context);

        FavoriteService favoriteService = LocationApiClient.getInstance().getFavoriteService();
        Call<JsonObject> call = favoriteService.getFavoritePlaces(token, userUuid);

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                DialogUtils.dismissDialog();
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject body = response.body();
                    JsonArray favoritesArray = body.getAsJsonObject("_data").getAsJsonArray("favorites");

                    isFavorite = false;
                    for (JsonElement item : favoritesArray) {
                        JsonObject favoriteObj = item.getAsJsonObject();
                        String favPlaceUuid = favoriteObj.get("place_uuid").getAsString();
                        if (favPlaceUuid.equals(currentPlaceUuid)) {
                            isFavorite = true;
                            break;
                        }
                    }
                    updateFavoriteIcon();

                } else {
                    Log.e(TAG, "Failed to fetch favorites. Code: " + response.code());
                    isFavorite = false;
                    updateFavoriteIcon();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                DialogUtils.dismissDialog();
                Log.e(TAG, "Error fetching favorite places", t);
                isFavorite = false;
                updateFavoriteIcon();
            }
        });
    }

    private void updateFavoriteIcon() {
        if (isFavorite) {
            ivFavorite.setImageResource(R.drawable.ic_favorite);
        } else {
            ivFavorite.setImageResource(R.drawable.ic_favorite_border);
        }
    }

    private void getReportsByPlaceUuid(String placeUuid) {
        DialogUtils.showLoadingDialog(context, getString(R.string.Loading_reports));

        String token = "Bearer " + SharedPref.getAccessToken(context);

        ReportService reportService = LocationApiClient.getInstance().getReportService();
        Call<JsonObject> call = reportService.getPlaceReports(token, placeUuid);

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                DialogUtils.dismissDialog();
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject responseBody = response.body();
                    JsonObject data = responseBody.getAsJsonObject("_data");

                    if (data != null && data.has("reports")) {
                        reportList.clear(); // Clear previous list

                        for (JsonElement element : data.getAsJsonArray("reports")) {
                            Report report = getReport(element);

                            reportList.add(report);
                        }
                        // to get latest report first
                        Collections.reverse(reportList);
                        List<Report> latestReports = reportList.size() > 5 ? reportList.subList(0, 5) : reportList;

                        // Update the RecyclerView with latest reports
                        setUpRecyclerView(latestReports);
                    } else {
                        setDataVisibility(false);
                        Helper.makeSnackBar(rlPlaceDetails, getString(R.string.No_reports_found));
                    }
                } else {
                    setDataVisibility(false);
                    Helper.makeSnackBar(rlPlaceDetails, getString(R.string.Failed_to_load_reports_Try_again_));
                    Log.e(TAG, "Get Reports Error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                DialogUtils.dismissDialog();
                Log.e(TAG, "Get Reports Failure: ", t);
                Helper.makeSnackBar(rlPlaceDetails, context.getString(R.string.Network_error_Try_again));
            }
        });
    }

    private static @NonNull Report getReport(JsonElement element) {
        JsonObject reportObject = element.getAsJsonObject();
        Report report = new Report();

        report.setUuid(reportObject.get("uuid").getAsString());
        report.setReportRating(reportObject.get("rating").getAsInt());
        report.setDescription(reportObject.get("description").getAsString());
        report.setPlaceName(reportObject.get("place_name").getAsString());
        report.setPlaceUuid(reportObject.get("place_uuid").getAsString());
        return report;
    }
}

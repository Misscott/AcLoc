package com.example.acloc.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.acloc.activity.AddNewPlaceActivity;
import com.example.acloc.activity.AddReportActivity;
import com.example.acloc.activity.PlaceDetailActivity;
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
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ieslamar.acloc.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlaceBottomSheetDialog extends BottomSheetDialogFragment {
    private static final String TAG = "PlaceBottomSheetDialog";

    private final Place place;
    private Context context;
    private boolean isFavorite = false;

    private TextView tvPlaceName, tvAddress, tvDescription, tvNoReports;
    private ImageView ivFavorite, ivEdit, ivExpand;
    private AppCompatButton btnAddReport;
    private RecyclerView rvReports;
    private PlaceReportsAdapter adapter;
    private final List<Report> reportList = new ArrayList<>();
    private BottomSheetBehavior<View> behavior;

    public PlaceBottomSheetDialog(Place place) {
        this.place = place;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);

        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialogInterface;
            View bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                behavior = BottomSheetBehavior.from(bottomSheet);

                //Set initial state to half expanded
                behavior.setPeekHeight(getResources().getDisplayMetrics().heightPixels / 2);
                behavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);

                //Add callback to update expand/collapse icon
                behavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                    @Override
                    public void onStateChanged(@NonNull View bottomSheet, int newState) {
                        if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                            ivExpand.setImageResource(R.drawable.ic_expand_more);
                        } else {
                            ivExpand.setImageResource(R.drawable.ic_expand_less);
                        }
                    }

                    @Override
                    public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                        // animation based on slide position - do nothing for now
                    }
                });
            }
        });

        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_place_detail, container, false);
        context = getContext();

        initUI(view);
        setPlaceData();
        initListeners();
        checkIfPlaceIsFavorite(SharedPref.getUserUid(context), place.getUuid());
        loadReports();

        return view;
    }

    private void initUI(View view) {
        tvPlaceName = view.findViewById(R.id.tvPlaceName);
        tvAddress = view.findViewById(R.id.tvAddress);
        tvDescription = view.findViewById(R.id.tvDescription);
        tvNoReports = view.findViewById(R.id.tvNoData);
        ivFavorite = view.findViewById(R.id.ivFavorite);
        ivEdit = view.findViewById(R.id.ivEdit);
        ivExpand = view.findViewById(R.id.ivExpand);
        btnAddReport = view.findViewById(R.id.btnAddReport);
        rvReports = view.findViewById(R.id.rvReports);

        rvReports.setLayoutManager(new LinearLayoutManager(context));
    }

    private void setPlaceData() {
        tvPlaceName.setText(place.getName());
        tvAddress.setText(place.getAddress());
        tvDescription.setText(place.getDescription());
    }

    private void initListeners() {
        ivFavorite.setOnClickListener(v -> {
            if (isFavorite) {
                removePlaceFromFavorites(SharedPref.getUserUid(context), place.getUuid());
            } else {
                addPlaceToFavorites(SharedPref.getUserUid(context), place.getUuid());
            }
        });

        ivEdit.setOnClickListener(v -> {
            Helper.goTo(context, AddNewPlaceActivity.class, Constants.PLACE, place);
            dismiss();
        });

        ivExpand.setOnClickListener(v -> {
            if (behavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                behavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
                ivExpand.setImageResource(R.drawable.ic_expand_less);
            } else {
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                ivExpand.setImageResource(R.drawable.ic_expand_more);
            }
        });

        btnAddReport.setOnClickListener(v -> {
            Helper.goTo(context, AddReportActivity.class, Constants.PLACE, place);
            dismiss();
        });

        // Open full screen details on click
        View.OnClickListener fullScreenListener = v -> {
            Helper.goTo(context, PlaceDetailActivity.class, Constants.PLACE, place);
            dismiss();
        };

        tvPlaceName.setOnClickListener(fullScreenListener);
        tvAddress.setOnClickListener(fullScreenListener);
        tvDescription.setOnClickListener(fullScreenListener);
    }

    private void loadReports() {
        String token = "Bearer " + SharedPref.getAccessToken(context);
        ReportService reportService = LocationApiClient.getInstance().getReportService();

        Call<JsonObject> call = reportService.getPlaceReports(token, place.getUuid());
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject responseBody = response.body();
                    JsonObject data = responseBody.getAsJsonObject("_data");

                    if (data != null && data.has("reports")) {
                        reportList.clear();

                        for (JsonElement element : data.getAsJsonArray("reports")) {
                            Report report = getReport(element);

                            reportList.add(report);
                        }

                        // Show latest reports first
                        Collections.reverse(reportList);
                        List<Report> latestReports = reportList.size() > 3 ?
                                reportList.subList(0, 3) : reportList;

                        updateReportsUI(latestReports);
                    } else {
                        showNoReports();
                    }
                } else {
                    showNoReports();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                showNoReports();
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

    private void updateReportsUI(List<Report> reports) {
        if (reports != null && !reports.isEmpty()) {
            adapter = new PlaceReportsAdapter(context, reports);
            rvReports.setAdapter(adapter);
            rvReports.setVisibility(View.VISIBLE);
            tvNoReports.setVisibility(View.GONE);
        } else {
            showNoReports();
        }
    }

    private void showNoReports() {
        rvReports.setVisibility(View.GONE);
        tvNoReports.setVisibility(View.VISIBLE);
    }

    private void checkIfPlaceIsFavorite(String userUuid, String placeUuid) {
        String token = "Bearer " + SharedPref.getAccessToken(context);
        FavoriteService favoriteService = LocationApiClient.getInstance().getFavoriteService();

        Call<JsonObject> call = favoriteService.getFavoritePlaces(token, userUuid);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject body = response.body();
                    JsonArray favoritesArray = body.getAsJsonObject("_data").getAsJsonArray("favorites");

                    isFavorite = false;
                    for (JsonElement item : favoritesArray) {
                        JsonObject favoriteObj = item.getAsJsonObject();
                        String favPlaceUuid = favoriteObj.get("place_uuid").getAsString();
                        if (favPlaceUuid.equals(placeUuid)) {
                            isFavorite = true;
                            break;
                        }
                    }
                    updateFavoriteIcon();
                } else {
                    isFavorite = false;
                    updateFavoriteIcon();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
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

    private void addPlaceToFavorites(String userUuid, String placeUuid) {
        DialogUtils.showLoadingDialog(context, getString(R.string.Adding_to_favorites));

        JsonObject body = new JsonObject();
        body.addProperty("place_uuid", placeUuid);

        String token = "Bearer " + SharedPref.getAccessToken(context);
        FavoriteService favoriteService = LocationApiClient.getInstance().getFavoriteService();

        Call<JsonObject> call = favoriteService.addPlaceToFavorites(token, userUuid, body);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                DialogUtils.dismissDialog();
                if (response.isSuccessful()) {
                    isFavorite = true;
                    updateFavoriteIcon();
                    Helper.showToast(context, getString(R.string.Place_added_to_favorites));
                } else {
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();

                            // Try restoring if it's a duplicate (409 Conflict) or it isn't found (deleted)
                            if ((response.code() == 409 && errorBody.contains("ER_DUP_ENTRY"))|| response.code() == 404 ) {
                                restorePlaceToFavorites(userUuid, placeUuid);
                                return;
                            }
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "ERROR: ", e);
                    }

                    Helper.showToast(context, getString(R.string.Failed_to_add_favorite_Server_error));
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                DialogUtils.dismissDialog();
                Helper.showToast(context, getString(R.string.Network_error_Try_again));
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
                    isFavorite = true;
                    updateFavoriteIcon();
                    Helper.showToast(context, getString(R.string.Place_restored_to_favorites));
                } else {
                    Helper.showToast(context, getString(R.string.Failed_to_restore_favorite));
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                DialogUtils.dismissDialog();
                Helper.showToast(context, getString(R.string.Network_error_Try_again));
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
                    isFavorite = false;
                    updateFavoriteIcon();
                    Helper.showToast(context, getString(R.string.Removed_from_favorites));
                } else {
                    Helper.showToast(context, getString(R.string.Failed_to_remove_from_favorites));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                DialogUtils.dismissDialog();
                Helper.showToast(context, getString(R.string.Network_error_Try_again));
            }
        });
    }
}

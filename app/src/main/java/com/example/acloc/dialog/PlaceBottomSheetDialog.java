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
import com.example.acloc.adapter.AccessibilityTagsAdapter;
import com.example.acloc.api.LocationApiClient;
import com.example.acloc.model.Place;
import com.example.acloc.model.Report;
import com.example.acloc.service.FavoriteService;
import com.example.acloc.service.ReportService;
import com.example.acloc.utility.Constants;
import com.example.acloc.utility.DialogUtils;
import com.example.acloc.utility.Helper;
import com.example.acloc.utility.SharedPref;
import com.example.acloc.utility.AccessibilityHelper;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ieslamar.acloc.R;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlaceBottomSheetDialog extends BottomSheetDialogFragment {
    private static final String TAG = "PlaceBottomSheetDialog";

    private final Place place;
    private Context context;
    private boolean isFavorite = false;

    private TextView tvPlaceName, tvAddress, tvDescription;
    private View tvNoReports;
    private ImageView ivFavorite, ivEdit, ivExpand, ivPlaceImage;
    private AppCompatButton btnAddReport;
    private RecyclerView rvReports, rvAccessibilityOverview;
    private PlaceReportsAdapter adapter;
    private final List<Report> reportList = new ArrayList<>();
    private BottomSheetBehavior<View> behavior;
    private TextView tvViewAllReports;
    private List<Report> allReportsList = new ArrayList<>();

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

                behavior.setPeekHeight(getResources().getDisplayMetrics().heightPixels / 2);
                behavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);

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
                        // Do nothing
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
        checkIfPlaceIsFavorite(SharedPref.getUserUuid(context), place.getUuid());
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
        ivPlaceImage = view.findViewById(R.id.ivPlaceImage);
        btnAddReport = view.findViewById(R.id.btnAddReport);
        rvReports = view.findViewById(R.id.rvReports);
        rvAccessibilityOverview = view.findViewById(R.id.rvAccessibilityOverview);
        tvViewAllReports = view.findViewById(R.id.tvViewAllReports);

        rvReports.setLayoutManager(new LinearLayoutManager(context));
        rvAccessibilityOverview.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
    }

    private void setPlaceData() {
        tvPlaceName.setText(place.getName());
        tvAddress.setText(place.getAddress());
        tvDescription.setText(place.getDescription());

        // Load place image with better error handling
        loadPlaceImage();
    }

    private void loadPlaceImage() {
        if (place.getImage() != null && !place.getImage().isEmpty()) {
            try {
                JSONArray imageArray = new JSONArray(place.getImage());
                if (imageArray.length() > 0) {
                    String imageUrl = imageArray.getString(0);

                    // Ensure the URL is properly formatted
                    if (!imageUrl.startsWith("http")) {
                        // Assume it's a relative path and prepend base URL
                        imageUrl = Constants.BASE_URL + imageUrl;
                    }

                    ivPlaceImage.setVisibility(View.VISIBLE);
                    Picasso.get()
                            .load(imageUrl)
                            .placeholder(R.drawable.place_header)
                            .error(R.drawable.place_header)
                            .fit()
                            .centerCrop()
                            .into(ivPlaceImage);

                    Log.d(TAG, "Loading place image: " + imageUrl);
                } else {
                    setDefaultPlaceImage();
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing place images", e);
                setDefaultPlaceImage();
            }
        } else {
            setDefaultPlaceImage();
        }
    }

    private void setDefaultPlaceImage() {
        ivPlaceImage.setVisibility(View.VISIBLE);
        ivPlaceImage.setImageResource(R.drawable.place_header);
    }

    private void initListeners() {
        ivFavorite.setOnClickListener(v -> {
            if (isFavorite) {
                removePlaceFromFavorites(SharedPref.getUserUuid(context), place.getUuid());
            } else {
                addPlaceToFavorites(SharedPref.getUserUuid(context), place.getUuid());
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

        /*View.OnClickListener fullScreenListener = v -> {
            Helper.goTo(context, PlaceDetailActivity.class, Constants.PLACE, place);
            dismiss();
        };*/

        tvViewAllReports.setOnClickListener(v -> {
            //expand
            showAllReports();

            // fullscreen
            // Helper.goTo(context, PlaceDetailActivity.class, Constants.PLACE, place);
            // dismiss();
        });

        /*tvPlaceName.setOnClickListener(fullScreenListener);
        tvAddress.setOnClickListener(fullScreenListener);
        tvDescription.setOnClickListener(fullScreenListener);
        ivPlaceImage.setOnClickListener(fullScreenListener);*/
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
                        allReportsList.clear();

                        for (JsonElement element : data.getAsJsonArray("reports")) {
                            Report report = getReport(element);
                            allReportsList.add(report);
                        }

                        Collections.reverse(allReportsList);

                        List<Report> latestReports = allReportsList.size() > 3 ?
                                allReportsList.subList(0, 3) : allReportsList;

                        updateReportsUI(latestReports);

                        if (allReportsList.size() > 3) {
                            tvViewAllReports.setVisibility(View.VISIBLE);
                            tvViewAllReports.setText(getString(R.string.view_all) + " (" + allReportsList.size() + ")");
                        } else {
                            tvViewAllReports.setVisibility(View.GONE);
                        }

                        calculateAndShowAccessibilityStats(allReportsList);
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

        try {
            // Campos básicos con verificación de null
            if (reportObject.has("uuid") && !reportObject.get("uuid").isJsonNull()) {
                report.setUuid(reportObject.get("uuid").getAsString());
            }

            if (reportObject.has("rating") && !reportObject.get("rating").isJsonNull()) {
                report.setReportRating(reportObject.get("rating").getAsInt());
            }

            if (reportObject.has("description") && !reportObject.get("description").isJsonNull()) {
                report.setDescription(reportObject.get("description").getAsString());
            }

            if (reportObject.has("place_name") && !reportObject.get("place_name").isJsonNull()) {
                report.setPlaceName(reportObject.get("place_name").getAsString());
            }

            if (reportObject.has("place_uuid") && !reportObject.get("place_uuid").isJsonNull()) {
                report.setPlaceUuid(reportObject.get("place_uuid").getAsString());
            }

            // Images
            if (reportObject.has("images") && !reportObject.get("images").isJsonNull()) {
                JsonElement imagesElement = reportObject.get("images");
                if (imagesElement.isJsonArray()) {
                    report.setImage(imagesElement.toString());
                } else {
                    report.setImage(imagesElement.getAsString());
                }
            }

            //report type
            List<String> reportTypeUuids = new ArrayList<>();
            List<String> reportTypeNames = new ArrayList<>();

            processReportTypeField(reportObject, "report_type_uuids", reportTypeUuids);
            processReportTypeField(reportObject, "report_type_names", reportTypeNames);

            // Fallback para campos legacy (singular)
            if (reportTypeUuids.isEmpty() && reportObject.has("report_type_uuid") && !reportObject.get("report_type_uuid").isJsonNull()) {
                reportTypeUuids.add(reportObject.get("report_type_uuid").getAsString());
            }

            if (reportTypeNames.isEmpty() && reportObject.has("report_type_name") && !reportObject.get("report_type_name").isJsonNull()) {
                reportTypeNames.add(reportObject.get("report_type_name").getAsString());
            }

            report.setReportTypeUuids(reportTypeUuids);
            report.setReportTypeNames(reportTypeNames);

        } catch (Exception e) {
            Log.e(TAG, "Error parsing report: " + (report.getUuid() != null ? report.getUuid() : "unknown"), e);
        }

        return report;
    }

    private static void processReportTypeField(JsonObject reportObject, String fieldName, List<String> resultList) {
        try {
            if (!reportObject.has(fieldName) || reportObject.get(fieldName).isJsonNull()) {
                return;
            }

            JsonElement element = reportObject.get(fieldName);

            if (element.isJsonArray()) {
                // array JSON
                JsonArray array = element.getAsJsonArray();
                for (JsonElement item : array) {
                    if (!item.isJsonNull()) {
                        String value = item.getAsString().trim();
                        if (!value.isEmpty()) {
                            resultList.add(value);
                        }
                    }
                }
                Log.d(TAG, fieldName + " processed as array: " + resultList.size() + " items");

            } else if (element.isJsonPrimitive()) {
                // string (GROUP_CONCAT)
                String stringValue = element.getAsString();
                if (!stringValue.isEmpty()) {
                    // Split (GROUP_CONCAT)
                    String[] values = stringValue.split(",");
                    for (String value : values) {
                        String cleanValue = value.trim();
                        if (!cleanValue.isEmpty()) {
                            resultList.add(cleanValue);
                        }
                    }
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error processing " + fieldName, e);
        }
    }

    private void showAllReports() {
        updateReportsUI(allReportsList);

        tvViewAllReports.setText(getString(R.string.show_less));

        tvViewAllReports.setOnClickListener(v -> showLessReports());

        // Expand bottom sheet if it is not expanded
        if (behavior != null && behavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    private void showLessReports() {
        // Mostrar solo los primeros 3 reportes
        List<Report> latestReports = allReportsList.size() > 3 ?
                allReportsList.subList(0, 3) : allReportsList;

        updateReportsUI(latestReports);

        // Restaurar texto y listener original
        tvViewAllReports.setText(getString(R.string.view_all) + " (" + allReportsList.size() + ")");
        tvViewAllReports.setOnClickListener(v -> showAllReports());
    }

    private void calculateAndShowAccessibilityStats(List<Report> allReports) {
        if (allReports.isEmpty()) {
            rvAccessibilityOverview.setVisibility(View.GONE);
            return;
        }

        // Calculate statistics for each accessibility type
        Map<String, AccessibilityStats> statsMap = new HashMap<>();
        int totalReports = allReports.size();

        for (Report report : allReports) {
            List<String> reportTypeNames = report.getReportTypeNames();
            if (reportTypeNames != null) {
                for (String typeName : reportTypeNames) {
                    String displayName = AccessibilityHelper.getDisplayName(context, typeName);
                    if (!displayName.equals(context.getString(R.string.accessibility_unknown))) {
                        AccessibilityStats stats = statsMap.getOrDefault(displayName, new AccessibilityStats(displayName));
                        stats.addRating(report.getReportRating());
                        statsMap.put(displayName, stats);
                    }
                }
            }
        }

        // Show overall statistics
        showAccessibilityOverview(new ArrayList<>(statsMap.values()));
    }

    private void showAccessibilityOverview(List<AccessibilityStats> statsList) {
        if (statsList.isEmpty()) {
            rvAccessibilityOverview.setVisibility(View.GONE);
            return;
        }

        // Convert stats to tag data for display
        List<AccessibilityTagsAdapter.TagData> tagDataList = new ArrayList<>();
        for (AccessibilityStats stats : statsList) {
            tagDataList.add(new AccessibilityTagsAdapter.TagData(
                    stats.typeName + " (" + stats.count + ")",
                    stats.getAverageRating()
            ));
        }

        AccessibilityTagsAdapter overviewAdapter = new AccessibilityTagsAdapter(context, tagDataList);
        rvAccessibilityOverview.setAdapter(overviewAdapter);
        rvAccessibilityOverview.setVisibility(View.VISIBLE);
    }

    private void showNoReports() {
        rvReports.setVisibility(View.GONE);
        rvAccessibilityOverview.setVisibility(View.GONE);
        tvNoReports.setVisibility(View.VISIBLE);
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

    /**
     * Helper method to set text for no reports state
     * @param text
     */
    private void setNoReportsText(String text) {
        if (tvNoReports instanceof TextView) {
            ((TextView) tvNoReports).setText(text);
        } else if (tvNoReports instanceof ViewGroup) {
            // Search for first TextView in the container
            ViewGroup container = (ViewGroup) tvNoReports;
            for (int i = 0; i < container.getChildCount(); i++) {
                View child = container.getChildAt(i);
                if (child instanceof TextView) {
                    ((TextView) child).setText(text);
                    break;
                }
            }
        }
    }

    // Helper class for accessibility statistics
    private static class AccessibilityStats {
        String typeName;
        int count = 0;
        int totalRating = 0;

        AccessibilityStats(String typeName) {
            this.typeName = typeName;
        }

        void addRating(int rating) {
            count++;
            totalRating += rating;
        }

        int getAverageRating() {
            if (count == 0) return Constants.AVERAGE_RATING;

            double average = (double) totalRating / count;
            if (average <= 1.5) return Constants.BAD_RATING;
            if (average <= 2.5) return Constants.AVERAGE_RATING;
            return Constants.GOOD_RATING;
        }
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
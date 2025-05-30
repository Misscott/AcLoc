package com.example.acloc.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.ieslamar.acloc.R;
import com.example.acloc.adapter.MyReportsAdapter;
import com.example.acloc.api.LocationApiClient;
import com.example.acloc.model.Report;
import com.example.acloc.service.ReportService;
import com.example.acloc.utility.DialogUtils;
import com.example.acloc.utility.Helper;
import com.example.acloc.utility.SharedPref;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyReportsFragment extends Fragment {
    public static final String TAG = MyReportsFragment.class.getSimpleName();
    private View view;
    private FrameLayout rlMyReport;
    private RecyclerView rvReport;
    private View tvNoData;
    private Context context;
    private MyReportsAdapter adapter;
    private Report reportEntity;
    private List<Report> reportList;

    public MyReportsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_my_reports, container, false);
        initUI();
        initObj();
        return view;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) {
            setDataVisibility(false);
            reportList.clear();
        }
        if (context != null) loadData();
    }

    private void setDataVisibility(boolean isDataAvailable) {
        if (isDataAvailable) {
            rvReport.setVisibility(View.VISIBLE);
            tvNoData.setVisibility(View.GONE);
        } else {
            rvReport.setVisibility(View.GONE);
            tvNoData.setVisibility(View.VISIBLE);
        }
    }

    private void initUI() {
        rlMyReport = view.findViewById(R.id.rlMyReport);
        rvReport = view.findViewById(R.id.rvReport);
        tvNoData = view.findViewById(R.id.tvNoData);
    }

    private void initObj() {
        context = getContext();
        reportEntity = new Report();
    }

    private void loadData() {
        try {
            if (reportList == null) {
                reportList = new ArrayList<>();
            }
            String userUuid = SharedPref.getUserUuid(context);
            getReportsByUserUuid(userUuid);
        } catch (Exception e) {
            Log.e(TAG, "Error in MyReportsFragment", e);
            Helper.makeSnackBar(view, getString(R.string.Something_went_wrong_Try_again));
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void setUpRecyclerView() {
        try {
            if (adapter != null) {
                adapter.updateReportList(reportList);
            } else {
                adapter = new MyReportsAdapter(context, reportList);
                rvReport.setAdapter(adapter);
                rvReport.setLayoutManager(Helper.getVerticalManager(context));
                adapter.notifyDataSetChanged();
            }
            setDataVisibility(true);
        } catch (Exception e) {
            Log.e(TAG, "Error in MyReportFragment", e);
            Helper.makeSnackBar(rlMyReport, getString(R.string.Something_went_wrong_Try_again));
            setDataVisibility(false);
        }
    }

    private void getReportsByUserUuid(String userUuid) {
        DialogUtils.showLoadingDialog(context, getString(R.string.Loading_reports));

        String token = "Bearer " + SharedPref.getAccessToken(context);

        ReportService reportService = LocationApiClient.getInstance().getReportService();
        Call<JsonObject> call = reportService.getUserReports(token, userUuid);

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
                            JsonObject reportObject = element.getAsJsonObject();
                            Report report = new Report();

                            report.setUuid(reportObject.get("uuid").getAsString());
                            report.setReportRating(reportObject.get("rating").getAsInt());
                            report.setDescription(reportObject.get("description").getAsString());
                            report.setPlaceName(reportObject.get("place_name").getAsString());
                            report.setPlaceUuid(reportObject.get("place_uuid").getAsString());

                            // Images
                            if (reportObject.has("images") && !reportObject.get("images").isJsonNull()) {
                                JsonElement imagesElement = reportObject.get("images");
                                if (imagesElement.isJsonArray()) {
                                    report.setImage(imagesElement.toString());
                                } else {
                                    report.setImage(imagesElement.getAsString());
                                }
                            }

                            // Report Type UUIDs
                            if (reportObject.has("report_type_uuids") && !reportObject.get("report_type_uuids").isJsonNull()) {
                                JsonElement typeUuidsElement = reportObject.get("report_type_uuids");
                                List<String> uuids = new ArrayList<>();

                                if (typeUuidsElement.isJsonArray()) {
                                    JsonArray uuidsArray = typeUuidsElement.getAsJsonArray();
                                    for (JsonElement uuidElement : uuidsArray) {
                                        uuids.add(uuidElement.getAsString());
                                    }
                                } else if (typeUuidsElement.isJsonPrimitive()) {
                                    String uuidsString = typeUuidsElement.getAsString();
                                    if (uuidsString != null && !uuidsString.trim().isEmpty()) {
                                        String[] uuidArray = uuidsString.split(",");
                                        for (String uuid : uuidArray) {
                                            if (!uuid.trim().isEmpty()) {
                                                uuids.add(uuid.trim());
                                            }
                                        }
                                    }
                                }
                                report.setReportTypeUuids(uuids);
                            }

                            // Report Type Names
                            if (reportObject.has("report_type_names") && !reportObject.get("report_type_names").isJsonNull()) {
                                JsonElement typeNamesElement = reportObject.get("report_type_names");
                                List<String> names = new ArrayList<>();

                                if (typeNamesElement.isJsonArray()) {
                                    JsonArray namesArray = typeNamesElement.getAsJsonArray();
                                    for (JsonElement nameElement : namesArray) {
                                        names.add(nameElement.getAsString());
                                    }
                                } else if (typeNamesElement.isJsonPrimitive()) {
                                    String namesString = typeNamesElement.getAsString();
                                    if (namesString != null && !namesString.trim().isEmpty()) {
                                        String[] nameArray = namesString.split(",");
                                        for (String name : nameArray) {
                                            if (!name.trim().isEmpty()) {
                                                names.add(name.trim());
                                            }
                                        }
                                    }
                                }
                                report.setReportTypeNames(names);
                            }

                            reportList.add(report);
                        }

                        // Set up recyclerview
                        if (!reportList.isEmpty()) {
                            setUpRecyclerView();
                        } else {
                            setDataVisibility(false);
                        }

                    } else {
                        setDataVisibility(false);
                        Helper.makeSnackBar(rlMyReport, getString(R.string.No_reports_found));
                    }
                } else {
                    setDataVisibility(false);
                    Helper.makeSnackBar(rlMyReport, getString(R.string.Failed_to_load_reports_Try_again_));
                    Log.e(TAG, "Get Reports Error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                DialogUtils.dismissDialog();
                Log.e(TAG, "Get Reports Failure: ", t);
                Helper.makeSnackBar(rlMyReport, context.getString(R.string.Network_error_Try_again));
            }
        });
    }
}

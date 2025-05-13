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

import com.example.acloc.MainActivity;
import com.ieslamar.acloc.R;
import com.example.acloc.adapter.MyReportsAdapter;
import com.example.acloc.api.ApiClient;
import com.example.acloc.interfaces.ApiService;
import com.example.acloc.model.Report;
import com.example.acloc.utility.DialogUtils;
import com.example.acloc.utility.Helper;
import com.example.acloc.utility.SharedPref;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyReportsFragment extends Fragment implements View.OnClickListener {
    public static final String TAG = MyReportsFragment.class.getSimpleName();
    private View view;
    private FrameLayout rlMyReport;
    private RecyclerView rvReport;
    private TextView tvNoData;
    private ExtendedFloatingActionButton extendedFbReport;
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
        initListener();
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
        extendedFbReport = view.findViewById(R.id.extendedFbReport);
    }

    private void initObj() {
        context = getContext();
        reportEntity = new Report();
    }

    private void initListener() {
        extendedFbReport.setOnClickListener(this);
    }

    private void loadData() {
        try {
            if (reportList == null) {
                reportList = new ArrayList<>();
            }
            String userUuid = SharedPref.getUserUid(context);
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

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.extendedFbReport) {
            onClickExtendedFbReport();
        }
    }

    //Open Map Fragment to add new place report
    private void onClickExtendedFbReport() {
        ((MainActivity) requireActivity())
                .openFragmentFromChild(new MapFragment(), getString(R.string.Map), R.id.menu_map);
    }

    private void getReportsByUserUuid(String userUuid) {
        DialogUtils.showLoadingDialog(context, getString(R.string.Loading_reports));

        String token = "Bearer " + SharedPref.getAccessToken(context);
        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        Call<JsonObject> call = apiService.getUserReports(token, userUuid);
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
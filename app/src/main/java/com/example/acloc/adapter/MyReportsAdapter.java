package com.example.acloc.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.ieslamar.acloc.R;
import com.example.acloc.activity.AddReportActivity;
import com.example.acloc.api.LocationApiClient;
import com.example.acloc.model.Report;
import com.example.acloc.service.ReportService;
import com.example.acloc.utility.Constants;
import com.example.acloc.utility.DialogUtils;
import com.example.acloc.utility.Helper;
import com.example.acloc.utility.SharedPref;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyReportsAdapter extends RecyclerView.Adapter<MyReportsAdapter.ViewHolder> {
    public static final String TAG = MyReportsAdapter.class.getSimpleName();
    private final Context context;
    private List<Report> reportList;

    public MyReportsAdapter(Context context, List<Report> reportList) {
        this.context = context;
        this.reportList = reportList;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateReportList(List<Report> reportList) {
        try {
            if (reportList != null) {
                this.reportList = reportList;
                notifyDataSetChanged();
            }
        } catch (Exception exception) {
            Log.e(TAG, "Error in MyReportsAdapter", exception);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View detailItem = inflater.inflate(R.layout.list_view_my_report, parent, false);
        return new ViewHolder(detailItem);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull MyReportsAdapter.ViewHolder holder, int position) {
        try {
            if (!reportList.isEmpty()) {
                Report report = reportList.get(position);
                holder.tvPlaceName.setText(report.getPlaceName());
                holder.tvDescription.setText(report.getDescription());
                if (report.getReportRating() == 1) {
                    holder.tvRating.setText(context.getString(R.string.Rating_BAD));
                    holder.tvRating.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.red));
                } else if (report.getReportRating() == 2) {
                    holder.tvRating.setText(context.getString(R.string.Rating_AVERAGE));
                    holder.tvRating.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.yellow));
                } else if (report.getReportRating() == 3) {
                    holder.tvRating.setText(context.getString(R.string.Rating_GOOD));
                    holder.tvRating.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.green));
                }

                holder.ivDelete.setOnClickListener(v -> {
                    String confirmationText = context.getString(R.string.Delete_report);

                    AlertDialog dialog = DialogUtils.confirmationDialog(
                            context,
                            confirmationText,
                            (dialogInterface, i) -> {
                                removeReport(report.getUuid());
                            }
                    );
                    dialog.show();
                });

                holder.ivEdit.setOnClickListener(v -> {
                    Helper.goTo(context, AddReportActivity.class, Constants.REPORT, report);
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in MyReports Adapter", e);
        }
    }

    @Override
    public int getItemCount() {
        return reportList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvPlaceName, tvDescription, tvRating;
        private final ImageView ivEdit, ivDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPlaceName = itemView.findViewById(R.id.tvPlaceName);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvRating = itemView.findViewById(R.id.tvRating);
            ivEdit = itemView.findViewById(R.id.ivEdit);
            ivDelete = itemView.findViewById(R.id.ivDelete);
        }
    }

    private void removeReport(String reportUuid) {
        DialogUtils.showLoadingDialog(context, context.getString(R.string.Removing_Report));
        String token = "Bearer " + SharedPref.getAccessToken(context);

        ReportService reportService = LocationApiClient.getInstance().getReportService();
        Call<Void> call = reportService.removeReport(token, reportUuid);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                DialogUtils.dismissDialog();
                View rootView = ((Activity) context).findViewById(android.R.id.content);

                if (response.isSuccessful()) {
                    // Find position of the report to be deleted
                    int positionToRemove = -1;
                    for (int i = 0; i < reportList.size(); i++) {
                        if (reportList.get(i).getUuid().equals(reportUuid)) {
                            positionToRemove = i;
                            break;
                        }
                    }

                    // Remove the report from the list
                    if (positionToRemove != -1) {
                        reportList.remove(positionToRemove);
                        notifyItemRemoved(positionToRemove);  // Notify the adapter that the item was removed
                    }

                    Log.d(TAG, "Report removed!");
                    Helper.makeSnackBar(rootView, context.getString(R.string.Report_removed));
                } else {
                    Log.d(TAG, "Failed to remove report");
                    Helper.makeSnackBar(rootView, context.getString(R.string.Failed_to_remove_report));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                DialogUtils.dismissDialog();
                View rootView = ((Activity) context).findViewById(android.R.id.content);
                Log.e(TAG, "Remove Report Error: ", t);
                Helper.makeSnackBar(rootView, context.getString(R.string.Network_error_Try_again));
            }
        });
    }
}

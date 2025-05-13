package com.example.acloc.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ieslamar.acloc.R;
import com.example.acloc.model.Report;

import java.util.List;

public class PlaceReportsAdapter extends RecyclerView.Adapter<PlaceReportsAdapter.ViewHolder> {
    public static final String TAG = PlaceReportsAdapter.class.getSimpleName();
    private final Context context;
    private List<Report> reportList;

    public PlaceReportsAdapter(Context context, List<Report> reportList) {
        this.context = context;
        this.reportList = reportList;
    }
    public void clearReports() {
        reportList.clear();
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateReportsList(List<Report> reportList) {
        try {
            if (reportList != null) {
                this.reportList = reportList;
                notifyDataSetChanged();
            }
        } catch (Exception exception) {
            Log.e(TAG, "Error in PlaceReportsAdapter", exception);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View detailItem = inflater.inflate(R.layout.list_view_place_reports, parent, false);
        return new ViewHolder(detailItem);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull PlaceReportsAdapter.ViewHolder holder, int position) {
        try {
            if (!reportList.isEmpty()) {
                Report report = reportList.get(position);
                holder.tvDescription.setText(report.getDescription());
                if (report.getReportRating() == 1) {
                    holder.tvRating.setText(context.getString(R.string.Rating_BAD));
                    holder.ivRating.setImageResource(R.drawable.ic_thumbs_down);} else if (report.getReportRating() == 2) {
                    holder.tvRating.setText(context.getString(R.string.Rating_AVERAGE));
                    holder.ivRating.setImageResource(R.drawable.ic_thumb_up_average);
                } else if (report.getReportRating() == 3) {
                    holder.tvRating.setText(context.getString(R.string.Rating_GOOD));
                    holder.ivRating.setImageResource(R.drawable.ic_thumbs_up);
                }

            }
        } catch (Exception e) {
            Log.e(TAG, "Error in PlaceReportsAdapter", e);
        }
    }

    @Override
    public int getItemCount() {
        return reportList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvDescription, tvRating;
        private final ImageView ivRating;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvRating = itemView.findViewById(R.id.tvRating);
            ivRating = itemView.findViewById(R.id.ivRating);
        }
    }
}

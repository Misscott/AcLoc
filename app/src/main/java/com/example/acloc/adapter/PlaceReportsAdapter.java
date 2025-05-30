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
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.acloc.model.Report;
import com.example.acloc.utility.Constants;
import com.example.acloc.utility.AccessibilityHelper;
import com.google.android.material.card.MaterialCardView;
import com.ieslamar.acloc.R;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class PlaceReportsAdapter extends RecyclerView.Adapter<PlaceReportsAdapter.ViewHolder> {
    public static final String TAG = PlaceReportsAdapter.class.getSimpleName();
    private final Context context;
    private List<Report> reportList;
    private OnViewAllClickListener onViewAllClickListener;

    public interface OnViewAllClickListener {
        void onViewAllClick();
    }

    public PlaceReportsAdapter(Context context, List<Report> reportList) {
        this.context = context;
        this.reportList = reportList;
    }

    public void setOnViewAllClickListener(OnViewAllClickListener listener) {
        this.onViewAllClickListener = listener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateReportsList(List<Report> reportList) {
        if (reportList != null) {
            this.reportList = reportList;
            notifyDataSetChanged();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void clearReports() {
        if (this.reportList != null) {
            this.reportList.clear();
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_view_place_reports, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            Report report = reportList.get(position);
            holder.tvDescription.setText(report.getDescription());

            // Set rating
            setRatingDisplay(holder, report.getReportRating());

            // Setup report image
            setupReportImage(holder, report);

            // Setup accessibility tags for multiple report types
            setupAccessibilityTags(holder.rvAccessibilityTags, report, position);

        } catch (Exception e) {
            Log.e(TAG, "Error binding report", e);
        }
    }

    private void setupReportImage(ViewHolder holder, Report report) {
        if (report.getImage() != null && !report.getImage().isEmpty() && !report.getImage().equals("[]")) {
            try {
                JSONArray imageArray = new JSONArray(report.getImage());
                if (imageArray.length() > 0) {
                    String imageUrl = imageArray.getString(0);

                    // Ensure the URL is properly formatted
                    if (!imageUrl.startsWith("http")) {
                        imageUrl = Constants.BASE_URL + "public/" + imageUrl;
                    }

                    holder.cvReportImage.setVisibility(View.VISIBLE);
                    Picasso.get()
                            .load(imageUrl)
                            .placeholder(R.drawable.place_header)
                            .error(R.drawable.place_header)
                            .fit()
                            .centerCrop()
                            .into(holder.ivReportImage);

                    Log.d(TAG, "Loading report image: " + imageUrl);
                } else {
                    holder.cvReportImage.setVisibility(View.GONE);
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing report image", e);
                holder.cvReportImage.setVisibility(View.GONE);
            }
        } else {
            holder.cvReportImage.setVisibility(View.GONE);
        }
    }

    // Solo necesitas modificar el método setRatingDisplay en tu código existente:

    private void setRatingDisplay(ViewHolder holder, int rating) {
        int color;

        switch (rating) {
            case Constants.BAD_RATING:
                holder.tvRating.setText(context.getString(R.string.Rating_BAD));
                holder.ivRating.setImageResource(R.drawable.ic_thumbs_down);
                color = ContextCompat.getColor(context, R.color.red);
                break;
            case Constants.AVERAGE_RATING:
                holder.tvRating.setText(context.getString(R.string.Rating_AVERAGE));
                holder.ivRating.setImageResource(R.drawable.ic_thumb_up_average);
                color = ContextCompat.getColor(context, R.color.yellow);
                break;
            case Constants.GOOD_RATING:
                holder.tvRating.setText(context.getString(R.string.Rating_GOOD));
                holder.ivRating.setImageResource(R.drawable.ic_thumbs_up);
                color = ContextCompat.getColor(context, R.color.green);
                break;
            default:
                color = ContextCompat.getColor(context, R.color.yellow);
                break;
        }

        // Apply color to text and icon
        holder.tvRating.setTextColor(color);
        holder.ivRating.setColorFilter(color);
    }


    private void setupAccessibilityTags(RecyclerView rvTags, Report report, int position) {
        // Clear any existing adapter first to avoid conflicts
        rvTags.setAdapter(null);

        // Get report type names - support both single and multiple
        List<String> reportTypeNames = new ArrayList<>();

        if (report.getReportTypeNames() != null && !report.getReportTypeNames().isEmpty()) {
            reportTypeNames.addAll(report.getReportTypeNames());
        }

        // Only show tags if we have report type names
        if (reportTypeNames.isEmpty()) {
            rvTags.setVisibility(View.GONE);
            return;
        }

        // Create tag data for each report type
        List<AccessibilityTagsAdapter.TagData> tagDataList = new ArrayList<>();

        for (String reportTypeName : reportTypeNames) {
            // Use AccessibilityHelper to get proper display name
            String displayName = AccessibilityHelper.getDisplayName(context, reportTypeName);

            // Skip if display name is unknown or empty
            if (!displayName.equals(context.getString(R.string.accessibility_unknown)) &&
                    !displayName.trim().isEmpty()) {

                tagDataList.add(new AccessibilityTagsAdapter.TagData(
                        displayName,
                        report.getReportRating()
                ));
            }
        }

        // Hide tags if no valid display names
        if (tagDataList.isEmpty()) {
            rvTags.setVisibility(View.GONE);
            return;
        }

        // Setup RecyclerView with unique configuration
        LinearLayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
        rvTags.setLayoutManager(layoutManager);

        // Create new adapter instance for each item
        AccessibilityTagsAdapter adapter = new AccessibilityTagsAdapter(context, tagDataList);
        rvTags.setAdapter(adapter);

        // Set unique tag to avoid recycling conflicts
        rvTags.setTag("accessibility_tags_" + position);

        // Disable nested scrolling to avoid conflicts
        rvTags.setNestedScrollingEnabled(false);

        rvTags.setVisibility(View.VISIBLE);

        // Force layout update
        rvTags.post(() -> {
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public int getItemCount() {
        return reportList.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvDescription, tvRating, tvReportDate;
        final ImageView ivRating, ivReportImage;
        final RecyclerView rvAccessibilityTags;
        final MaterialCardView cvReportImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvReportDate = itemView.findViewById(R.id.tvReportDate);
            ivRating = itemView.findViewById(R.id.ivRating);
            ivReportImage = itemView.findViewById(R.id.ivReportImage);
            rvAccessibilityTags = itemView.findViewById(R.id.rvAccessibilityTags);
            cvReportImage = itemView.findViewById(R.id.cvReportImage);
        }
    }
}

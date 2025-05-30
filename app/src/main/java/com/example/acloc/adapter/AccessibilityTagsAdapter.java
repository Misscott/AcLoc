package com.example.acloc.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.acloc.utility.AccessibilityHelper;
import com.google.android.material.card.MaterialCardView;
import com.ieslamar.acloc.R;

import java.util.List;

public class AccessibilityTagsAdapter extends RecyclerView.Adapter<AccessibilityTagsAdapter.ViewHolder> {

    public static class TagData {
        public final String reportTypeName;
        public final int rating;

        public TagData(String reportTypeName, int rating) {
            this.reportTypeName = reportTypeName;
            this.rating = rating;
        }
    }

    private final Context context;
    private final List<TagData> tagDataList;

    public AccessibilityTagsAdapter(Context context, List<TagData> tagDataList) {
        this.context = context;
        this.tagDataList = tagDataList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_accessibility_tag, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TagData tagData = tagDataList.get(position);
        AccessibilityHelper.AccessibilityInfo info = AccessibilityHelper.getAccessibilityInfo(
                context,
                tagData.reportTypeName,
                tagData.rating
        );

        // Set text and icon
        holder.tvTypeName.setText(info.displayName);
        holder.ivIcon.setImageResource(info.iconResource);

        // Set colors
        holder.card.setCardBackgroundColor(
                ContextCompat.getColor(context, info.backgroundColorRes));
        holder.card.setStrokeColor(
                ContextCompat.getColor(context, info.borderColorRes));
        holder.tvTypeName.setTextColor(
                ContextCompat.getColor(context, info.textColorRes));
        holder.ivIcon.setColorFilter(
                ContextCompat.getColor(context, info.textColorRes));
    }

    @Override
    public int getItemCount() {
        return tagDataList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final ImageView ivIcon;
        final TextView tvTypeName;
        final ImageView ivSelected;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.reportTypeCard);
            ivIcon = itemView.findViewById(R.id.reportTypeIcon);
            tvTypeName = itemView.findViewById(R.id.tvReportTypeName);
            ivSelected = itemView.findViewById(R.id.ivSelected);
        }
    }
}

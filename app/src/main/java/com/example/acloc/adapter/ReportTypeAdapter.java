package com.example.acloc.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.acloc.model.ReportType;
import com.example.acloc.utility.AccessibilityHelper;
import com.google.android.material.card.MaterialCardView;
import com.ieslamar.acloc.R;

import java.util.ArrayList;
import java.util.List;

public class ReportTypeAdapter extends RecyclerView.Adapter<ReportTypeAdapter.ViewHolder> {

    private final Context context;
    private final List<ReportType> reportTypesList;
    private OnReportTypeClickListener onReportTypeClickListener;
    private List<Integer> selectedPositions = new ArrayList<>(); // Track multiple selected items

    public interface OnReportTypeClickListener {
        void onReportTypeClick(ReportType reportType, int position, boolean isSelected);
        void onSelectionChanged(List<ReportType> selectedReportTypes);
    }

    public ReportTypeAdapter(Context context, List<ReportType> reportTypesList) {
        this.context = context;
        this.reportTypesList = reportTypesList;
    }

    public void setOnReportTypeClickListener(OnReportTypeClickListener listener) {
        this.onReportTypeClickListener = listener;
    }

    public void clearSelection() {
        List<Integer> previousSelected = new ArrayList<>(selectedPositions);
        selectedPositions.clear();

        // Update all previously selected items
        for (int position : previousSelected) {
            if (position < reportTypesList.size()) {
                reportTypesList.get(position).setSelected(false);
                notifyItemChanged(position);
            }
        }

        if (onReportTypeClickListener != null) {
            onReportTypeClickListener.onSelectionChanged(getSelectedReportTypes());
        }
    }

    public void setSelectedPositions(List<Integer> positions) {
        // Clear previous selections
        clearSelection();

        // Set new selections
        for (int position : positions) {
            if (position >= 0 && position < reportTypesList.size()) {
                selectedPositions.add(position);
                reportTypesList.get(position).setSelected(true);
                notifyItemChanged(position);
            }
        }

        if (onReportTypeClickListener != null) {
            onReportTypeClickListener.onSelectionChanged(getSelectedReportTypes());
        }
    }

    public void setSelectedReportTypes(List<String> reportTypeUuids) {
        clearSelection();

        if (reportTypeUuids != null) {
            for (int i = 0; i < reportTypesList.size(); i++) {
                ReportType reportType = reportTypesList.get(i);
                if (reportTypeUuids.contains(reportType.getUuid())) {
                    selectedPositions.add(i);
                    reportType.setSelected(true);
                    notifyItemChanged(i);
                }
            }
        }

        if (onReportTypeClickListener != null) {
            onReportTypeClickListener.onSelectionChanged(getSelectedReportTypes());
        }
    }

    public List<Integer> getSelectedPositions() {
        return new ArrayList<>(selectedPositions);
    }

    public List<ReportType> getSelectedReportTypes() {
        List<ReportType> selected = new ArrayList<>();
        for (int position : selectedPositions) {
            if (position < reportTypesList.size()) {
                selected.add(reportTypesList.get(position));
            }
        }
        return selected;
    }

    public List<String> getSelectedReportTypeUuids() {
        List<String> uuids = new ArrayList<>();
        for (ReportType reportType : getSelectedReportTypes()) {
            uuids.add(reportType.getUuid());
        }
        return uuids;
    }

    private void toggleSelection(int position) {
        if (position < 0 || position >= reportTypesList.size()) return;

        ReportType reportType = reportTypesList.get(position);

        if (selectedPositions.contains(position)) {
            // Deselect
            selectedPositions.remove(Integer.valueOf(position));
            reportType.setSelected(false);
        } else {
            // Select
            selectedPositions.add(position);
            reportType.setSelected(true);
        }

        notifyItemChanged(position);

        if (onReportTypeClickListener != null) {
            onReportTypeClickListener.onReportTypeClick(reportType, position, reportType.isSelected());
            onReportTypeClickListener.onSelectionChanged(getSelectedReportTypes());
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_report_type_selector, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ReportType reportType = reportTypesList.get(position);
        boolean isSelected = selectedPositions.contains(position);

        // Ensure the model state matches the adapter state
        reportType.setSelected(isSelected);

        // Get icon and display name using AccessibilityHelper
        int iconResource = AccessibilityHelper.getIconForReportType(reportType.getName());
        String displayName = AccessibilityHelper.getDisplayName(context, reportType.getName());

        // Set icon and text
        holder.ivIcon.setImageResource(iconResource);
        holder.tvTypeName.setText(displayName);

        // Set selection state
        if (isSelected) {
            // Selected state - use theme colors
            TypedValue typedValue = new TypedValue();
            context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true);
            int primaryColor = typedValue.data;

            context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnPrimary, typedValue, true);
            int onPrimaryColor = typedValue.data;

            holder.card.setCardBackgroundColor(primaryColor);
            holder.card.setStrokeColor(primaryColor);
            holder.card.setStrokeWidth(4);

            holder.tvTypeName.setTextColor(onPrimaryColor);
            holder.ivIcon.setImageTintList(ColorStateList.valueOf(onPrimaryColor));
            holder.ivSelected.setVisibility(View.VISIBLE);

        } else {
            // Unselected state - use theme colors
            TypedValue typedValue = new TypedValue();
            context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true);
            int surfaceColor = typedValue.data;

            context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorOutline, typedValue, true);
            int outlineColor = typedValue.data;

            context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true);
            int onSurfaceColor = typedValue.data;

            context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurfaceVariant, typedValue, true);
            int onSurfaceVariantColor = typedValue.data;

            holder.card.setCardBackgroundColor(surfaceColor);
            holder.card.setStrokeColor(outlineColor);
            holder.card.setStrokeWidth(2);

            holder.tvTypeName.setTextColor(onSurfaceColor);
            holder.ivIcon.setImageTintList(ColorStateList.valueOf(onSurfaceVariantColor));
            holder.ivSelected.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> toggleSelection(position));

        String selectionState = isSelected ?
                context.getString(R.string.selected) :
                context.getString(R.string.tap_to_select);
        String contentDescription = displayName + ", " + selectionState;

        if (!selectedPositions.isEmpty()) {
            contentDescription += ". " + selectedPositions.size() + " " +
                    context.getString(R.string.selected);
        }

        holder.itemView.setContentDescription(contentDescription);
    }

    @Override
    public int getItemCount() {
        return reportTypesList != null ? reportTypesList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;
        ImageView ivIcon;
        TextView tvTypeName;
        ImageView ivSelected;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.reportTypeCard);
            ivIcon = itemView.findViewById(R.id.reportTypeIcon);
            tvTypeName = itemView.findViewById(R.id.tvReportTypeName);
            ivSelected = itemView.findViewById(R.id.ivSelected);
        }
    }
}

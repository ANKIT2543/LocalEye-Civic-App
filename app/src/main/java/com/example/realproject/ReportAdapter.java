package com.example.realproject;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ViewHolder> {

    private List<Report> reportList;
    private String currentUserId;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onDeleteClick(int position);
        void onResolveClick(int position);
        void onDetailsClick(Report report);
        void onUpvoteClick(int position); // --- NEW INTERFACE METHOD ---
    }

    public ReportAdapter(List<Report> reportList, String currentUserId, OnItemClickListener listener) {
        this.reportList = reportList;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Report report = reportList.get(position);

        holder.tvTitle.setText(report.getTitle());
        holder.tvDescription.setText(report.getDescription());

        // Display Evidence Image if exists
        if (report.getImageUrl() != null && !report.getImageUrl().isEmpty()) {
            holder.ivEvidence.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext()).load(report.getImageUrl()).centerCrop().into(holder.ivEvidence);
        } else {
            holder.ivEvidence.setVisibility(View.GONE);
        }

        boolean isOwner = (currentUserId != null && currentUserId.equals(report.userId));
        holder.btnDelete.setVisibility(isOwner ? View.VISIBLE : View.GONE);

        // ==========================================================
        // SMART BADGE LOGIC (High Priority Triage)
        // ==========================================================
        if ("Resolved".equals(report.getStatus()) || "Closed".equals(report.getStatus())) {
            holder.cvStatusBadge.setCardBackgroundColor(Color.parseColor("#E8F5E9"));
            holder.tvStatus.setTextColor(Color.parseColor("#2E7D32"));
            holder.tvStatus.setText(report.getStatus());
            holder.btnResolve.setVisibility(View.GONE);
        } else {
            // Show resolve button ONLY if it's pending AND the user is the owner
            holder.btnResolve.setVisibility(isOwner ? View.VISIBLE : View.GONE);

            // Check for HIGH PRIORITY (10+ Upvotes)
            if (report.upvotes >= 10) {
                holder.cvStatusBadge.setCardBackgroundColor(Color.parseColor("#FFEBEE")); // Light Red
                holder.tvStatus.setTextColor(Color.parseColor("#C62828")); // Dark Red
                holder.tvStatus.setText("High Priority");
            } else {
                holder.cvStatusBadge.setCardBackgroundColor(Color.parseColor("#FFF3E0"));
                holder.tvStatus.setTextColor(Color.parseColor("#EF6C00"));
                holder.tvStatus.setText(report.getStatus()); // Usually "Reported" or "Pending"
            }
        }

        // ==========================================================
        // IMPACTS ME TOO (Upvote Logic)
        // ==========================================================
        holder.tvUpvoteCount.setText(report.upvotes + " Impacts Me Too");

        // Has the current user already clicked it?
        if (report.upvotedBy != null && report.upvotedBy.contains(currentUserId)) {
            // Highlight it Blue
            holder.ivUpvoteIcon.setColorFilter(Color.parseColor("#1565C0"));
            holder.tvUpvoteCount.setTextColor(Color.parseColor("#1565C0"));
        } else {
            // Keep it Grey
            holder.ivUpvoteIcon.setColorFilter(Color.parseColor("#90A4AE"));
            holder.tvUpvoteCount.setTextColor(Color.parseColor("#90A4AE"));
        }

        // Button Click Listeners
        holder.llUpvoteContainer.setOnClickListener(v -> listener.onUpvoteClick(position));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(position));
        holder.btnResolve.setOnClickListener(v -> listener.onResolveClick(position));
        holder.btnDetails.setOnClickListener(v -> listener.onDetailsClick(report));
    }

    @Override
    public int getItemCount() {
        return reportList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDescription, tvStatus, tvUpvoteCount;
        TextView btnDelete, btnResolve, btnDetails;
        CardView cvStatusBadge;
        ImageView ivEvidence, ivUpvoteIcon;
        LinearLayout llUpvoteContainer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvReportTitle);
            tvDescription = itemView.findViewById(R.id.tvReportDesc);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            cvStatusBadge = itemView.findViewById(R.id.cvStatusBadge);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnResolve = itemView.findViewById(R.id.btnResolve);
            btnDetails = itemView.findViewById(R.id.btnDetails);
            ivEvidence = itemView.findViewById(R.id.ivReportEvidence);

            // New Views
            llUpvoteContainer = itemView.findViewById(R.id.llUpvoteContainer);
            ivUpvoteIcon = itemView.findViewById(R.id.ivUpvoteIcon);
            tvUpvoteCount = itemView.findViewById(R.id.tvUpvoteCount);
        }
    }
}
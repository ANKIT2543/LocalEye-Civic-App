package com.example.realproject;

import android.graphics.Color;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Locale;

public class DetailsFragment extends Fragment {

    private FirebaseFirestore db;
    private View rootView;

    public DetailsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rootView = view;
        db = FirebaseFirestore.getInstance();

        TextView tvTitle = view.findViewById(R.id.tvDetailTitle);
        TextView tvAssignedAuthority = view.findViewById(R.id.tvAssignedAuthority); // NEW
        TextView tvLocation = view.findViewById(R.id.tvDetailLocation);
        TextView tvDesc = view.findViewById(R.id.tvDetailDesc);
        ImageView ivEvidence = view.findViewById(R.id.ivDetailEvidence);
        CardView btnBackCard = view.findViewById(R.id.btnBackCard);

        TextView tvTimestamp = view.findViewById(R.id.tvDetailTimestamp);
        TextView tvReportId = view.findViewById(R.id.tvDetailReportId);

        LinearLayout llResolutionContainer = view.findViewById(R.id.llResolutionContainer);
        ImageView ivResolvedEvidence = view.findViewById(R.id.ivResolvedEvidence);
        View btnEscalateTwitter = view.findViewById(R.id.btnEscalateTwitter);

        // 1. Back Button
        btnBackCard.setOnClickListener(v -> Navigation.findNavController(view).navigateUp());

        // 2. Initial Data Load
        if (getArguments() != null) {
            String title = getArguments().getString("title", "No Title");
            String location = getArguments().getString("location", "No Location Provided");
            String desc = getArguments().getString("description", "No Description");
            String imageUrl = getArguments().getString("imageUrl", "");
            long timestamp = getArguments().getLong("timestamp", 0);
            String reportId = getArguments().getString("reportId", "");

            tvTitle.setText(title);
            tvLocation.setText(location);
            tvDesc.setText(desc);
            tvReportId.setText("#" + reportId);

            if (timestamp > 0) {
                Calendar cal = Calendar.getInstance(Locale.ENGLISH);
                cal.setTimeInMillis(timestamp);
                String date = DateFormat.format("MMM dd, yyyy, h:mm a", cal).toString();
                tvTimestamp.setText("Reported on: " + date);
            } else {
                tvTimestamp.setText("Reported on: Unknown Date");
            }

            if (!imageUrl.isEmpty()) {
                ivEvidence.clearColorFilter();
                Glide.with(this).load(imageUrl).centerCrop().into(ivEvidence);
            } else {
                ivEvidence.setImageResource(android.R.drawable.ic_menu_gallery);
                ivEvidence.setColorFilter(Color.parseColor("#9E9E9E"));
            }

            // ==========================================================
            // 3. THE REAL-TIME LISTENER
            // ==========================================================
            if (!reportId.isEmpty()) {
                db.collection("reports").document(reportId).addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) return;

                    if (documentSnapshot != null && documentSnapshot.exists()) {

                        // Handle the Status & Pipeline Tracker
                        String liveStatus = documentSnapshot.getString("status");
                        if (liveStatus == null) liveStatus = "Reported";
                        updateTrackerUI(liveStatus);

                        // Handle the Smart Authority Badge
                        String assignedAuthority = documentSnapshot.getString("assignedAuthority");
                        if (assignedAuthority != null && !assignedAuthority.isEmpty()) {
                            tvAssignedAuthority.setText("Assigned to: " + assignedAuthority);
                            tvAssignedAuthority.setVisibility(View.VISIBLE);
                        } else {
                            tvAssignedAuthority.setVisibility(View.GONE);
                        }

                        // Handle the dynamic Twitter Escalate Button
                        String targetTwitterHandle = documentSnapshot.getString("targetTwitterHandle");
                        if (targetTwitterHandle == null || targetTwitterHandle.isEmpty()) {
                            targetTwitterHandle = "@bmcbbsr"; // Safety fallback
                        }

                        final String finalTags = targetTwitterHandle;
                        btnEscalateTwitter.setOnClickListener(v -> {
                            String tweetText = "URGENT: Civic issue reported via #LocalEyeApp.\n\n" +
                                    "🛑 Issue: " + title + "\n" +
                                    "📍 Location: " + location + "\n\n" +
                                    "Please resolve immediately! " + finalTags + "\n\n" +
                                    "Evidence: " + imageUrl;

                            try {
                                String encodedTweet = java.net.URLEncoder.encode(tweetText, "UTF-8");
                                String twitterUrl = "https://twitter.com/intent/tweet?text=" + encodedTweet;
                                android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
                                intent.setData(android.net.Uri.parse(twitterUrl));
                                startActivity(intent);
                            } catch (Exception e) {
                                Toast.makeText(getContext(), "Unable to open X.", Toast.LENGTH_SHORT).show();
                            }
                        });

                        // Reveal the hidden Resolution card if it is Closed and we have an image link
                        String resolvedImageUrl = documentSnapshot.getString("resolvedImageUrl");
                        if ((liveStatus.equalsIgnoreCase("Resolved") || liveStatus.equalsIgnoreCase("Closed"))
                                && resolvedImageUrl != null && !resolvedImageUrl.isEmpty()) {
                            llResolutionContainer.setVisibility(View.VISIBLE);
                            Glide.with(requireContext()).load(resolvedImageUrl).centerCrop().into(ivResolvedEvidence);
                        } else {
                            llResolutionContainer.setVisibility(View.GONE);
                        }
                    }
                });
            }
        }
    }

    private void updateTrackerUI(String status) {
        int colorInactive = Color.parseColor("#B0BEC5");
        int colorActive = Color.parseColor("#1565C0");
        int colorSuccess = Color.parseColor("#2E7D32");

        CardView dot1 = rootView.findViewById(R.id.dot1); View line1 = rootView.findViewById(R.id.line1); TextView tvStage1 = rootView.findViewById(R.id.tvStage1);
        CardView dot2 = rootView.findViewById(R.id.dot2); View line2 = rootView.findViewById(R.id.line2); TextView tvStage2 = rootView.findViewById(R.id.tvStage2);
        CardView dot3 = rootView.findViewById(R.id.dot3); View line3 = rootView.findViewById(R.id.line3); TextView tvStage3 = rootView.findViewById(R.id.tvStage3);
        CardView dot4 = rootView.findViewById(R.id.dot4); View line4 = rootView.findViewById(R.id.line4); TextView tvStage4 = rootView.findViewById(R.id.tvStage4);
        CardView dot5 = rootView.findViewById(R.id.dot5); TextView tvStage5 = rootView.findViewById(R.id.tvStage5);

        clearAnimation(dot1); clearAnimation(dot2); clearAnimation(dot3); clearAnimation(dot4); clearAnimation(dot5);

        dot1.setCardBackgroundColor(colorInactive); line1.setBackgroundColor(colorInactive); tvStage1.setTextColor(colorInactive);
        dot2.setCardBackgroundColor(colorInactive); line2.setBackgroundColor(colorInactive); tvStage2.setTextColor(colorInactive);
        dot3.setCardBackgroundColor(colorInactive); line3.setBackgroundColor(colorInactive); tvStage3.setTextColor(colorInactive);
        dot4.setCardBackgroundColor(colorInactive); line4.setBackgroundColor(colorInactive); tvStage4.setTextColor(colorInactive);
        dot5.setCardBackgroundColor(colorInactive); tvStage5.setTextColor(colorInactive);

        switch (status.toLowerCase()) {
            case "closed":
                dot5.setCardBackgroundColor(colorSuccess); tvStage5.setTextColor(colorSuccess);
            case "resolved":
                dot4.setCardBackgroundColor(colorSuccess); line4.setBackgroundColor(colorSuccess); tvStage4.setTextColor(colorSuccess);
                if (status.equalsIgnoreCase("resolved")) applyPulsingAnimation(dot4);
            case "in progress":
                dot3.setCardBackgroundColor(colorActive); line3.setBackgroundColor(colorActive); tvStage3.setTextColor(colorActive);
                if (status.equalsIgnoreCase("in progress")) applyPulsingAnimation(dot3);
            case "acknowledged":
                dot2.setCardBackgroundColor(colorActive); line2.setBackgroundColor(colorActive); tvStage2.setTextColor(colorActive);
                if (status.equalsIgnoreCase("acknowledged")) applyPulsingAnimation(dot2);
            case "reported":
            case "pending":
            default:
                dot1.setCardBackgroundColor(colorActive); line1.setBackgroundColor(colorActive); tvStage1.setTextColor(colorActive);
                if (status.equalsIgnoreCase("reported") || status.equalsIgnoreCase("pending")) applyPulsingAnimation(dot1);
                break;
        }
    }

    private void applyPulsingAnimation(View activeDot) {
        android.animation.ObjectAnimator animator = android.animation.ObjectAnimator.ofFloat(activeDot, "alpha", 1f, 0.3f, 1f);
        animator.setDuration(1200);
        animator.setRepeatCount(android.animation.ObjectAnimator.INFINITE);
        animator.start();
        activeDot.setTag(animator);
    }

    private void clearAnimation(View dot) {
        dot.setAlpha(1f);
        if (dot.getTag() instanceof android.animation.ObjectAnimator) {
            ((android.animation.ObjectAnimator) dot.getTag()).cancel();
            dot.setTag(null);
        }
    }
}
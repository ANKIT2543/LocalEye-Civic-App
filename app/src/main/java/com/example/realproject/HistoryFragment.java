package com.example.realproject;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment {

    private RecyclerView rvHistory;
    private ReportAdapter adapter;
    private List<Report> reportList;
    private FirebaseFirestore db;
    private TextView tvEmptyState;

    public HistoryFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        rvHistory = view.findViewById(R.id.rvHistory);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);

        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        reportList = new ArrayList<>();

        String currentUserId = FirebaseAuth.getInstance().getUid();

        adapter = new ReportAdapter(reportList, currentUserId, new ReportAdapter.OnItemClickListener() {
            @Override
            public void onDeleteClick(int position) {
                deleteReport(position);
            }

            @Override
            public void onResolveClick(int position) {
                Toast.makeText(getContext(), "Status Updated", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDetailsClick(Report report) {
                Bundle bundle = new Bundle();
                bundle.putString("title", report.getTitle());
                bundle.putString("location", report.location != null ? report.location : "No Location Provided");
                bundle.putString("description", report.getDescription());
                bundle.putString("status", report.getStatus());
                bundle.putString("imageUrl", report.getImageUrl() != null ? report.getImageUrl() : "");

                bundle.putLong("timestamp", report.timestamp);
                bundle.putString("reportId", report.reportId);

                Navigation.findNavController(getView()).navigate(R.id.detailsFragment, bundle);
            }

            // ==========================================================
            // UPVOTE METHOD
            // ==========================================================
            @Override
            public void onUpvoteClick(int position) {
                Report clickedReport = reportList.get(position);
                String userId = FirebaseAuth.getInstance().getUid();

                if (userId == null) return;

                if (clickedReport.upvotedBy != null && clickedReport.upvotedBy.contains(userId)) {
                    // Un-vote
                    db.collection("reports").document(clickedReport.reportId)
                            .update("upvotes", com.google.firebase.firestore.FieldValue.increment(-1),
                                    "upvotedBy", com.google.firebase.firestore.FieldValue.arrayRemove(userId));
                } else {
                    // New vote
                    db.collection("reports").document(clickedReport.reportId)
                            .update("upvotes", com.google.firebase.firestore.FieldValue.increment(1),
                                    "upvotedBy", com.google.firebase.firestore.FieldValue.arrayUnion(userId));
                }
            }
        });

        rvHistory.setAdapter(adapter);
        fetchMyReports();
    }

    // ==========================================================
    // REAL-TIME SNAPSHOT LISTENER
    // ==========================================================
    private void fetchMyReports() {
        String myUserId = FirebaseAuth.getInstance().getUid();
        if (myUserId == null) return;

        db.collection("reports")
                .whereEqualTo("userId", myUserId)
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (error != null) {
                        Toast.makeText(getContext(), "Error loading history.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    reportList.clear();
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot d : queryDocumentSnapshots.getDocuments()) {
                            Report report = d.toObject(Report.class).withId(d.getId());
                            reportList.add(report);
                        }
                        adapter.notifyDataSetChanged();
                        tvEmptyState.setVisibility(View.GONE);
                    } else {
                        adapter.notifyDataSetChanged();
                        tvEmptyState.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void deleteReport(int position) {
        Report report = reportList.get(position);
        db.collection("reports").document(report.reportId).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Deleted", Toast.LENGTH_SHORT).show();
                });
    }
}
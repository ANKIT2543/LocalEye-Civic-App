package com.example.realproject;

import androidx.annotation.NonNull;
import com.google.firebase.firestore.Exclude;
import java.util.ArrayList;
import java.util.List;

public class Report {
    @Exclude public String reportId;

    public String title;
    public String description;
    public String status;

    public String location;
    public String userId;
    public long timestamp;
    public String imageUrl;

    // --- CATEGORY FIELD ---
    public String category;

    // --- COMMUNITY UPVOTING FIELDS ---
    public int upvotes = 0;
    public List<String> upvotedBy = new ArrayList<>();

    public Report() {} // Empty constructor for Firebase

    public Report(String title, String description, String status) {
        this.title = title;
        this.description = description;
        this.status = status;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public String getImageUrl() { return imageUrl; }
    public String getCategory() { return category; }

    public <T extends Report> T withId(@NonNull final String id) {
        this.reportId = id;
        return (T) this;
    }
}
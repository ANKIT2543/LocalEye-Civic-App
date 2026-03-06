package com.example.realproject;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

// --- GOOGLE LOCATION IMPORTS ---
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

// --- OSMDROID (OPENSTREETMAP) IMPORTS ---
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections; // IMPORT FOR SORTING
import java.util.Comparator;  // IMPORT FOR SORTING
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private RecyclerView rvTrending;
    private ReportAdapter adapter;
    private List<Report> reportList;
    private List<Report> allReportsList;
    private FirebaseFirestore db;

    // --- MAP VARIABLES ---
    private MapView mapView;
    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private EditText etSearchLocation;

    public HomeFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // --- FOR OSMDROID: Initialize configuration BEFORE inflating layout ---
        Context ctx = requireActivity().getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        Configuration.getInstance().setUserAgentValue(ctx.getPackageName()); // Required by OSM policies

        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                requireActivity().finish();
            }
        });

        db = FirebaseFirestore.getInstance();
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        reportList = new ArrayList<>();
        allReportsList = new ArrayList<>();

        String currentUserId = FirebaseAuth.getInstance().getUid();

        adapter = new ReportAdapter(reportList, currentUserId, new ReportAdapter.OnItemClickListener() {
            @Override
            public void onDeleteClick(int position) { deleteReport(position); }

            @Override
            public void onResolveClick(int position) { updateReportStatus(position); }

            @Override
            public void onDetailsClick(Report report) {
                Bundle bundle = new Bundle();
                bundle.putString("title", report.title);
                bundle.putString("location", report.location != null ? report.location : "No Location Provided");
                bundle.putString("description", report.description);
                bundle.putString("status", report.status);
                bundle.putString("imageUrl", report.imageUrl != null ? report.imageUrl : "");

                bundle.putLong("timestamp", report.timestamp);
                bundle.putString("reportId", report.reportId);

                try {
                    Navigation.findNavController(getView()).navigate(R.id.detailsFragment, bundle);
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Navigation Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onUpvoteClick(int position) {
                Report clickedReport = reportList.get(position);
                String currentUserId = FirebaseAuth.getInstance().getUid();

                if (currentUserId == null) {
                    Toast.makeText(getContext(), "You must be logged in to upvote.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (clickedReport.upvotedBy != null && clickedReport.upvotedBy.contains(currentUserId)) {
                    // Un-vote
                    db.collection("reports").document(clickedReport.reportId)
                            .update("upvotes", com.google.firebase.firestore.FieldValue.increment(-1),
                                    "upvotedBy", com.google.firebase.firestore.FieldValue.arrayRemove(currentUserId));
                } else {
                    // Vote
                    db.collection("reports").document(clickedReport.reportId)
                            .update("upvotes", com.google.firebase.firestore.FieldValue.increment(1),
                                    "upvotedBy", com.google.firebase.firestore.FieldValue.arrayUnion(currentUserId));
                }
            }
        });
        recyclerView.setAdapter(adapter);
        fetchReports();
        setupTrendingList(view);

        view.findViewById(R.id.btnReportIssue).setOnClickListener(v -> {
            try {
                Navigation.findNavController(view).navigate(R.id.reportFragment);
            } catch (Exception e) {
                Toast.makeText(getContext(), "Navigation Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // ==========================================
        // OPENSTREETMAP SETUP & LOGIC
        // ==========================================
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        mapView = view.findViewById(R.id.mapView);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        // ==========================================================
        // UI FIX: Smooth Scrolling
        // ==========================================================
        // 1. Hide the default + and - buttons
        mapView.getZoomController().setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER);

        // 2. Stop the parent screen from stealing the map's touch gestures
        mapView.setOnTouchListener((v, event) -> {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });

        IMapController mapController = mapView.getController();
        mapController.setZoom(14.0);

        GeoPoint defaultLocation = new GeoPoint(20.2961, 85.8245);
        mapController.setCenter(defaultLocation);

        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                enableUserLocation();
            } else {
                Toast.makeText(getContext(), "Showing default location.", Toast.LENGTH_SHORT).show();
            }
        });

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enableUserLocation();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        etSearchLocation = view.findViewById(R.id.etSearchLocation);
        etSearchLocation.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {

                searchMapLocation(etSearchLocation.getText().toString().trim());

                InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

                return true;
            }
            return false;
        });
    }

    @SuppressWarnings("MissingPermission")
    private void enableUserLocation() {
        if (mapView == null) return; // Initial safety check

        // Added requireActivity() to bind this to the screen's lifecycle so it stops if the screen closes
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(requireActivity(), location -> {

                    // ==========================================================
                    // THE SAFETY NET: If the map isn't ready or the user already
                    // switched to another tab, just stop and don't crash!
                    // ==========================================================
                    if (mapView == null) return;

                    if (location != null) {
                        GeoPoint myLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                        mapView.getController().animateTo(myLocation);
                        mapView.getController().setZoom(16.0);

                        Marker userMarker = new Marker(mapView);
                        userMarker.setPosition(myLocation);
                        userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                        userMarker.setTitle("You are here");
                        mapView.getOverlays().add(userMarker);
                        mapView.invalidate();
                    }
                });
    }

    private void searchMapLocation(String locationName) {
        if (locationName.isEmpty() || mapView == null) return;

        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(locationName, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                GeoPoint searchPoint = new GeoPoint(address.getLatitude(), address.getLongitude());

                mapView.getOverlays().clear();
                Marker searchMarker = new Marker(mapView);
                searchMarker.setPosition(searchPoint);
                searchMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                searchMarker.setTitle(locationName);

                mapView.getOverlays().add(searchMarker);
                mapView.getController().animateTo(searchPoint);
                mapView.getController().setZoom(15.0);
                mapView.invalidate();

            } else {
                Toast.makeText(getContext(), "Location not found", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(getContext(), "Error searching location", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }

    private void setupTrendingList(View view) {
        rvTrending = view.findViewById(R.id.rvTrending);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        rvTrending.setLayoutManager(layoutManager);

        List<String> trendingData = new ArrayList<>();
        trendingData.add("All Issues");
        trendingData.add("Road Block");
        trendingData.add("Power Outage");
        trendingData.add("Water Supply");
        trendingData.add("Traffic Jam");
        trendingData.add("Garbage");

        TrendingAdapter trendingAdapter = new TrendingAdapter(trendingData, new TrendingAdapter.OnTrendingItemClickListener() {
            @Override
            public void onCategoryClick(String category) {
                filterReports(category);
            }
        });
        rvTrending.setAdapter(trendingAdapter);
    }

    private void filterReports(String category) {
        reportList.clear();

        if (category.equals("All Issues")) {
            reportList.addAll(allReportsList);
        } else {
            String searchPhrase = category.toLowerCase();
            for (Report report : allReportsList) {
                boolean matchInTitle = report.title != null && report.title.toLowerCase().contains(searchPhrase);
                boolean matchInDesc = report.description != null && report.description.toLowerCase().contains(searchPhrase);

                if (matchInTitle || matchInDesc || (report.category != null && report.category.toLowerCase().contains(searchPhrase))) {
                    reportList.add(report);
                }
            }
        }
        adapter.notifyDataSetChanged();
        Toast.makeText(getContext(), "Filtered: " + category, Toast.LENGTH_SHORT).show();
    }

    private void deleteReport(int position) {
        if (position >= reportList.size()) return;
        Report report = reportList.get(position);
        db.collection("reports").document(report.reportId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Deleted!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error deleting", Toast.LENGTH_SHORT).show());
    }

    private void updateReportStatus(int position) {
        if (position >= reportList.size()) return;
        Report report = reportList.get(position);
        db.collection("reports").document(report.reportId)
                .update("status", "Resolved")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Status Updated!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Update Failed", Toast.LENGTH_SHORT).show());
    }

    // ==========================================================
    // THE SMART SORTING ENGINE
    // ==========================================================
    private void fetchReports() {
        db.collection("reports").addSnapshotListener((queryDocumentSnapshots, error) -> {
            if (error != null) {
                Toast.makeText(getContext(), "Error loading feed.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (queryDocumentSnapshots != null) {
                reportList.clear();
                allReportsList.clear();

                List<DocumentSnapshot> list = queryDocumentSnapshots.getDocuments();
                for (DocumentSnapshot d : list) {
                    Report report = d.toObject(Report.class).withId(d.getId());
                    allReportsList.add(report);
                }

                // 1. Sort the Master List using our Custom Algorithm
                Collections.sort(allReportsList, new Comparator<Report>() {
                    @Override
                    public int compare(Report r1, Report r2) {
                        boolean r1HighPriority = r1.upvotes >= 10;
                        boolean r2HighPriority = r2.upvotes >= 10;

                        if (r1HighPriority && !r2HighPriority) {
                            return -1; // r1 is high priority, push it to the top
                        } else if (!r1HighPriority && r2HighPriority) {
                            return 1;  // r2 is high priority, push it to the top
                        } else {
                            // Either both are high priority, or both are normal.
                            // Sort by timestamp (newest first)
                            return Long.compare(r2.timestamp, r1.timestamp);
                        }
                    }
                });

                // 2. Copy the newly sorted list into the visible feed
                reportList.addAll(allReportsList);

                // 3. Update the screen
                adapter.notifyDataSetChanged();
            }
        });
    }
}
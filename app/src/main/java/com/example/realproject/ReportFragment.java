package com.example.realproject;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

// --- CLOUDINARY IMPORTS ---
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.cloudinary.android.policy.UploadPolicy;

// --- GOOGLE LOCATION IMPORTS ---
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportFragment extends Fragment {

    private TextInputEditText etTitle, etLocation, etDescription;
    private AutoCompleteTextView actvCategory;
    private Button btnSubmit;
    private FirebaseFirestore db;

    private Uri imageUri = null;
    private ImageView ivPreview;
    private TextView tvUploadText;

    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    public ReportFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_report, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        etTitle = view.findViewById(R.id.etTitle);
        etLocation = view.findViewById(R.id.etLocation);
        etDescription = view.findViewById(R.id.etDescription);
        actvCategory = view.findViewById(R.id.actvCategory);
        btnSubmit = view.findViewById(R.id.btnSubmit);
        View btnUpload = view.findViewById(R.id.btnUpload);

        ivPreview = view.findViewById(R.id.ivUploadPreview);
        tvUploadText = view.findViewById(R.id.tvUploadText);

        // ==========================================================
        // 1. THE CATEGORY DROPDOWN
        // ==========================================================
        String[] categories = {
                "Garbage & Solid Waste",
                "Potholes & Damaged Roads",
                "Broken or Flickering Streetlights",
                "Water Leakage or Contamination",
                "Power Outage or Sparking Wires",
                "Illegal Encroachment",
                "Faulty or Illegal Construction",
                "Traffic Jams & Broken Signals",
                "Waterlogging & Blocked Drains",
                "Stray Animal Menace",
                "Fallen Trees & Park Maintenance"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, categories);
        actvCategory.setAdapter(adapter);

        // Location Initialization
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) fetchLocation();
            else Toast.makeText(getContext(), "GPS Permission Denied", Toast.LENGTH_SHORT).show();
        });

        TextInputLayout tilLocation = view.findViewById(R.id.tilLocation);
        tilLocation.setEndIconOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fetchLocation();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        });

        // Image Picker
        ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        imageUri = result.getData().getData();
                        if (tvUploadText != null) tvUploadText.setText("Image Selected!");
                        if (ivPreview != null) {
                            ivPreview.setImageURI(imageUri);
                            ivPreview.clearColorFilter();
                        }
                    }
                }
        );

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            etTitle.setEnabled(false);
            etLocation.setEnabled(false);
            etDescription.setEnabled(false);
            actvCategory.setEnabled(false);
            etTitle.setHint("Login required to edit");

            btnUpload.setOnClickListener(v -> Toast.makeText(getContext(), "Login required", Toast.LENGTH_SHORT).show());
            btnSubmit.setText("Login Required to Submit");
            btnSubmit.setBackgroundColor(Color.parseColor("#B0BEC5"));
            btnSubmit.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.loginFragment));

        } else {
            btnUpload.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                galleryLauncher.launch(intent);
            });

            btnSubmit.setOnClickListener(v -> {
                String title = etTitle.getText().toString().trim();
                String location = etLocation.getText().toString().trim();
                String description = etDescription.getText().toString().trim();
                String category = actvCategory.getText().toString().trim();

                if (TextUtils.isEmpty(title)) { etTitle.setError("Required"); return; }
                if (TextUtils.isEmpty(category)) { actvCategory.setError("Required"); return; }
                if (TextUtils.isEmpty(description)) { etDescription.setError("Required"); return; }

                btnSubmit.setEnabled(false);
                Context appContext = requireActivity().getApplicationContext();

                // ==========================================================
                // 2. THE ROUTING BRAIN
                // ==========================================================
                String assignedAuthority = "";
                String targetTwitterHandle = "";

                switch (category) {
                    case "Garbage & Solid Waste":
                    case "Potholes & Damaged Roads":
                    case "Waterlogging & Blocked Drains":
                        assignedAuthority = "BMC - City Engineering & Sanitation";
                        targetTwitterHandle = "@bmcbbsr @HUDDeptOdisha";
                        break;
                    case "Broken or Flickering Streetlights":
                        assignedAuthority = "BMC - Electrical Wing";
                        targetTwitterHandle = "@bmcbbsr @HUDDeptOdisha";
                        break;
                    case "Water Leakage or Contamination":
                        assignedAuthority = "WATCO - City Distribution Division";
                        targetTwitterHandle = "@WATCO_Odisha";
                        break;
                    case "Power Outage or Sparking Wires":
                        assignedAuthority = "TPCODL - Local Section Office";
                        targetTwitterHandle = "@TPCODL @energyodisha";
                        break;
                    case "Illegal Encroachment":
                        assignedAuthority = "BDA / BMC - Joint Enforcement Squad";
                        targetTwitterHandle = "@BDA_Bhubaneswar @bmcbbsr";
                        break;
                    case "Faulty or Illegal Construction":
                    case "Fallen Trees & Park Maintenance":
                        assignedAuthority = "BDA - Bhubaneswar Development Authority";
                        targetTwitterHandle = "@BDA_Bhubaneswar";
                        break;
                    case "Traffic Jams & Broken Signals":
                        assignedAuthority = "Commissionerate Police - Traffic Bureau";
                        targetTwitterHandle = "@cpbbsrctc";
                        break;
                    case "Stray Animal Menace":
                        assignedAuthority = "BMC - Animal Welfare Cell";
                        targetTwitterHandle = "@bmcbbsr";
                        break;
                    default:
                        assignedAuthority = "General Administration";
                        targetTwitterHandle = "@bmcbbsr";
                        break;
                }

                Map<String, Object> reportMap = new HashMap<>();
                reportMap.put("title", title);
                reportMap.put("category", category); // Save selected category
                reportMap.put("assignedAuthority", assignedAuthority); // Save the routed department
                reportMap.put("targetTwitterHandle", targetTwitterHandle); // Save the mapped Twitter tags
                reportMap.put("location", location);
                reportMap.put("description", description);
                reportMap.put("status", "Pending");
                reportMap.put("userId", FirebaseAuth.getInstance().getUid());
                reportMap.put("timestamp", System.currentTimeMillis());
                reportMap.put("upvotes", 0); // Initialize upvotes

                if (imageUri != null) {
                    Toast.makeText(appContext, "Uploading report in background...", Toast.LENGTH_SHORT).show();
                    uploadImageAndSaveReport(appContext, reportMap);
                } else {
                    saveReportToFirestore(appContext, reportMap);
                }

                Navigation.findNavController(view).navigateUp();
            });
        }
    }

    @SuppressWarnings("MissingPermission")
    private void fetchLocation() {
        Toast.makeText(getContext(), "Locating...", Toast.LENGTH_SHORT).show();
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        try {
                            Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
                            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

                            if (addresses != null && !addresses.isEmpty()) {
                                etLocation.setText(addresses.get(0).getAddressLine(0));
                            } else {
                                etLocation.setText(location.getLatitude() + ", " + location.getLongitude());
                            }
                        } catch (Exception e) {
                            etLocation.setText(location.getLatitude() + ", " + location.getLongitude());
                        }
                    } else {
                        Toast.makeText(getContext(), "Could not find location. Ensure GPS is on.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void uploadImageAndSaveReport(Context appContext, Map<String, Object> reportMap) {
        MediaManager.get().upload(imageUri)
                .unsigned("localeye_upload")
                .policy(new UploadPolicy.Builder()
                        .networkPolicy(UploadPolicy.NetworkType.ANY)
                        .requiresCharging(false)
                        .build())
                .callback(new UploadCallback() {
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
                    @Override public void onReschedule(String requestId, ErrorInfo error) {}
                    @Override public void onError(String requestId, ErrorInfo error) {
                        Toast.makeText(appContext, "Upload Failed: " + error.getDescription(), Toast.LENGTH_LONG).show();
                    }
                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        reportMap.put("imageUrl", (String) resultData.get("secure_url"));
                        saveReportToFirestore(appContext, reportMap);
                    }
                }).dispatch();
    }

    private void saveReportToFirestore(Context appContext, Map<String, Object> reportMap) {
        FirebaseFirestore.getInstance().collection("reports").add(reportMap)
                .addOnSuccessListener(documentReference -> Toast.makeText(appContext, "Report Successfully Published!", Toast.LENGTH_LONG).show())
                .addOnFailureListener(e -> Toast.makeText(appContext, "Database Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
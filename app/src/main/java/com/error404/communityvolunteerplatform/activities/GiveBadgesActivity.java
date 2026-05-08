package com.error404.communityvolunteerplatform.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.error404.communityvolunteerplatform.databinding.ActivityGiveBadgesBinding;
import com.error404.communityvolunteerplatform.helpers.BadgeAwardHelper;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;

import java.util.List;

public class GiveBadgesActivity extends AppCompatActivity {

    private ActivityGiveBadgesBinding binding;
    private String currentEventOpportunityId;
    private FirebaseFirestore db;
    private boolean isProcessing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGiveBadgesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        currentEventOpportunityId = getIntent().getStringExtra("opportunityId");

        if (currentEventOpportunityId == null) {
            Toast.makeText(this, "No event selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        setupScanner();
    }

    private void setupScanner() {
        binding.barcodeScanner.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (isProcessing || result.getText() == null) return;
                processScannedData(result.getText());
            }

            @Override
            public void possibleResultPoints(List<com.google.zxing.ResultPoint> resultPoints) {}
        });
    }

    private void processScannedData(String data) {
        if (!data.startsWith("CVP::")) {
            Toast.makeText(this, "Invalid QR code", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] parts = data.split("::");
        if (parts.length != 4) {
            Toast.makeText(this, "Invalid QR format", Toast.LENGTH_SHORT).show();
            return;
        }

        String scannedOppId = parts[1];
        String volunteerId = parts[2];
        String applicationId = parts[3];

        if (!scannedOppId.equals(currentEventOpportunityId)) {
            Toast.makeText(this, "This QR code is for a different event. Cannot scan.", Toast.LENGTH_SHORT).show();
            return;
        }

        isProcessing = true;
        binding.barcodeScanner.pause();

        db.collection("applications").document(applicationId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String status = documentSnapshot.getString("status");
                        if ("completed".equals(status)) {
                            showErrorDialog("This volunteer has already received their badge.");
                        } else {
                            awardBadge(applicationId, volunteerId);
                        }
                    } else {
                        showErrorDialog("Application not found.");
                    }
                })
                .addOnFailureListener(e -> {
                    showErrorDialog("Failed to verify application: " + e.getMessage());
                });
    }

    private void awardBadge(String applicationId, String volunteerId) {
        WriteBatch batch = db.batch();
        batch.update(db.collection("applications").document(applicationId), "status", "completed");

        batch.commit().addOnSuccessListener(aVoid -> {
            // Call BadgeAwardHelper to update volunteer stats and award badges
            BadgeAwardHelper.recordEventCompletion(volunteerId, 1.0, this);
            
            fetchVolunteerNameAndShowSuccess(volunteerId);
        }).addOnFailureListener(e -> {
            showErrorDialog("Failed to award badge: " + e.getMessage());
        });
    }

    private void fetchVolunteerNameAndShowSuccess(String volunteerId) {
        db.collection("users").document(volunteerId).get()
                .addOnSuccessListener(doc -> {
                    String name = "Volunteer";
                    if (doc.exists()) {
                        name = doc.getString("name");
                    }
                    showSuccessDialog(name);
                })
                .addOnFailureListener(e -> showSuccessDialog("Volunteer"));
    }

    private void showSuccessDialog(String volunteerName) {
        new AlertDialog.Builder(this)
                .setTitle("Success")
                .setMessage("Badge awarded! " + volunteerName + "'s passport has been updated.")
                .setPositiveButton("OK", (dialog, which) -> resumeScanning())
                .setCancelable(false)
                .show();
    }

    private void showErrorDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> resumeScanning())
                .setCancelable(false)
                .show();
    }

    private void resumeScanning() {
        isProcessing = false;
        binding.barcodeScanner.resume();
    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.barcodeScanner.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        binding.barcodeScanner.pause();
    }
}
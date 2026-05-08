package com.error404.communityvolunteerplatform.activities;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.error404.communityvolunteerplatform.databinding.ActivityQrCodeBinding;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class QrCodeActivity extends AppCompatActivity {

    private ActivityQrCodeBinding binding;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQrCodeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();

        String applicationId = getIntent().getStringExtra("applicationId");
        String opportunityId = getIntent().getStringExtra("opportunityId");

        if (applicationId == null || opportunityId == null) {
            Toast.makeText(this, "Missing application information", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        fetchEventDetails(opportunityId);
        fetchQrTokenAndGenerate(applicationId);
    }

    private void fetchEventDetails(String opportunityId) {
        db.collection("opportunities").document(opportunityId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String title = documentSnapshot.getString("title");
                        binding.tvEventTitle.setText(title);
                    }
                });
    }

    private void fetchQrTokenAndGenerate(String applicationId) {
        db.collection("applications").document(applicationId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String qrToken = documentSnapshot.getString("qrToken");
                        if (qrToken != null) {
                            generateQrCode(qrToken);
                        } else {
                            Toast.makeText(this, "QR Token not found", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to fetch QR token", Toast.LENGTH_SHORT).show();
                });
    }

    private void generateQrCode(String token) {
        MultiFormatWriter writer = new MultiFormatWriter();
        try {
            BitMatrix bitMatrix = writer.encode(token, BarcodeFormat.QR_CODE, 512, 512);
            BarcodeEncoder encoder = new BarcodeEncoder();
            Bitmap bitmap = encoder.createBitmap(bitMatrix);
            binding.ivQrCode.setImageBitmap(bitmap);
        } catch (WriterException e) {
            Toast.makeText(this, "Error generating QR code", Toast.LENGTH_SHORT).show();
        }
    }
}
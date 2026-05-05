// activities/PassportActivity.java
// Fixed: correct package + all imports now point to com.error404... and helpers.*
package com.error404.communityvolunteerplatform.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.error404.communityvolunteerplatform.R;
import com.error404.communityvolunteerplatform.helpers.BadgeEngine;
import com.error404.communityvolunteerplatform.helpers.CloudinaryManager;
import com.error404.communityvolunteerplatform.helpers.GroqRecommendationHelper;
import com.error404.communityvolunteerplatform.models.Volunteer;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PassportActivity extends AppCompatActivity {

    private static final int REQUEST_PICK_IMAGE = 1001;

    private ImageView      ivProfilePhoto;
    private TextView       tvVolunteerName;
    private TextView       tvLocation;
    private TextView       tvHoursValue;
    private TextView       tvEventsValue;
    private TextView       tvPassportId;
    private ChipGroup      cgSkills;
    private LinearLayout   llBadgesContainer;
    private MaterialButton btnChangePhoto;
    private MaterialButton btnRemovePhoto;
    private MaterialButton btnEditProfile;
    private MaterialButton btnSharePassport;
    private ProgressBar    pbUpload;

    private MaterialCardView cvImpactCard;
    private TextView tvPassportImpactScore;
    private TextView tvPassportImpactSummary;
    private ProgressBar pbPassportImpact;

    private FirebaseFirestore db;
    private String            currentUserId;
    private Volunteer         currentVolunteer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passport);

        CloudinaryManager.init(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Passport");
        }

        bindViews();
        db            = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (currentUserId == null) {
            Toast.makeText(this, "Not logged in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadPassportData();
        loadImpactScore();
        btnChangePhoto.setOnClickListener(v -> openImagePicker());
        btnRemovePhoto.setOnClickListener(v -> removePhoto());
        btnEditProfile.setOnClickListener(v -> {
            startActivity(new Intent(this, EditProfileActivity.class));
        });
        btnSharePassport.setOnClickListener(v -> generateAndSharePassportPDF());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentUserId != null) {
            loadPassportData();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void loadImpactScore() {
        if (currentUserId == null) return;
        GroqRecommendationHelper.getImpactSummary(currentUserId,
                new GroqRecommendationHelper.OnImpactListener() {
                    @Override
                    public void onSuccess(String summary, int score) {
                        cvImpactCard.setVisibility(View.VISIBLE);
                        tvPassportImpactScore.setText(String.valueOf(score));
                        pbPassportImpact.setProgress(score);
                        tvPassportImpactSummary.setText(summary);
                    }

                    @Override
                    public void onError(String message) {
                        cvImpactCard.setVisibility(View.GONE);
                    }
                });
    }

    private void bindViews() {
        ivProfilePhoto    = findViewById(R.id.ivPassportPhoto);
        tvVolunteerName   = findViewById(R.id.tvPassportName);
        tvLocation        = findViewById(R.id.tvPassportLocation);
        tvHoursValue      = findViewById(R.id.tvPassportHours);
        tvEventsValue     = findViewById(R.id.tvPassportEvents);
        tvPassportId      = findViewById(R.id.tvPassportId);
        cgSkills          = findViewById(R.id.cgPassportSkills);
        llBadgesContainer = findViewById(R.id.llPassportBadges);
        btnChangePhoto    = findViewById(R.id.btnChangePhoto);
        btnRemovePhoto    = findViewById(R.id.btnRemovePhoto);
        btnEditProfile    = findViewById(R.id.btnEditProfile);
        btnSharePassport  = findViewById(R.id.btnSharePassport);
        pbUpload          = findViewById(R.id.pbUpload);

        cvImpactCard = findViewById(R.id.cvImpactCard);
        tvPassportImpactScore = findViewById(R.id.tvPassportImpactScore);
        tvPassportImpactSummary = findViewById(R.id.tvPassportImpactSummary);
        pbPassportImpact = findViewById(R.id.pbPassportImpact);
    }

    private void removePhoto() {
        if (currentUserId == null) return;

        pbUpload.setVisibility(View.VISIBLE);
        btnRemovePhoto.setEnabled(false);

        db.collection("volunteers")
                .document(currentUserId)
                .update("profilePicUrl", "")
                .addOnSuccessListener(aVoid -> {
                    pbUpload.setVisibility(View.GONE);
                    btnRemovePhoto.setEnabled(true);
                    btnRemovePhoto.setVisibility(View.GONE);
                    ivProfilePhoto.setImageResource(R.drawable.ic_default_avatar);
                    if (currentVolunteer != null) {
                        currentVolunteer.setProfilePicUrl("");
                    }
                    Toast.makeText(this, "Photo removed", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    pbUpload.setVisibility(View.GONE);
                    btnRemovePhoto.setEnabled(true);
                    Toast.makeText(this, "Failed to remove photo", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadPassportData() {
        db.collection("volunteers")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "Profile not found.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    currentVolunteer = doc.toObject(Volunteer.class);
                    if (currentVolunteer != null) {
                        currentVolunteer.setUserId(doc.getId());
                        populatePassport(currentVolunteer);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load passport.", Toast.LENGTH_SHORT).show());
    }

    private void populatePassport(Volunteer v) {
        tvVolunteerName.setText(v.getFullName());
        tvLocation.setText(v.getLocation() != null && !v.getLocation().isEmpty()
                ? v.getLocation() : "Location not set");

        String passId = v.getUserId().length() >= 8
                ? v.getUserId().substring(v.getUserId().length() - 8).toUpperCase()
                : v.getUserId().toUpperCase();
        tvPassportId.setText("ID: CVP-" + passId);

        tvHoursValue.setText(String.valueOf((int) v.getTotalHours()));
        tvEventsValue.setText(String.valueOf(v.getProjectsCompleted()));

        if (v.getProfilePicUrl() != null && !v.getProfilePicUrl().isEmpty()) {
            btnRemovePhoto.setVisibility(View.VISIBLE);
            String thumbUrl = CloudinaryManager.buildThumbnailUrl(v.getProfilePicUrl(), 200, 200);
            Glide.with(this)
                    .load(thumbUrl)
                    .transform(new CircleCrop())
                    .placeholder(R.drawable.ic_default_avatar)
                    .error(R.drawable.ic_default_avatar)
                    .into(ivProfilePhoto);
        } else {
            btnRemovePhoto.setVisibility(View.GONE);
            ivProfilePhoto.setImageResource(R.drawable.ic_default_avatar);
        }

        cgSkills.removeAllViews();
        if (v.getSkills() != null && !v.getSkills().isEmpty()) {
            for (String skill : v.getSkills()) {
                Chip chip = new Chip(this);
                chip.setText(skill);
                chip.setClickable(false);
                chip.setChipBackgroundColorResource(R.color.chip_skill_background);
                chip.setTextColor(ContextCompat.getColor(this, R.color.chip_skill_text));
                cgSkills.addView(chip);
            }
        } else {
            Chip placeholder = new Chip(this);
            placeholder.setText("No skills added yet");
            placeholder.setClickable(false);
            cgSkills.addView(placeholder);
        }

        populateBadges(v.getBadgeIds());
    }

    private void generateAndSharePassportPDF() {
        if (currentVolunteer == null) {
            Toast.makeText(this, "Profile data not loaded yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Generating PDF Passport...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Use a background task or just run on UI since it's simple
        new Thread(() -> {
            try {
                PdfDocument document = new PdfDocument();
                // A4 size: 595 x 842 points
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
                PdfDocument.Page page = document.startPage(pageInfo);
                Canvas canvas = page.getCanvas();

                int margin = (int) (40 * getResources().getDisplayMetrics().density / 2); // approximate 40dp
                if (margin < 40) margin = 40; // ensuring at least some margin

                Paint paint = new Paint();

                // 1. Header Rectangle
                paint.setColor(Color.parseColor("#1D9E75"));
                canvas.drawRect(0, 0, 595, 150, paint);

                // 2. Volunteer Name
                paint.setColor(Color.WHITE);
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                paint.setTextSize(28);
                canvas.drawText(currentVolunteer.getFullName(), margin, 70, paint);

                // 3. Subtitle
                paint.setTextSize(16);
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                canvas.drawText("Community Volunteer Passport", margin, 100, paint);

                // 4. Content - Stats
                paint.setColor(Color.BLACK);
                int y = 200;
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                paint.setTextSize(14);
                canvas.drawText("VOLUNTEER STATISTICS", margin, y, paint);
                y += 30;

                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                drawLabelValue(canvas, paint, "Total Hours Volunteered:", String.valueOf((int)currentVolunteer.getTotalHours()), margin, y);
                y += 25;
                drawLabelValue(canvas, paint, "Events Completed:", String.valueOf(currentVolunteer.getProjectsCompleted()), margin, y);
                y += 25;
                drawLabelValue(canvas, paint, "Badges Earned:", String.valueOf(currentVolunteer.getBadgeIds() != null ? currentVolunteer.getBadgeIds().size() : 0), margin, y);
                y += 50;

                // 5. Skills
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                canvas.drawText("SKILLS", margin, y, paint);
                y += 25;
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                String skillsStr = (currentVolunteer.getSkills() != null && !currentVolunteer.getSkills().isEmpty())
                        ? String.join(", ", currentVolunteer.getSkills())
                        : "No skills listed";
                
                // Handle text wrapping for skills if needed (simple version)
                if (skillsStr.length() > 70) {
                   canvas.drawText(skillsStr.substring(0, 70) + "...", margin, y, paint);
                } else {
                   canvas.drawText(skillsStr, margin, y, paint);
                }
                y += 50;

                // 6. Badges Details
                if (currentVolunteer.getBadgeIds() != null && !currentVolunteer.getBadgeIds().isEmpty()) {
                    paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                    canvas.drawText("BADGES EARNED", margin, y, paint);
                    y += 25;
                    paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                    for (String badgeId : currentVolunteer.getBadgeIds()) {
                        canvas.drawText("• " + BadgeEngine.getBadgeName(badgeId), margin + 10, y, paint);
                        y += 20;
                        if (y > 750) break; // simple page overflow prevention
                    }
                    y += 30;
                }

                // 7. Location
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                canvas.drawText("LOCATION", margin, y, paint);
                y += 25;
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                canvas.drawText(currentVolunteer.getLocation() != null ? currentVolunteer.getLocation() : "Not specified", margin, y, paint);

                // 8. Footer
                paint.setColor(Color.GRAY);
                paint.setTextSize(10);
                String date = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date());
                canvas.drawText("Generated by Community Volunteer Platform on " + date, margin, 810, paint);

                document.finishPage(page);

                File file = new File(getCacheDir(), "volunteer_passport.pdf");
                document.writeTo(new FileOutputStream(file));
                document.close();

                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    sharePDF(file);
                });

            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Error generating PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void drawLabelValue(Canvas canvas, Paint paint, String label, String value, int x, int y) {
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText(label, x, y, paint);
        float width = paint.measureText(label);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText(" " + value, x + width, y, paint);
    }

    private void sharePDF(File file) {
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Share Volunteer Passport"));
    }

    private void populateBadges(List<String> badgeIds) {
        llBadgesContainer.removeAllViews();

        if (badgeIds == null || badgeIds.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Complete events to earn badges!");
            empty.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            llBadgesContainer.addView(empty);
            return;
        }

        for (String badgeId : badgeIds) {
            View badgeView = getLayoutInflater()
                    .inflate(R.layout.item_badge, llBadgesContainer, false);

            ImageView ivIcon  = badgeView.findViewById(R.id.ivBadgeIcon);
            TextView  tvLabel = badgeView.findViewById(R.id.tvBadgeLabel);

            String drawableName = BadgeEngine.getBadgeDrawableName(badgeId);
            int    drawableRes  = getResources().getIdentifier(
                    drawableName, "drawable", getPackageName());

            if (drawableRes != 0) {
                ivIcon.setImageResource(drawableRes);
            } else {
                ivIcon.setImageResource(R.drawable.ic_badge_locked);
            }

            tvLabel.setText(BadgeEngine.getBadgeName(badgeId));

            badgeView.setOnLongClickListener(v -> {
                Toast.makeText(this,
                        BadgeEngine.getBadgeDescription(badgeId),
                        Toast.LENGTH_LONG).show();
                return true;
            });

            llBadgesContainer.addView(badgeView);
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_IMAGE
                && resultCode == Activity.RESULT_OK
                && data != null && data.getData() != null) {
            uploadPhoto(data.getData());
        }
    }

    private void uploadPhoto(Uri imageUri) {
        pbUpload.setVisibility(View.VISIBLE);
        btnChangePhoto.setEnabled(false);

        CloudinaryManager.uploadProfilePhoto(currentUserId, imageUri,
                new CloudinaryManager.OnUploadListener() {

                    @Override
                    public void onProgress(int percent) {
                        runOnUiThread(() -> pbUpload.setProgress(percent));
                    }

                    @Override
                    public void onSuccess(String secureUrl) {
                        db.collection("volunteers")
                                .document(currentUserId)
                                .update("profilePicUrl", secureUrl)
                                .addOnSuccessListener(unused -> runOnUiThread(() -> {
                                    pbUpload.setVisibility(View.GONE);
                                    btnChangePhoto.setEnabled(true);
                                    btnRemovePhoto.setVisibility(View.VISIBLE);

                                    if (currentVolunteer != null) {
                                        currentVolunteer.setProfilePicUrl(secureUrl);
                                        List<String> updatedBadges =
                                                BadgeEngine.mergeWithExisting(currentVolunteer);
                                        currentVolunteer.setBadgeIds(updatedBadges);
                                        db.collection("volunteers")
                                                .document(currentUserId)
                                                .update("badgeIds", updatedBadges);
                                        populatePassport(currentVolunteer);
                                    }

                                    Toast.makeText(PassportActivity.this,
                                            "Photo updated!", Toast.LENGTH_SHORT).show();
                                }));
                    }

                    @Override
                    public void onError(String errorMessage) {
                        runOnUiThread(() -> {
                            pbUpload.setVisibility(View.GONE);
                            btnChangePhoto.setEnabled(true);
                            Toast.makeText(PassportActivity.this,
                                    "Upload failed: " + errorMessage,
                                    Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }
}
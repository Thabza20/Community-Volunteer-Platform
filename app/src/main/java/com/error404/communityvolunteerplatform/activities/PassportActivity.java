// activities/PassportActivity.java
// Fixed: correct package + all imports now point to com.error404... and helpers.*
package com.error404.communityvolunteerplatform.activities;

import android.app.Activity;
import android.content.Intent;
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
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.error404.communityvolunteerplatform.R;
import com.error404.communityvolunteerplatform.helpers.BadgeEngine;
import com.error404.communityvolunteerplatform.helpers.CloudinaryManager;
import com.error404.communityvolunteerplatform.models.Volunteer;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

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
    private ProgressBar    pbUpload;

    private FirebaseFirestore db;
    private String            currentUserId;
    private Volunteer         currentVolunteer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passport);

        CloudinaryManager.init(this);

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
        btnChangePhoto.setOnClickListener(v -> openImagePicker());
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
        pbUpload          = findViewById(R.id.pbUpload);
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
                    if (currentVolunteer != null) populatePassport(currentVolunteer);
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
            String thumbUrl = CloudinaryManager.buildThumbnailUrl(v.getProfilePicUrl(), 200, 200);
            Glide.with(this)
                    .load(thumbUrl)
                    .transform(new CircleCrop())
                    .placeholder(R.drawable.ic_default_avatar)
                    .error(R.drawable.ic_default_avatar)
                    .into(ivProfilePhoto);
        } else {
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
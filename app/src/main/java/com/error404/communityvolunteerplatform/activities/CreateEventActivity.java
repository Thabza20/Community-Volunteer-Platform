package com.error404.communityvolunteerplatform.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.error404.communityvolunteerplatform.R;
import com.error404.communityvolunteerplatform.helpers.GroqRecommendationHelper;
import com.error404.communityvolunteerplatform.models.Opportunity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CreateEventActivity extends AppCompatActivity {

    private TextInputEditText etEventName, etDescription, etEventLocation, etDate, etVolunteersNeeded, etQualifications;
    private com.google.android.material.textfield.TextInputLayout tilQualifications;
    private Spinner spinnerCategory;
    private MaterialCheckBox cbRequiresExperience, cbRequiresQualification;
    private MaterialButton btnCreateEvent, btnGenerateAi;

    private FirebaseFirestore db;
    private String orgId;
    private String orgName;
    private String orgLocation;
    private String orgDescription;

    private boolean isEditMode = false;
    private String opportunityId;
    private Opportunity existingOpportunity;
    private String originalLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            orgId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            finish();
            return;
        }

        initializeViews();
        fetchOrgName();
        setupCategorySpinner();
        setupAiDescriptionLogic();

        isEditMode = getIntent().getBooleanExtra("EDIT_MODE", false);
        opportunityId = getIntent().getStringExtra("OPPORTUNITY_ID");

        if (isEditMode && opportunityId != null) {
            loadOpportunityData();
            btnCreateEvent.setText(R.string.update_event);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(R.string.edit_event);
            }
        }

        etDate.setOnClickListener(v -> showDatePicker());
        btnCreateEvent.setOnClickListener(v -> saveEvent());
    }

    private void initializeViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        etEventName = findViewById(R.id.etEventName);
        etDescription = findViewById(R.id.etDescription);
        etEventLocation = findViewById(R.id.etEventLocation);
        etDate = findViewById(R.id.etDate);
        etVolunteersNeeded = findViewById(R.id.etVolunteersNeeded);
        etQualifications = findViewById(R.id.etQualifications);
        tilQualifications = findViewById(R.id.tilQualifications);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        cbRequiresExperience = findViewById(R.id.cbRequiresExperience);
        cbRequiresQualification = findViewById(R.id.cbRequiresQualification);
        cbRequiresQualification.setOnCheckedChangeListener((buttonView, isChecked) -> {
            tilQualifications.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (!isChecked) {
                etQualifications.setText("");
                tilQualifications.setError(null);
            }
        });

        etQualifications.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilQualifications.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnCreateEvent = findViewById(R.id.btnCreateEvent);
        btnGenerateAi = findViewById(R.id.btnGenerateAi);
    }

    private void setupAiDescriptionLogic() {
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateAiButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        etEventName.addTextChangedListener(watcher);

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateAiButtonState();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnGenerateAi.setOnClickListener(v -> generateAiDescription());
    }

    private void updateAiButtonState() {
        String title = etEventName.getText() != null ? etEventName.getText().toString().trim() : "";
        boolean hasCategory = spinnerCategory.getSelectedItem() != null;
        btnGenerateAi.setEnabled(!title.isEmpty() && hasCategory);
    }

    private void generateAiDescription() {
        String title = etEventName.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();
        String location = etEventLocation.getText().toString().trim();
        String currentOrgName = (orgName != null) ? orgName : "Our Organisation";

        btnGenerateAi.setEnabled(false);
        btnGenerateAi.setText(R.string.ai_generating);

        GroqRecommendationHelper.generateOpportunityDescription(title, category, location, currentOrgName, new GroqRecommendationHelper.OnDescriptionListener() {
            @Override
            public void onSuccess(String description) {
                etDescription.setText(description);
                btnGenerateAi.setEnabled(true);
                btnGenerateAi.setText(R.string.ai_generate_btn);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(CreateEventActivity.this, "AI Error: " + message, Toast.LENGTH_SHORT).show();
                btnGenerateAi.setEnabled(true);
                btnGenerateAi.setText(R.string.ai_generate_btn);
            }
        });
    }

    private void fetchOrgName() {
        db.collection("organisations").document(orgId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        orgName = documentSnapshot.getString("orgName");
                        orgLocation = documentSnapshot.getString("location");
                        orgDescription = documentSnapshot.getString("orgDetails");
                    }
                });
    }

    private void setupCategorySpinner() {
        String[] categories = {
                Opportunity.CAT_COMMUNITY,
                Opportunity.CAT_EDUCATION,
                Opportunity.CAT_HEALTH,
                Opportunity.CAT_ENVIRONMENT,
                Opportunity.CAT_EMERGENCY,
                Opportunity.CAT_ANIMAL,
                Opportunity.CAT_ARTS,
                Opportunity.CAT_SKILLS,
                Opportunity.CAT_REMOTE
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    String date = String.format(Locale.getDefault(), "%d-%02d-%02d", year1, monthOfYear + 1, dayOfMonth);
                    etDate.setText(date);
                }, year, month, day);

        // Prevent selecting past dates in the UI
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void loadOpportunityData() {
        db.collection("opportunities").document(opportunityId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        existingOpportunity = documentSnapshot.toObject(Opportunity.class);
                        if (existingOpportunity != null) {
                            etEventName.setText(existingOpportunity.getTitle());
                            etDescription.setText(existingOpportunity.getOpportunityDescription());
                            etEventLocation.setText(existingOpportunity.getLocation());
                            originalLocation = existingOpportunity.getLocation();
                            etDate.setText(String.valueOf(existingOpportunity.getEventDate()));
                            etVolunteersNeeded.setText(String.valueOf(existingOpportunity.getSlotsTotal()));
                            cbRequiresExperience.setChecked(existingOpportunity.isRequiresExperience());
                            cbRequiresQualification.setChecked(existingOpportunity.isRequiresQualification());
                            if (existingOpportunity.isRequiresQualification()) {
                                etQualifications.setText(existingOpportunity.getRequiredQualificationsText());
                                tilQualifications.setVisibility(View.VISIBLE);
                            }
                            
                            // Set spinner category
                            ArrayAdapter adapter = (ArrayAdapter) spinnerCategory.getAdapter();
                            int position = adapter.getPosition(existingOpportunity.getCategory());
                            spinnerCategory.setSelection(position);
                        }
                    }
                });
    }

    private void saveEvent() {
        if (etEventName.getText() == null || etDescription.getText() == null ||
            etEventLocation.getText() == null || etDate.getText() == null || etVolunteersNeeded.getText() == null) {
            return;
        }

        String title = etEventName.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String eventLocation = etEventLocation.getText().toString().trim();
        String date = etDate.getText().toString().trim();
        String slotsStr = etVolunteersNeeded.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(description) || TextUtils.isEmpty(eventLocation) || TextUtils.isEmpty(date) || TextUtils.isEmpty(slotsStr)) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate Date
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            Date selectedDate = sdf.parse(date);
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            if (selectedDate != null && selectedDate.before(today.getTime())) {
                Toast.makeText(this, "invalid date: Event cannot be in the past", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (ParseException e) {
            Toast.makeText(this, "invalid date format", Toast.LENGTH_SHORT).show();
            return;
        }

        if (cbRequiresQualification.isChecked() && TextUtils.isEmpty(etQualifications.getText().toString().trim())) {
            tilQualifications.setError(getString(R.string.error_qualifications_required));
            Toast.makeText(this, R.string.error_qualifications_required, Toast.LENGTH_SHORT).show();
            return;
        }

        int slotsTotal = Integer.parseInt(slotsStr);

        if (isEditMode && existingOpportunity != null) {
            updateEvent(title, description, eventLocation, date, category, slotsTotal);
        } else {
            createEvent(title, description, eventLocation, date, category, slotsTotal);
        }
    }

    private void createEvent(String title, String description, String eventLocation, String date, String category, int slotsTotal) {
        Opportunity event = new Opportunity(orgId, orgName, title, eventLocation, orgDescription, description, category, slotsTotal);
        event.setEventDate(date);
        event.setRequiresExperience(cbRequiresExperience.isChecked());
        event.setRequiresQualification(cbRequiresQualification.isChecked());
        if (cbRequiresQualification.isChecked()) {
            event.setRequiredQualificationsText(etQualifications.getText().toString().trim());
        }

        btnCreateEvent.setEnabled(false);
        Toast.makeText(this, "Creating event...", Toast.LENGTH_SHORT).show();

        db.collection("opportunities").add(event)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(CreateEventActivity.this, "Event Created Successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnCreateEvent.setEnabled(true);
                    Toast.makeText(CreateEventActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateEvent(String title, String description, String eventLocation, String date, String category, int slotsTotal) {
        boolean locationChanged = !eventLocation.equals(originalLocation);

        existingOpportunity.setTitle(title);
        existingOpportunity.setOpportunityDescription(description);
        existingOpportunity.setLocation(eventLocation);
        existingOpportunity.setEventDate(date);
        existingOpportunity.setCategory(category);
        existingOpportunity.setSlotsTotal(slotsTotal);
        existingOpportunity.setRequiresExperience(cbRequiresExperience.isChecked());
        existingOpportunity.setRequiresQualification(cbRequiresQualification.isChecked());
        if (cbRequiresQualification.isChecked()) {
            existingOpportunity.setRequiredQualificationsText(etQualifications.getText().toString().trim());
        } else {
            existingOpportunity.setRequiredQualificationsText(null);
        }
        existingOpportunity.setUpdatedAt(com.google.firebase.Timestamp.now());

        btnCreateEvent.setEnabled(false);
        
        com.google.firebase.firestore.WriteBatch batch = db.batch();
        batch.set(db.collection("opportunities").document(opportunityId), existingOpportunity);

        if (locationChanged) {
            notifyApplicantsOfLocationChange(batch, title, eventLocation);
        } else {
            commitBatch(batch);
        }
    }

    private void notifyApplicantsOfLocationChange(com.google.firebase.firestore.WriteBatch batch, String title, String newLocation) {
        db.collection("applications")
                .whereEqualTo("opportunityId", opportunityId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String volunteerId = doc.getString("volunteerId");
                        if (volunteerId != null) {
                            java.util.Map<String, Object> notification = new java.util.HashMap<>();
                            notification.put("userId", volunteerId);
                            notification.put("title", "Location Change: " + title);
                            notification.put("message", "The location for '" + title + "' has changed to: " + newLocation + ". You can withdraw your application if this no longer suits you.");
                            notification.put("timestamp", com.google.firebase.Timestamp.now());
                            notification.put("read", false);
                            notification.put("type", "location_change");
                            notification.put("opportunityId", opportunityId);

                            batch.set(db.collection("notifications").document(), notification);
                        }
                    }
                    commitBatch(batch);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error notifying applicants: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    commitBatch(batch);
                });
    }

    private void commitBatch(com.google.firebase.firestore.WriteBatch batch) {
        batch.commit().addOnSuccessListener(aVoid -> {
            Toast.makeText(CreateEventActivity.this, R.string.event_updated_success, Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            btnCreateEvent.setEnabled(true);
            Toast.makeText(CreateEventActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}
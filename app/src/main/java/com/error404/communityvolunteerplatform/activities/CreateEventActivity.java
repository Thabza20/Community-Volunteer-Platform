package com.error404.communityvolunteerplatform.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.error404.communityvolunteerplatform.R;
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

    private TextInputEditText etEventName, etDescription, etDate, etVolunteersNeeded;
    private Spinner spinnerCategory;
    private MaterialCheckBox cbRequiresExperience, cbRequiresQualification;
    private MaterialButton btnCreateEvent;

    private FirebaseFirestore db;
    private String orgId;
    private String orgName;
    private String orgLocation;
    private String orgDescription;

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
        etDate = findViewById(R.id.etDate);
        etVolunteersNeeded = findViewById(R.id.etVolunteersNeeded);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        cbRequiresExperience = findViewById(R.id.cbRequiresExperience);
        cbRequiresQualification = findViewById(R.id.cbRequiresQualification);
        btnCreateEvent = findViewById(R.id.btnCreateEvent);
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

    private void saveEvent() {
        if (etEventName.getText() == null || etDescription.getText() == null ||
            etDate.getText() == null || etVolunteersNeeded.getText() == null) {
            return;
        }

        String title = etEventName.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String date = etDate.getText().toString().trim();
        String slotsStr = etVolunteersNeeded.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(description) || TextUtils.isEmpty(date) || TextUtils.isEmpty(slotsStr)) {
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

        int slotsTotal = Integer.parseInt(slotsStr);

        Opportunity event = new Opportunity(orgId, orgName, title, orgLocation, orgDescription, description, category, slotsTotal);
        event.setEventDate(date);
        event.setRequiresExperience(cbRequiresExperience.isChecked());
        event.setRequiresQualification(cbRequiresQualification.isChecked());

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
}
package com.error404.communityvolunteerplatform.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.error404.communityvolunteerplatform.R;
import com.error404.communityvolunteerplatform.adapters.OpportunityAdapter;
import com.error404.communityvolunteerplatform.models.Opportunity;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class BrowseOpportunitiesActivity extends AppCompatActivity {

    private EditText etSearchName;
    private Spinner spinnerCategory;
    private Button btnSearch;
    private RecyclerView rvOpportunities;
    private OpportunityAdapter adapter;
    private List<Opportunity> fullOpportunityList;
    private List<Opportunity> filteredList;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse_opportunities);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db = FirebaseFirestore.getInstance();
        etSearchName = findViewById(R.id.etSearchName);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        btnSearch = findViewById(R.id.btnSearch);
        rvOpportunities = findViewById(R.id.rvOpportunities);

        fullOpportunityList = new ArrayList<>();
        filteredList = new ArrayList<>();
        adapter = new OpportunityAdapter(filteredList, opportunity -> {
            Intent intent = new Intent(BrowseOpportunitiesActivity.this, OpportunityDetailsActivity.class);
            intent.putExtra("OPPORTUNITY_ID", opportunity.getOpportunityId());
            startActivity(intent);
        });

        rvOpportunities.setLayoutManager(new LinearLayoutManager(this));
        rvOpportunities.setAdapter(adapter);

        setupCategorySpinner();
        
        btnSearch.setOnClickListener(v -> performSearch());

        // Real-time filtering as the user types
        etSearchName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                performSearch();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Load initial data
        fetchOpportunities();
    }

    private void setupCategorySpinner() {
        String[] categories = {
                "All Categories",
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

    private void fetchOpportunities() {
        db.collection("opportunities")
                .whereEqualTo("status", Opportunity.STATUS_ACTIVE)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    fullOpportunityList.clear();
                    if (!queryDocumentSnapshots.isEmpty()) {
                        fullOpportunityList.addAll(queryDocumentSnapshots.toObjects(Opportunity.class));
                    }
                    performSearch();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching opportunities: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void performSearch() {
        String nameQuery = etSearchName.getText().toString().trim().toLowerCase();
        String selectedCategory = spinnerCategory.getSelectedItem().toString();

        filteredList.clear();

        for (Opportunity op : fullOpportunityList) {
            boolean matchesCategory = selectedCategory.equals("All Categories") || op.getCategory().equals(selectedCategory);
            boolean matchesName = nameQuery.isEmpty() || op.getTitle().toLowerCase().contains(nameQuery);

            if (matchesCategory && matchesName) {
                filteredList.add(op);
            }
        }

        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

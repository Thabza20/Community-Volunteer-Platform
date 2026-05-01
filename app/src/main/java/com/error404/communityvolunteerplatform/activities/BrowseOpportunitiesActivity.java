package com.error404.communityvolunteerplatform.activities;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.error404.communityvolunteerplatform.R;
import com.error404.communityvolunteerplatform.adapters.OpportunityAdapter;
import com.error404.communityvolunteerplatform.models.Opportunity;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BrowseOpportunitiesActivity extends AppCompatActivity {

    private EditText etSearchName;
    private Spinner spinnerCategory;
    private Button btnSearch;
    private RecyclerView rvOpportunities;
    private TabLayout tabLayout;
    private MapView mapView;
    private OpportunityAdapter adapter;
    private List<Opportunity> fullOpportunityList;
    private List<Opportunity> filteredList;
    private FirebaseFirestore db;
    private boolean isPinsLoaded = false;
    private Geocoder geocoder;

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
        Configuration.getInstance().load(this, android.preference.PreferenceManager.getDefaultSharedPreferences(this));
        geocoder = new Geocoder(this, Locale.getDefault());

        etSearchName = findViewById(R.id.etSearchName);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        btnSearch = findViewById(R.id.btnSearch);
        rvOpportunities = findViewById(R.id.rvOpportunities);
        tabLayout = findViewById(R.id.tabLayout);
        mapView = findViewById(R.id.mapView);

        initMap();

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

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    rvOpportunities.setVisibility(View.VISIBLE);
                    mapView.setVisibility(View.GONE);
                } else {
                    rvOpportunities.setVisibility(View.GONE);
                    mapView.setVisibility(View.VISIBLE);
                    if (!isPinsLoaded) {
                        loadOpportunityPins();
                    }
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Load initial data
        fetchOpportunities();
    }

    private void initMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(12.0);

        GeoPoint defaultPoint = new GeoPoint(-29.8587, 31.0218); // Durban
        mapView.getController().setCenter(defaultPoint);

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("volunteers").document(uid).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String location = documentSnapshot.getString("location");
                if (location != null && !location.isEmpty()) {
                    new Thread(() -> {
                        try {
                            List<Address> addresses = geocoder.getFromLocationName(location, 1);
                            if (addresses != null && !addresses.isEmpty()) {
                                GeoPoint userPoint = new GeoPoint(addresses.get(0).getLatitude(), addresses.get(0).getLongitude());
                                runOnUiThread(() -> mapView.getController().setCenter(userPoint));
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }).start();
                }
            }
        });
    }

    private void loadOpportunityPins() {
        isPinsLoaded = true;
        new Thread(() -> {
            for (Opportunity op : fullOpportunityList) {
                String loc = op.getLocation();
                if (loc == null || loc.isEmpty()) continue;

                try {
                    List<Address> addresses = geocoder.getFromLocationName(loc, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        GeoPoint point = new GeoPoint(addresses.get(0).getLatitude(), addresses.get(0).getLongitude());
                        runOnUiThread(() -> addMarker(op, point));
                    }
                } catch (IOException e) {
                    Log.e("MapGeocode", "Failed to geocode: " + loc);
                }
            }
        }).start();
    }

    private void addMarker(Opportunity op, GeoPoint point) {
        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setTitle(op.getTitle());
        marker.setSnippet(op.getLocation());
        
        // Custom Info Window
        CustomInfoWindow infoWindow = new CustomInfoWindow(R.layout.map_info_window, mapView, op.getOpportunityId());
        marker.setInfoWindow(infoWindow);
        
        mapView.getOverlays().add(marker);
        mapView.invalidate();
    }

    private class CustomInfoWindow extends MarkerInfoWindow {
        private String opportunityId;

        public CustomInfoWindow(int layoutResId, MapView mapView, String opportunityId) {
            super(layoutResId, mapView);
            this.opportunityId = opportunityId;
        }

        @Override
        public void onOpen(Object item) {
            super.onOpen(item);
            Marker marker = (Marker) item;
            
            TextView tvTitle = mView.findViewById(R.id.tvInfoTitle);
            TextView tvLocation = mView.findViewById(R.id.tvInfoLocation);
            
            tvTitle.setText(marker.getTitle());
            tvLocation.setText(marker.getSnippet());

            mView.setOnClickListener(v -> {
                Intent intent = new Intent(BrowseOpportunitiesActivity.this, OpportunityDetailsActivity.class);
                intent.putExtra("OPPORTUNITY_ID", opportunityId);
                startActivity(intent);
                close();
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
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

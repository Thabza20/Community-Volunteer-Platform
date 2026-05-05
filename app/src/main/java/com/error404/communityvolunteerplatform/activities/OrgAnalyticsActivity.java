package com.error404.communityvolunteerplatform.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.error404.communityvolunteerplatform.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class OrgAnalyticsActivity extends AppCompatActivity {

    private String orgId;
    private FirebaseFirestore db;
    private ProgressBar pbAnalytics;
    private TextView tvCompletionRateValue;

    private HorizontalBarChart chartAppFunnel;
    private PieChart chartCategoryPie;
    private LineChart chartAppTrends;
    private BarChart chartCompletionRate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_org_analytics);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Analytics Dashboard");
        }

        db = FirebaseFirestore.getInstance();
        orgId = getIntent().getStringExtra("orgId");
        if (orgId == null || orgId.isEmpty()) {
            orgId = FirebaseAuth.getInstance().getUid();
        }

        pbAnalytics = findViewById(R.id.pbAnalytics);
        tvCompletionRateValue = findViewById(R.id.tvCompletionRateValue);
        chartAppFunnel = findViewById(R.id.chartAppFunnel);
        chartCategoryPie = findViewById(R.id.chartCategoryPie);
        chartAppTrends = findViewById(R.id.chartAppTrends);
        chartCompletionRate = findViewById(R.id.chartCompletionRate);

        loadData();
    }

    private void loadData() {
        pbAnalytics.setVisibility(View.VISIBLE);

        Task<QuerySnapshot> applicationsTask = db.collection("applications")
                .whereEqualTo("orgId", orgId)
                .get();

        Task<QuerySnapshot> opportunitiesTask = db.collection("opportunities")
                .whereEqualTo("orgId", orgId)
                .get();

        Tasks.whenAllSuccess(applicationsTask, opportunitiesTask).addOnSuccessListener(results -> {
            pbAnalytics.setVisibility(View.GONE);
            QuerySnapshot appSnapshot = (QuerySnapshot) results.get(0);
            QuerySnapshot oppSnapshot = (QuerySnapshot) results.get(1);

            processApplicationData(appSnapshot);
            processOpportunityData(oppSnapshot);
        }).addOnFailureListener(e -> {
            pbAnalytics.setVisibility(View.GONE);
            Toast.makeText(this, "Error loading analytics: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void processApplicationData(QuerySnapshot snapshot) {
        int pending = 0, approved = 0, rejected = 0, completed = 0;
        Map<String, Integer> monthlyTrends = new TreeMap<>(); // Sorted by key

        // Initialize last 6 months
        Calendar cal = Calendar.getInstance();
        for (int i = 0; i < 6; i++) {
            String monthKey = String.format(Locale.getDefault(), "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1);
            monthlyTrends.put(monthKey, 0);
            cal.add(Calendar.MONTH, -1);
        }

        for (QueryDocumentSnapshot doc : snapshot) {
            String status = doc.getString("status");
            if (status != null) {
                switch (status) {
                    case "pending": pending++; break;
                    case "approved": approved++; break;
                    case "rejected": rejected++; break;
                    case "completed": completed++; break;
                }
            }

            com.google.firebase.Timestamp ts = doc.getTimestamp("createdAt");
            if (ts != null) {
                Calendar d = Calendar.getInstance();
                d.setTime(ts.toDate());
                String monthKey = String.format(Locale.getDefault(), "%04d-%02d", d.get(Calendar.YEAR), d.get(Calendar.MONTH) + 1);
                if (monthlyTrends.containsKey(monthKey)) {
                    monthlyTrends.put(monthKey, monthlyTrends.get(monthKey) + 1);
                }
            }
        }

        setupAppFunnelChart(pending, approved, rejected, completed);
        setupCompletionRate(approved, completed);
        setupTrendsChart(monthlyTrends);
    }

    private void setupAppFunnelChart(int pending, int approved, int rejected, int completed) {
        List<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0f, pending));
        entries.add(new BarEntry(1f, approved));
        entries.add(new BarEntry(2f, rejected));
        entries.add(new BarEntry(3f, completed));

        BarDataSet dataSet = new BarDataSet(entries, "Applications");
        dataSet.setColors(new int[]{Color.GRAY, Color.BLUE, Color.RED, Color.GREEN});
        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);

        chartAppFunnel.setData(barData);
        chartAppFunnel.getDescription().setEnabled(false);
        chartAppFunnel.getXAxis().setValueFormatter(new IndexAxisValueFormatter(new String[]{"Pending", "Approved", "Rejected", "Completed"}));
        chartAppFunnel.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chartAppFunnel.getXAxis().setDrawGridLines(false);
        chartAppFunnel.getAxisLeft().setAxisMinimum(0f);
        chartAppFunnel.invalidate();
    }

    private void setupCompletionRate(int approved, int completed) {
        float rate = 0;
        if ((approved + completed) > 0) {
            rate = (float) completed / (approved + completed) * 100;
        }
        tvCompletionRateValue.setText(String.format(Locale.getDefault(), "%.1f%%", rate));

        List<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0f, rate));

        BarDataSet dataSet = new BarDataSet(entries, "Completion %");
        dataSet.setColor(Color.parseColor("#1D9E75"));
        BarData barData = new BarData(dataSet);
        
        chartCompletionRate.setData(barData);
        chartCompletionRate.getAxisLeft().setAxisMaximum(100f);
        chartCompletionRate.getAxisLeft().setAxisMinimum(0f);
        chartCompletionRate.getAxisRight().setEnabled(false);
        chartCompletionRate.getXAxis().setEnabled(false);
        chartCompletionRate.getDescription().setEnabled(false);
        chartCompletionRate.getLegend().setEnabled(false);
        chartCompletionRate.invalidate();
    }

    private void setupTrendsChart(Map<String, Integer> trends) {
        List<Entry> entries = new ArrayList<>();
        final List<String> labels = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, Integer> entry : trends.entrySet()) {
            entries.add(new Entry(i, entry.getValue()));
            labels.add(entry.getKey());
            i++;
        }

        LineDataSet dataSet = new LineDataSet(entries, "Applications");
        dataSet.setColor(Color.BLUE);
        dataSet.setCircleColor(Color.BLUE);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(true);

        LineData lineData = new LineData(dataSet);
        chartAppTrends.setData(lineData);
        chartAppTrends.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chartAppTrends.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chartAppTrends.getXAxis().setGranularity(1f);
        chartAppTrends.getDescription().setEnabled(false);
        chartAppTrends.invalidate();
    }

    private void processOpportunityData(QuerySnapshot snapshot) {
        Map<String, Integer> categories = new HashMap<>();
        for (QueryDocumentSnapshot doc : snapshot) {
            String cat = doc.getString("category");
            if (cat == null) cat = "Other";
            categories.put(cat, categories.getOrDefault(cat, 0) + 1);
        }

        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : categories.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(ColorTemplate.COLORFUL_COLORS);
        PieData pieData = new PieData(dataSet);
        
        chartCategoryPie.setData(pieData);
        chartCategoryPie.getDescription().setEnabled(false);
        chartCategoryPie.setUsePercentValues(true);
        chartCategoryPie.setEntryLabelColor(Color.BLACK);
        chartCategoryPie.invalidate();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

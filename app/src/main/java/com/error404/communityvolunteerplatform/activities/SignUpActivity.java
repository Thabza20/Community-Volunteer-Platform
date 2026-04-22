package com.error404.communityvolunteerplatform.activities;

import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.error404.communityvolunteerplatform.R;
import com.google.android.material.tabs.TabLayout;

public class SignUpActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private LinearLayout layoutOrgForm, layoutVolForm;
    private CheckBox cbPopia;
    private TextView tvPopiaLink;
    private Button btnSignup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize Views
        tabLayout = findViewById(R.id.tabLayout);
        layoutOrgForm = findViewById(R.id.layoutOrgForm);
        layoutVolForm = findViewById(R.id.layoutVolForm);
        cbPopia = findViewById(R.id.cbPopia);
        tvPopiaLink = findViewById(R.id.tvPopiaLink);
        btnSignup = findViewById(R.id.btnSignup);

        // Setup Tab switching
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    layoutOrgForm.setVisibility(View.VISIBLE);
                    layoutVolForm.setVisibility(View.GONE);
                } else {
                    layoutOrgForm.setVisibility(View.GONE);
                    layoutVolForm.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Setup POPIA link popup
        setupPopiaLink();

        // Setup Signup button
        btnSignup.setOnClickListener(v -> {
            if (!cbPopia.isChecked()) {
                Toast.makeText(this, "Please consent to POPIA act to continue", Toast.LENGTH_SHORT).show();
            } else {
                String userType = tabLayout.getSelectedTabPosition() == 0 ? "Organization" : "Volunteer";
                Toast.makeText(this, "Signup successful as " + userType, Toast.LENGTH_LONG).show();
                // TODO: Implement actual signup logic (Firebase/Database)
            }
        });
    }

    private void setupPopiaLink() {
        String text = "I consent according to the POPIA act";
        SpannableString ss = new SpannableString(text);

        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                showPopiaDialog();
            }
        };

        // Make "POPIA" clickable (Indices 25 to 30)
        ss.setSpan(clickableSpan, 25, 30, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        tvPopiaLink.setText(ss);
        tvPopiaLink.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void showPopiaDialog() {
        new AlertDialog.Builder(this)
                .setTitle("POPIA Details")
                .setMessage("to do add POPIA details")
                .setPositiveButton("OK", null)
                .show();
    }
}
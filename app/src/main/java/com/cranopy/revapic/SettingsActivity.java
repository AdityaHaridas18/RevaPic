package com.cranopy.revapic;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Social Section
        setupSocialSection();
        
        // Help Section
        setupHelpSection();
        
        // Legal Section
        setupLegalSection();
    }

    private void setupSocialSection() {
        // Share App
        findViewById(R.id.shareApp).setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "RevaPic - Image Enhancement App");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out RevaPic - The best image enhancement app! Download now: [Your App Store Link]");
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        });

        // Instagram
        findViewById(R.id.instagram).setOnClickListener(v -> {
            openUrl("https://instagram.com/revapic");
        });

        // Facebook
        findViewById(R.id.facebook).setOnClickListener(v -> {
            openUrl("https://facebook.com/revapic");
        });
    }

    private void setupHelpSection() {
        // Help Center
        findViewById(R.id.helpCenter).setOnClickListener(v -> {
            openUrl("https://revapic.com/help");
        });

        // Contact Support
        findViewById(R.id.contactSupport).setOnClickListener(v -> {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:support@revapic.com"));
            startActivity(emailIntent);
        });

        // Suggest Feature
        findViewById(R.id.suggestFeature).setOnClickListener(v -> {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:features@revapic.com"));
            startActivity(emailIntent);
        });
    }

    private void setupLegalSection() {
        // Terms and Conditions
        findViewById(R.id.termsAndConditions).setOnClickListener(v -> {
            openUrl("https://revapic.com/terms");
        });

        // Privacy Policy
        findViewById(R.id.privacyPolicy).setOnClickListener(v -> {
            openUrl("https://revapic.com/privacy");
        });
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open link", Toast.LENGTH_SHORT).show();
        }
    }
} 
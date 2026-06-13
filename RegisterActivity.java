package com.example.mindpulse;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private EditText etFullName, etUsername, etEmail, etPassword;
    private Button btnSignUp;
    private TextView tvLoginLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        View mainView = findViewById(R.id.register_root_layout);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        setupHeaderActions();

        etFullName = findViewById(R.id.etFullName);
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnSignUp = findViewById(R.id.btnSignUp);
        tvLoginLink = findViewById(R.id.tvLoginLink);

        btnSignUp.setOnClickListener(v -> {
            String name = etFullName.getText().toString().trim();
            String user = etUsername.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();

            if (name.isEmpty() || user.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            } else {
                performRegistration(name, user, email, pass);
            }
        });

        tvLoginLink.setOnClickListener(v -> finish()); // Go back to Login
    }

    private void setupHeaderActions() {
        View headerView = findViewById(R.id.header);
        if (headerView != null) {
            ImageView ivMenu = headerView.findViewById(R.id.iv_menu);
            ImageView ivProfile = headerView.findViewById(R.id.iv_profile);
            ImageView ivLanguage = headerView.findViewById(R.id.iv_language);

            if (ivMenu != null) {
                ivMenu.setOnClickListener(v -> Toast.makeText(this, "Menu clicked", Toast.LENGTH_SHORT).show());
            }
            if (ivProfile != null) {
                ivProfile.setOnClickListener(v -> {
                    SharedPreferences prefs = getSharedPreferences("MindPulsePrefs", MODE_PRIVATE);
                    if (prefs.getString("auth_token", null) != null) {
                        startActivity(new Intent(this, ProfileActivity.class));
                    } else {
                        Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            if (ivLanguage != null) {
                ivLanguage.setOnClickListener(v -> showLanguageDialog());
            }
        }
    }

    private void showLanguageDialog() {
        String[] languages = {"English", "Amharic", "Oromo"};
        new AlertDialog.Builder(this)
                .setTitle("Select Language")
                .setItems(languages, (dialog, which) -> {
                    String selected = languages[which];
                    getSharedPreferences("MindPulsePrefs", MODE_PRIVATE).edit().putString("voice_lang", selected).apply();
                    Toast.makeText(this, "Language set to: " + selected, Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void performRegistration(String name, String username, String email, String password) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        RegisterRequest request = new RegisterRequest(name, username, email, password);

        apiService.registerUser(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(RegisterActivity.this, "Account Created! Please Login.", Toast.LENGTH_LONG).show();
                    finish(); // Closes registration and returns to LoginActivity
                } else {
                    Toast.makeText(RegisterActivity.this, "Registration Failed: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                Toast.makeText(RegisterActivity.this, "Connection Error!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

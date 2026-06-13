package com.example.mindpulse;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
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

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvForgot, tvGoToSignUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // Standardize System Bars Padding for consistent header level
        View rootLayout = findViewById(R.id.login_root_layout);
        if (rootLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        setupHeaderActions();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvForgot = findViewById(R.id.tvForgot);
        tvGoToSignUp = findViewById(R.id.tvGoToSignUp);

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString();
            String password = etPassword.getText().toString();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            } else {
                performLogin(email, password);
            }
        });

        tvForgot.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
        });

        tvGoToSignUp.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private void setupHeaderActions() {
        View headerView = findViewById(R.id.header);
        if (headerView != null) {
            ImageView ivMenu = headerView.findViewById(R.id.iv_menu);
            ImageView ivProfile = headerView.findViewById(R.id.iv_profile);
            ImageView ivLanguage = headerView.findViewById(R.id.iv_language);

            if (ivMenu != null) {
                ivMenu.setOnClickListener(v -> finish()); // Go back
            }
            if (ivProfile != null) {
                ivProfile.setOnClickListener(v -> {
                    SharedPreferences prefs = getSharedPreferences("MindPulsePrefs", MODE_PRIVATE);
                    if (prefs.getString("auth_token", null) != null) {
                        startActivity(new Intent(this, EditProfileActivity.class));
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

    private void performLogin(String email, String password) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        LoginRequest loginRequest = new LoginRequest(email, password);

        Log.d(TAG, "Attempting login for: " + email);

        apiService.loginUser(loginRequest).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();
                    
                    SharedPreferences prefs = getSharedPreferences("MindPulsePrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("auth_token", authResponse.getToken());
                    editor.putString("user_name", email.split("@")[0]); 
                    editor.putString("user_email", email);
                    editor.apply();

                    Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                    
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                } else {
                    String errorMsg = "Login Failed: " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            errorMsg += " - " + response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    Log.e(TAG, errorMsg);
                    Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                Log.e(TAG, "Network Error: " + t.getMessage(), t);
                Toast.makeText(LoginActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
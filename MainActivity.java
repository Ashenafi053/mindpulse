package com.example.mindpulse;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private boolean isUserLoggedIn = false;
    private TextView tvUserGreeting;
    private DrawerLayout drawerLayout;

    // Sidebar components
    private RecyclerView rvChatHistory;
    private ChatAdapter adapter;
    private List<ChatHistory> chatList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Initialize UI
        drawerLayout = findViewById(R.id.drawerLayout);
        tvUserGreeting = findViewById(R.id.tvUserGretting);

        // Handle System Bars Padding for consistent header level across all pages
        if (drawerLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(drawerLayout, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // --- 1. Session & Personalization ---
        SharedPreferences prefs = getSharedPreferences("MindPulsePrefs", MODE_PRIVATE);
        String token = prefs.getString("auth_token", null);
        isUserLoggedIn = (token != null);

        String name = prefs.getString("user_name", "User");
        int userId = prefs.getInt("userId", -1);
        String bearerToken = "Bearer " + token;

        tvUserGreeting.setText(isUserLoggedIn ? "Hi, " + name + "!" : "Hi, Guest!");

        // --- 2. Setup Navigation Drawer (Sidebar) ---
        setupNavigationDrawer(userId, bearerToken);

        // --- 3. Header Actions (Menu/Profile/Language) ---
        setupHeaderActions();

        // --- 4. Feature Cards ---
        setupFeatureCards();

        // --- 5. Quick Upload Button ---
        View btnAddQuick = findViewById(R.id.btnAddQuick);
        if (btnAddQuick != null) {
            btnAddQuick.setOnClickListener(v -> {
                if (isUserLoggedIn) openFilePicker();
                else redirectToLogin("upload documents");
            });
        }
    }

    private void setupNavigationDrawer(int userId, String token) {
        NavigationView navigationView = findViewById(R.id.navView);
        if (navigationView != null) {
            rvChatHistory = navigationView.findViewById(R.id.rvChatHistory);
            TextView btnSettings = navigationView.findViewById(R.id.btnSettings);

            if (rvChatHistory != null) {
                rvChatHistory.setLayoutManager(new LinearLayoutManager(this));
                adapter = new ChatAdapter(chatList);
                rvChatHistory.setAdapter(adapter);

                if (isUserLoggedIn) {
                    fetchChatHistoryFromBackend(userId, token);
                }
            }

            if (btnSettings != null) {
                btnSettings.setOnClickListener(v -> {
                    startActivity(new Intent(this, ProfileActivity.class));
                    drawerLayout.closeDrawer(GravityCompat.START);
                });
            }
        }
    }

    private void fetchChatHistoryFromBackend(int userId, String token) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.getUserDocuments(userId, token).enqueue(new Callback<List<ChatHistory>>() {
            @Override
            public void onResponse(Call<List<ChatHistory>> call, Response<List<ChatHistory>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    chatList.clear();
                    chatList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<List<ChatHistory>> call, Throwable t) {
                Log.e("API_ERROR", "Fetch history failed", t);
            }
        });
    }

    private void setupHeaderActions() {
        View headerView = findViewById(R.id.header);
        if (headerView != null) {
            ImageView ivProfile = headerView.findViewById(R.id.iv_profile);
            ImageView ivMenu = headerView.findViewById(R.id.iv_menu);
            ImageView ivLanguage = headerView.findViewById(R.id.iv_language);

            if (ivProfile != null) {
                ivProfile.setOnClickListener(v -> {
                    if (isUserLoggedIn) {
                        startActivity(new Intent(this, ProfileActivity.class));
                    } else {
                        startActivity(new Intent(this, LoginActivity.class));
                    }
                });
            }

            if (ivMenu != null) {
                ivMenu.setOnClickListener(v -> {
                    if (drawerLayout != null) {
                        drawerLayout.openDrawer(GravityCompat.START);
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

    private void setupFeatureCards() {
        View cardSummarization = findViewById(R.id.cardSummarization);
        View cardQuiz = findViewById(R.id.cardQuiz);
        View cardVoice = findViewById(R.id.cardVoice);

        if (cardSummarization != null) {
            populateCard(cardSummarization, "Summarization", "Condense PDFs into key points");
            cardSummarization.setOnClickListener(v -> handleFeatureClick("summary"));
        }
        if (cardQuiz != null) {
            populateCard(cardQuiz, "AI Quiz Generator", "Turn your knowledge automatically");
            cardQuiz.setOnClickListener(v -> handleFeatureClick("quiz"));
        }
        if (cardVoice != null) {
            populateCard(cardVoice, "Voice Reader", "Listen in English, Amharic and Oromo");
            cardVoice.setOnClickListener(v -> handleFeatureClick("voice"));
        }
    }

    private void handleFeatureClick(String feature) {
        if (isUserLoggedIn) {
            openFilePicker();
        } else {
            redirectToLogin("use " + feature);
        }
    }

    private void populateCard(View cardView, String title, String desc) {
        TextView tvTitle = cardView.findViewById(R.id.tvCardTitle);
        TextView tvDesc = cardView.findViewById(R.id.tvCardDesc);
        if (tvTitle != null) tvTitle.setText(title);
        if (tvDesc != null) tvDesc.setText(desc);
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        startActivityForResult(Intent.createChooser(intent, "Select PDF for MindPulse"), 101);
    }

    private void redirectToLogin(String action) {
        Toast.makeText(this, "Please login to " + action, Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, LoginActivity.class));
    }
}
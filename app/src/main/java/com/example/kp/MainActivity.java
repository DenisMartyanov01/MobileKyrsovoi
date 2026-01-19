package com.example.kp;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

public class MainActivity extends AppCompatActivity {

    private UserSession userSession;
    private DatabaseHelper dbHelper;
    private TextView userInfoTextView;
    private ImageView adminCrownIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        userSession = new UserSession(this);
        dbHelper = new DatabaseHelper(this);

        if (!userSession.isUserLoggedIn()) {
            redirectToLogin();
            return;
        }

        initViews();
        setupClickListeners();
        updateUserInfo();
    }

    private void initViews() {
        userInfoTextView = findViewById(R.id.userInfoTextView);
        adminCrownIcon = findViewById(R.id.adminCrownIcon);
    }

    private void setupClickListeners() {
        LinearLayout loginCard = findViewById(R.id.loginButton);
        loginCard.setOnClickListener(v -> {
            if (userSession.isUserLoggedIn()) {
                showLogoutDialog();
            } else {
                redirectToLogin();
            }
        });

        LinearLayout historyCard = findViewById(R.id.historyButton);
        historyCard.setOnClickListener(v -> {
            if (userSession.isUserLoggedIn()) {
                Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Требуется авторизация", Toast.LENGTH_SHORT).show();
                redirectToLogin();
            }
        });

        LinearLayout gcsCard = findViewById(R.id.card_dose_calculator);
        gcsCard.setOnClickListener(v -> {
            if (userSession.isUserLoggedIn()) {
                Intent intent = new Intent(MainActivity.this, GCSActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Требуется авторизация", Toast.LENGTH_SHORT).show();
                redirectToLogin();
            }
        });
    }

    private void updateUserInfo() {
        if (userSession.isUserLoggedIn()) {
            String username = userSession.getCurrentUsername();

            userInfoTextView.setText(username);

            if (userSession.isAdmin()) {
                adminCrownIcon.setVisibility(View.VISIBLE);
                userInfoTextView.setText(username + " (админ)");
            } else {
                adminCrownIcon.setVisibility(View.GONE);
            }
        } else {
            userInfoTextView.setText("Не авторизован");
            adminCrownIcon.setVisibility(View.GONE);
        }
    }

    private void showLogoutDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Выход");
        builder.setMessage("Вы уверены, что хотите выйти из системы?");

        builder.setPositiveButton("Выйти", (dialog, which) -> {
            userSession.logoutUser();
            Toast.makeText(MainActivity.this, "Вы вышли из системы", Toast.LENGTH_SHORT).show();
            redirectToLogin();
        });

        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_logout) {
            showLogoutDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (userSession.isUserLoggedIn()) {
            updateUserInfo();
        } else {
            redirectToLogin();
        }
    }

    @Override
    protected void onDestroy() {
        dbHelper.close();
        super.onDestroy();
    }
}
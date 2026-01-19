package com.example.kp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText usernameEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private TextView registerLink;
    private DatabaseHelper dbHelper;
    private UserSession userSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        dbHelper = new DatabaseHelper(this);
        userSession = new UserSession(this);

        if (userSession.isUserLoggedIn()) {
            redirectToMainActivity();
            return;
        }

        initViews();
        setupListeners();

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String username = extras.getString("username", "");
            if (!TextUtils.isEmpty(username)) {
                usernameEditText.setText(username);
                passwordEditText.requestFocus();
            }
        }
    }

    private void initViews() {
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        registerLink = findViewById(R.id.registerLink);
    }

    private void setupListeners() {
        loginButton.setOnClickListener(v -> loginUser());

        registerLink.setOnClickListener(v -> showRegistrationDialog());

    }

    private void loginUser() {
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Введите логин и пароль", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseHelper.User user = dbHelper.authenticateUser(username, password);

        if (user != null) {
            userSession.createUserSession(user.getId(), user.getUsername(), user.getRole());

            String welcomeMessage = user.getRole().equals("admin") ?
                    "Добро пожаловать, администратор!" :
                    "Добро пожаловать, " + user.getUsername() + "!";
            Toast.makeText(this, welcomeMessage, Toast.LENGTH_SHORT).show();

            redirectToMainActivity();
        } else {
            Toast.makeText(this, "Неверный логин или пароль", Toast.LENGTH_SHORT).show();
        }
    }

    private void showRegistrationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Регистрация нового пользователя");

        androidx.appcompat.app.AlertDialog.Builder alertDialogBuilder = new androidx.appcompat.app.AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Регистрация");

        final EditText regUsernameInput = new EditText(this);
        regUsernameInput.setHint("Логин");
        regUsernameInput.setPadding(32, 32, 32, 16);

        final EditText regPasswordInput = new EditText(this);
        regPasswordInput.setHint("Пароль");
        regPasswordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        regPasswordInput.setPadding(32, 16, 32, 16);

        final EditText confirmPasswordInput = new EditText(this);
        confirmPasswordInput.setHint("Подтвердите пароль");
        confirmPasswordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        confirmPasswordInput.setPadding(32, 16, 32, 32);

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.addView(regUsernameInput);
        layout.addView(regPasswordInput);
        layout.addView(confirmPasswordInput);

        alertDialogBuilder.setView(layout);

        alertDialogBuilder.setPositiveButton("Зарегистрировать", (dialog, which) -> {
            String username = regUsernameInput.getText().toString().trim();
            String password = regPasswordInput.getText().toString().trim();
            String confirmPassword = confirmPasswordInput.getText().toString().trim();

            registerUser(username, password, confirmPassword);
        });

        alertDialogBuilder.setNegativeButton("Отмена", (dialog, which) -> dialog.cancel());

        alertDialogBuilder.show();
    }

    private void registerUser(String username, String password, String confirmPassword) {
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, "Все поля обязательны для заполнения", Toast.LENGTH_SHORT).show();
            return;
        }

        if (username.length() < 3) {
            Toast.makeText(this, "Логин должен содержать минимум 3 символа", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 4) {
            Toast.makeText(this, "Пароль должен содержать минимум 4 символа", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show();
            return;
        }

        if (dbHelper.userExists(username)) {
            Toast.makeText(this, "Пользователь с таким логином уже существует", Toast.LENGTH_SHORT).show();
            return;
        }

        long userId = dbHelper.addUser(username, password, "user");

        if (userId != -1) {
            Toast.makeText(this, "Регистрация успешна! Теперь вы можете войти", Toast.LENGTH_SHORT).show();

            usernameEditText.setText(username);
            passwordEditText.setText("");
            passwordEditText.requestFocus();
        } else {
            Toast.makeText(this, "Ошибка при регистрации", Toast.LENGTH_SHORT).show();
        }
    }

    private void redirectToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        dbHelper.close();
        super.onDestroy();
    }
}
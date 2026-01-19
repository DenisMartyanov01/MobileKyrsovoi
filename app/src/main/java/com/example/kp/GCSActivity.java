package com.example.kp;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class GCSActivity extends AppCompatActivity {

    private RadioGroup eyesRadioGroup;
    private RadioGroup verbalRadioGroup;
    private RadioGroup motorRadioGroup;

    private CardView resultContainer;
    private TextView totalScoreTextView;
    private TextView scoreBreakdownTextView;
    private TextView interpretationTextView;

    private DatabaseHelper dbHelper;

    private int eyesScore = 0;
    private int verbalScore = 0;
    private int motorScore = 0;
    private int totalScore = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gcs);

        UserSession userSession = new UserSession(this);
        dbHelper = new DatabaseHelper(this);

        initViews();

        View buttonToMain = findViewById(R.id.button_to_main);
        buttonToMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(GCSActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        Button calculateButton = findViewById(R.id.calculateButton);
        calculateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calculateGCS();
            }
        });

        setupRadioGroupListeners();
    }

    private void initViews() {
        eyesRadioGroup = findViewById(R.id.eyesRadioGroup);
        verbalRadioGroup = findViewById(R.id.verbalRadioGroup);
        motorRadioGroup = findViewById(R.id.motorRadioGroup);

        resultContainer = findViewById(R.id.resultContainer);
        totalScoreTextView = findViewById(R.id.totalScoreTextView);
        scoreBreakdownTextView = findViewById(R.id.scoreBreakdownTextView);
        interpretationTextView = findViewById(R.id.interpretationTextView);
    }

    private void setupRadioGroupListeners() {
        eyesRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                updateEyesScore(checkedId);
            }
        });

        verbalRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                updateVerbalScore(checkedId);
            }
        });

        motorRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                updateMotorScore(checkedId);
            }
        });
    }

    private void updateEyesScore(int checkedId) {
        if (checkedId == R.id.eyesSpontaneous) {
            eyesScore = 4;
        } else if (checkedId == R.id.eyesToSpeech) {
            eyesScore = 3;
        } else if (checkedId == R.id.eyesToPain) {
            eyesScore = 2;
        } else if (checkedId == R.id.eyesNone) {
            eyesScore = 1;
        } else {
            eyesScore = 0;
        }
    }

    private void updateVerbalScore(int checkedId) {
        if (checkedId == R.id.verbalOriented) {
            verbalScore = 5;
        } else if (checkedId == R.id.verbalConfused) {
            verbalScore = 4;
        } else if (checkedId == R.id.verbalInappropriate) {
            verbalScore = 3;
        } else if (checkedId == R.id.verbalIncomprehensible) {
            verbalScore = 2;
        } else if (checkedId == R.id.verbalNone) {
            verbalScore = 1;
        } else {
            verbalScore = 0;
        }
    }

    private void updateMotorScore(int checkedId) {
        if (checkedId == R.id.motorObeys) {
            motorScore = 6;
        } else if (checkedId == R.id.motorLocalizes) {
            motorScore = 5;
        } else if (checkedId == R.id.motorWithdraws) {
            motorScore = 4;
        } else if (checkedId == R.id.motorFlexion) {
            motorScore = 3;
        } else if (checkedId == R.id.motorExtension) {
            motorScore = 2;
        } else if (checkedId == R.id.motorNone) {
            motorScore = 1;
        } else {
            motorScore = 0;
        }
    }

    private void calculateGCS() {
        if (eyesScore == 0 || verbalScore == 0 || motorScore == 0) {
            Toast.makeText(this, "Пожалуйста, выберите все параметры оценки", Toast.LENGTH_SHORT).show();
            return;
        }

        totalScore = eyesScore + verbalScore + motorScore;

        displayResults();
        saveResultToDatabase();
    }

    private void displayResults() {
        resultContainer.setVisibility(View.VISIBLE);

        totalScoreTextView.setText(String.valueOf(totalScore));

        setScoreColor(totalScore);

        String breakdown = "Глаза: " + eyesScore + " баллов\n" +
                "Речь: " + verbalScore + " баллов\n" +
                "Двигательная: " + motorScore + " баллов";
        scoreBreakdownTextView.setText(breakdown);

        String interpretation = getInterpretation(totalScore);
        interpretationTextView.setText(interpretation);

        resultContainer.post(new Runnable() {
            @Override
            public void run() {
                resultContainer.requestFocus();
            }
        });
    }

    private void setScoreColor(int score) {
        int color;

        if (score >= 13) {
            color = Color.parseColor("#4CAF50");
        } else if (score >= 9) {
            color = Color.parseColor("#FF9800");
        } else {
            color = Color.parseColor("#F44336");
        }

        totalScoreTextView.setTextColor(color);
    }

    private String getInterpretation(int score) {
        if (score == 15) {
            return "Ясное сознание";
        } else if (score >= 13 && score <= 14) {
            return "Оглушение";
        } else if (score >= 9 && score <= 12) {
            return "Сопор";
        } else if (score >= 3 && score <= 8) {
            return "Кома";
        } else {
            return "Невозможно оценить";
        }
    }

    private void saveResultToDatabase() {
        String interpretation = getInterpretation(totalScore);
        String resultString = "GCS: " + totalScore + " (" + interpretation +
                ") [Глаза: " + eyesScore + ", Речь: " + verbalScore +
                ", Двигательная: " + motorScore + "]";

        UserSession userSession = new UserSession(this);
        int userId = userSession.getCurrentUserId();

        long id = dbHelper.addGcsResult(resultString, userId);

        if (id != -1) {
            Toast.makeText(this, "Результат сохранен в историю", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Ошибка сохранения", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        dbHelper.close();
        super.onDestroy();
    }
}
package com.example.kp;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private DatabaseHelper dbHelper;
    private UserSession userSession;
    private TextView emptyTextView;
    private Button clearAllButton;
    private List<DatabaseHelper.GcsResult> gcsResults;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        View buttonToMain = findViewById(R.id.button_to_main);
        buttonToMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HistoryActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        dbHelper = new DatabaseHelper(this);
        userSession = new UserSession(this);

        if (!userSession.isUserLoggedIn()) {
            Toast.makeText(this, "Требуется авторизация", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        loadHistoryData();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        emptyTextView = findViewById(R.id.emptyTextView);
        clearAllButton = findViewById(R.id.clearAllButton);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Показываем/скрываем кнопку очистки истории
        if (userSession.isAdmin()) {
            clearAllButton.setVisibility(View.VISIBLE);
            clearAllButton.setOnClickListener(v -> showClearAllConfirmationDialog());
        } else {
            clearAllButton.setVisibility(View.GONE);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadHistoryData() {
        if (userSession.isAdmin()) {
            gcsResults = dbHelper.getAllGcsResults();
        } else {
            int userId = userSession.getCurrentUserId();
            gcsResults = dbHelper.getUserGcsResults(userId);
        }

        adapter = new HistoryAdapter(gcsResults, userSession.isAdmin());
        recyclerView.setAdapter(adapter);

        if (gcsResults.isEmpty()) {
            emptyTextView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyTextView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void deleteHistoryItem(int position) {
        if (position < 0 || position >= gcsResults.size()) return;

        DatabaseHelper.GcsResult item = gcsResults.get(position);

        new AlertDialog.Builder(this)
                .setTitle("Удаление записи")
                .setMessage("Вы уверены, что хотите удалить эту запись?")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    boolean success = dbHelper.deleteGcsResult(item.getId());
                    if (success) {
                        gcsResults.remove(position);
                        adapter.notifyItemRemoved(position);
                        Toast.makeText(this, "Запись удалена", Toast.LENGTH_SHORT).show();

                        if (gcsResults.isEmpty()) {
                            emptyTextView.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                        }
                    } else {
                        Toast.makeText(this, "Ошибка при удалении", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showClearAllConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Очистка всей истории")
                .setMessage("Вы уверены, что хотите удалить всю историю расчетов?")
                .setPositiveButton("Очистить", (dialog, which) -> clearAllHistory())
                .setNegativeButton("Отмена", null)
                .show();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void clearAllHistory() {
        if (userSession.isAdmin()) {
            dbHelper.clearAllGcsResults();
        } else {
            dbHelper.clearUserGcsResults(userSession.getCurrentUserId());
        }

        gcsResults.clear();
        adapter.notifyDataSetChanged();

        emptyTextView.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        Toast.makeText(this, "История очищена", Toast.LENGTH_SHORT).show();
    }

    private class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

        private final List<DatabaseHelper.GcsResult> items;
        private final boolean isAdmin;

        public HistoryAdapter(List<DatabaseHelper.GcsResult> items, boolean isAdmin) {
            this.items = items;
            this.isAdmin = isAdmin;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_history, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DatabaseHelper.GcsResult item = items.get(position);

            holder.resultTextView.setText(item.toString());

            if (isAdmin) {
                holder.deleteButton.setVisibility(View.VISIBLE);
                holder.deleteButton.setOnClickListener(v ->
                        deleteHistoryItem(holder.getAdapterPosition()));
            } else {
                holder.deleteButton.setVisibility(View.GONE);
            }

        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView resultTextView;
            Button deleteButton;
            ViewHolder(View itemView) {
                super(itemView);
                resultTextView = itemView.findViewById(R.id.resultTextView);
                deleteButton = itemView.findViewById(R.id.deleteButton);

                itemView.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        DatabaseHelper.GcsResult item = items.get(position);
                        showItemDetailsDialog(item);
                    }
                });
            }
        }
    }

    private void showItemDetailsDialog(DatabaseHelper.GcsResult item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Детали расчета");

        String formattedText = item.toString().replace("\\n", "\n");

        builder.setMessage(formattedText);
        builder.setPositiveButton("OK", null);

        if (userSession.isAdmin()) {
            builder.setNeutralButton("Удалить", (dialog, which) -> {
                for (int i = 0; i < gcsResults.size(); i++) {
                    if (gcsResults.get(i).getId() == item.getId()) {
                        deleteHistoryItem(i);
                        break;
                    }
                }
            });
        }

        builder.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHistoryData();
    }

    @Override
    protected void onDestroy() {
        dbHelper.close();
        super.onDestroy();
    }
}
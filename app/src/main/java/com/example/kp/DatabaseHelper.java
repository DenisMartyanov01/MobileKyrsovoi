package com.example.kp;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "medical_calculations.db";
    private static final int DATABASE_VERSION = 4;

    private static final String TABLE_USERS = "users";
    private static final String TABLE_GCS = "gcs";

    private static final String KEY_ID = "_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_ROLE = "role";
    private static final String KEY_CREATED_AT = "created_at";
    private static final String KEY_RESULT = "result";
    private static final String KEY_USER_ID = "user_id";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_USERNAME + " TEXT UNIQUE NOT NULL,"
                + KEY_PASSWORD + " TEXT NOT NULL,"
                + KEY_ROLE + " TEXT DEFAULT 'user',"
                + KEY_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP"
                + ")";
        db.execSQL(CREATE_USERS_TABLE);

        String CREATE_GCS_TABLE = "CREATE TABLE " + TABLE_GCS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_RESULT + " TEXT NOT NULL,"
                + KEY_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP,"
                + KEY_USER_ID + " INTEGER,"
                + "FOREIGN KEY (" + KEY_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + KEY_ID + ")"
                + ")";
        db.execSQL(CREATE_GCS_TABLE);
        createDefaultAdmin(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d("DatabaseHelper", "Upgrading database from version " + oldVersion + " to " + newVersion);

        if (oldVersion < 4) {
            try {
                db.execSQL("CREATE TABLE gcs_temp AS SELECT * FROM gcs");

                db.execSQL("DROP TABLE IF EXISTS gcs");

                String CREATE_GCS_TABLE = "CREATE TABLE " + TABLE_GCS + "("
                        + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + KEY_RESULT + " TEXT NOT NULL,"
                        + KEY_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP,"
                        + KEY_USER_ID + " INTEGER,"
                        + "FOREIGN KEY (" + KEY_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + KEY_ID + ")"
                        + ")";
                db.execSQL(CREATE_GCS_TABLE);

                db.execSQL("INSERT INTO gcs (_id, result, created_at, user_id) " +
                        "SELECT _id, result, created_at, 1 FROM gcs_temp");

                db.execSQL("DROP TABLE IF EXISTS gcs_temp");

                Log.d("DatabaseHelper", "Database upgraded to version 4 successfully");
            } catch (Exception e) {
                Log.e("DatabaseHelper", "Error upgrading database: " + e.getMessage());
                db.execSQL("DROP TABLE IF EXISTS gcs");
                onCreate(db);
            }
        }
    }

    private void createDefaultAdmin(SQLiteDatabase db) {
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE " + KEY_USERNAME + " = 'admin'", null);
        if (cursor.getCount() == 0) {
            ContentValues values = new ContentValues();
            values.put(KEY_USERNAME, "admin");
            values.put(KEY_PASSWORD, "admin123");
            values.put(KEY_ROLE, "admin");

            db.insert(TABLE_USERS, null, values);
        }
        cursor.close();
    }
    public User authenticateUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        User user = null;

        Cursor cursor = db.query(TABLE_USERS,
                new String[]{KEY_ID, KEY_USERNAME, KEY_ROLE, KEY_CREATED_AT},
                KEY_USERNAME + " = ? AND " + KEY_PASSWORD + " = ?",
                new String[]{username, password},
                null, null, null, "1");

        if (cursor != null && cursor.moveToFirst()) {
            try {
                @SuppressLint("Range") int id = cursor.getInt(cursor.getColumnIndex(KEY_ID));
                @SuppressLint("Range") String userName = cursor.getString(cursor.getColumnIndex(KEY_USERNAME));
                @SuppressLint("Range") String role = cursor.getString(cursor.getColumnIndex(KEY_ROLE));
                @SuppressLint("Range") String createdAt = cursor.getString(cursor.getColumnIndex(KEY_CREATED_AT));

                user = new User(id, userName, role, createdAt);
                Log.d("DatabaseHelper", "Authenticated user: " + userName + ", ID: " + id + ", role: " + role);
            } catch (Exception e) {
                e.printStackTrace();
            }
            cursor.close();
        }
        db.close();
        return user;
    }

    public boolean userExists(String username) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_USERS,
                new String[]{KEY_ID},
                KEY_USERNAME + " = ?",
                new String[]{username},
                null, null, null, "1");

        boolean exists = cursor != null && cursor.getCount() > 0;

        if (cursor != null) {
            cursor.close();
        }
        db.close();
        return exists;
    }

    public long addUser(String username, String password, String role) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_USERNAME, username);
        values.put(KEY_PASSWORD, password);
        values.put(KEY_ROLE, role);

        long id = -1;
        try {
            id = db.insert(TABLE_USERS, null, values);
            Log.d("DatabaseHelper", "User added: " + username + ", ID: " + id);
        } catch (SQLiteException e) {
            e.printStackTrace();
        }

        db.close();
        return id;
    }

    public long addGcsResult(String result, int userId) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_RESULT, result);
        values.put(KEY_CREATED_AT, getCurrentDateTime());
        values.put(KEY_USER_ID, userId);

        long id = -1;
        try {
            id = db.insert(TABLE_GCS, null, values);
            Log.d("DatabaseHelper", "GCS результат добавлен для user_id: " + userId + ", ID: " + id);
        } catch (SQLiteException e) {
            e.printStackTrace();
            Log.e("DatabaseHelper", "Ошибка при добавлении GCS результата: " + e.getMessage());
        }

        db.close();
        return id;
    }

    public ArrayList<GcsResult> getAllGcsResults() {
        ArrayList<GcsResult> results = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT g." + KEY_ID + ", g." + KEY_RESULT + ", g." + KEY_CREATED_AT +
                ", g." + KEY_USER_ID + ", u." + KEY_USERNAME +
                " FROM " + TABLE_GCS + " g" +
                " LEFT JOIN " + TABLE_USERS + " u ON g." + KEY_USER_ID + " = u." + KEY_ID +
                " ORDER BY g." + KEY_CREATED_AT + " DESC";

        Cursor cursor = db.rawQuery(query, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                @SuppressLint("Range") int id = cursor.getInt(cursor.getColumnIndex(KEY_ID));
                @SuppressLint("Range") String result = cursor.getString(cursor.getColumnIndex(KEY_RESULT));
                @SuppressLint("Range") String createdAt = cursor.getString(cursor.getColumnIndex(KEY_CREATED_AT));
                @SuppressLint("Range") int userId = cursor.getInt(cursor.getColumnIndex(KEY_USER_ID));
                @SuppressLint("Range") String username = cursor.getString(cursor.getColumnIndex(KEY_USERNAME));

                // Если username пустой (например, если пользователь удален), показываем "Неизвестный пользователь"
                if (username == null || username.isEmpty()) {
                    username = "Неизвестный пользователь";
                }

                GcsResult gcsResult = new GcsResult(id, result, createdAt, userId, username);
                results.add(gcsResult);
            } while (cursor.moveToNext());

            cursor.close();
        } else {
            Log.d("DatabaseHelper", "Нет результатов GCS в базе данных");
        }

        db.close();
        return results;
    }
    public ArrayList<GcsResult> getUserGcsResults(int userId) {
        ArrayList<GcsResult> results = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT g." + KEY_ID + ", g." + KEY_RESULT + ", g." + KEY_CREATED_AT +
                ", g." + KEY_USER_ID + ", u." + KEY_USERNAME +
                " FROM " + TABLE_GCS + " g" +
                " LEFT JOIN " + TABLE_USERS + " u ON g." + KEY_USER_ID + " = u." + KEY_ID +
                " WHERE g." + KEY_USER_ID + " = ?" +
                " ORDER BY g." + KEY_CREATED_AT + " DESC";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});

        if (cursor != null && cursor.moveToFirst()) {
            do {
                @SuppressLint("Range") int id = cursor.getInt(cursor.getColumnIndex(KEY_ID));
                @SuppressLint("Range") String result = cursor.getString(cursor.getColumnIndex(KEY_RESULT));
                @SuppressLint("Range") String createdAt = cursor.getString(cursor.getColumnIndex(KEY_CREATED_AT));
                @SuppressLint("Range") int user_Id = cursor.getInt(cursor.getColumnIndex(KEY_USER_ID));
                @SuppressLint("Range") String username = cursor.getString(cursor.getColumnIndex(KEY_USERNAME));

                if (username == null || username.isEmpty()) {
                    username = "Вы";
                }

                GcsResult gcsResult = new GcsResult(id, result, createdAt, user_Id, username);
                results.add(gcsResult);
            } while (cursor.moveToNext());

            cursor.close();
        }

        db.close();
        return results;
    }

    public boolean deleteGcsResult(int gcsId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_GCS, KEY_ID + " = ?", new String[]{String.valueOf(gcsId)});
        db.close();
        return result > 0;
    }

    public void clearAllGcsResults() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_GCS, null, null);
        db.close();
    }

    public void clearUserGcsResults(int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_GCS, KEY_USER_ID + " = ?", new String[]{String.valueOf(userId)});
        db.close();
    }

    private String getCurrentDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }

    public static class User {
        private int id;
        private String username;
        private String role;
        private String createdAt;

        public User(int id, String username, String role, String createdAt) {
            this.id = id;
            this.username = username;
            this.role = role;
            this.createdAt = createdAt;
        }

        public int getId() { return id; }
        public String getUsername() { return username; }
        public String getRole() { return role; }
    }

    public static class GcsResult {
        private int id;
        private String result;
        private String createdAt;
        private int userId;
        private String username;

        public GcsResult(int id, String result, String createdAt, int userId, String username) {
            this.id = id;
            this.result = result;
            this.createdAt = createdAt;
            this.userId = userId;
            this.username = username;
        }

        public int getId() { return id; }
        @Override
        public String toString() {
            if (username != null && !username.isEmpty()) {
                return "Пользователь: " + username + "\nДата: " + createdAt + "\nРезультат: " + result;
            } else {
                return "Дата: " + createdAt + "\nРезультат: " + result;
            }
        }
    }
}
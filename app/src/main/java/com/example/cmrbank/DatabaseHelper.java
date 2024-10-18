package com.example.cmrbank;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "bank.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_NAME = "customers";
    private static final String COL_1 = "user_id";
    private static final String COL_2 = "username";
    private static final String COL_3 = "email";
    private static final String COL_4 = "password";
    private static final String COL_5 = "balance";

    public static final String TABLE_TRANSACTIONS = "transactions";
    public static final String COLUMN_TRANSACTION_ID = "transaction_id";
    public static final String COLUMN_USER_ID = "user_id";
    public static final String COLUMN_TRANSACTION_TYPE = "transaction_type";
    public static final String COLUMN_AMOUNT = "amount";
    public static final String COLUMN_TIMESTAMP = "timestamp";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createCustomersTable = "CREATE TABLE " + TABLE_NAME + " (" +
                COL_1 + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_2 + " TEXT UNIQUE, " +
                COL_3 + " TEXT UNIQUE, " +
                COL_4 + " TEXT, " +
                COL_5 + " DOUBLE DEFAULT 0)";

        String createTransactionsTable = "CREATE TABLE " + TABLE_TRANSACTIONS + " ("
                + COLUMN_TRANSACTION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_USER_ID + " INTEGER, "
                + COLUMN_TRANSACTION_TYPE + " TEXT, "
                + COLUMN_AMOUNT + " REAL, "
                + COLUMN_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP, "
                + "FOREIGN KEY(" + COLUMN_USER_ID + ") REFERENCES " + TABLE_NAME + "(" + COL_1 + ")"
                + ");";

        db.execSQL(createCustomersTable);
        db.execSQL(createTransactionsTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRANSACTIONS);
        onCreate(db);
    }

    // Check if username or email already exists
    public boolean checkUserExists(String username, String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE " + COL_2 + "=? OR " + COL_3 + "=?", new String[]{username, email});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    // Insert new user data
    public boolean insertUser(String username, String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_2, username);
        contentValues.put(COL_3, email);
        contentValues.put(COL_4, password);
        long result = db.insert(TABLE_NAME, null, contentValues);
        return result != -1; // Returns true if insert was successful
    }

    // Validate user credentials
    public boolean validateUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE " + COL_2 + "=? AND " + COL_4 + "=?", new String[]{username, password});
        boolean isValid = cursor.getCount() > 0;
        cursor.close();
        return isValid;
    }

    // Check if a specific user exists
    public boolean doesUserExist(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COL_2 + " FROM " + TABLE_NAME + " WHERE " + COL_2 + " = ?", new String[]{username});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    // Insert a transaction record
    public boolean insertTransaction(int userId, String transactionType, double amount) {



        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_ID, userId);
        values.put(COLUMN_TRANSACTION_TYPE, transactionType);
        values.put(COLUMN_AMOUNT, amount);

        long result = db.insert(TABLE_TRANSACTIONS, null, values);
        if (result != -1) {
            updateUserBalance(userId, transactionType, amount);
            return true;
        }
        return false;
    }

    // Update the user's balance based on the transaction
    private void updateUserBalance(int userId, String transactionType, double amount) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COL_5 + " FROM " + TABLE_NAME + " WHERE " + COL_1 + " = ?", new String[]{String.valueOf(userId)});

        if (cursor.moveToFirst()) {
            double currentBalance = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_5));
            cursor.close();

            Log.d("TransactionDebug", "Current Balance: " + currentBalance);
            Log.d("TransactionDebug", "Transaction Type: " + transactionType);

            if (transactionType.equalsIgnoreCase("deposit")) {
                currentBalance += amount;
            } else if (transactionType.equalsIgnoreCase("withdrawal") || transactionType.equalsIgnoreCase("send")) {
                currentBalance -= amount;
            }

            Log.d("TransactionDebug", "Updated Balance: " + currentBalance);

            ContentValues values = new ContentValues();
            values.put(COL_5, currentBalance);
            int rowsAffected = db.update(TABLE_NAME, values, COL_1 + " = ?", new String[]{String.valueOf(userId)});
            Log.d("TransactionDebug", "Balance Update Rows Affected: " + rowsAffected);

            if (rowsAffected <= 0) {
                Log.e("TransactionDebug", "Failed to update balance for userId: " + userId);
            }
        } else {
            Log.e("TransactionDebug", "No user found with userId: " + userId);
            cursor.close();
        }
    }


    // Get the user ID by username
    public int getUserIdByUsername(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COL_1 + " FROM " + TABLE_NAME + " WHERE " + COL_2 + " = ?", new String[]{username});

        if (cursor.moveToFirst()) {
            int userId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_1));
            cursor.close();
            return userId;
        }

        cursor.close();
        return -1; // Return -1 if user does not exist
    }

    public double getUserBalance(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COL_5 + " FROM " + TABLE_NAME + " WHERE " + COL_2 + " = ?", new String[]{username});

        double balance = 0.0;
        if (cursor.moveToFirst()) {
            balance = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_5));
        }
        cursor.close();
        return balance;
    }

}

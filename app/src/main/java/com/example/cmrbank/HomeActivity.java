package com.example.cmrbank;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    private ActivityResultLauncher<Intent> takePictureLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private AlertDialog profileDialog; // Track profile dialog

    Button btnSendMoney, btnCheckBalance, btnDeposit, btnTransactionHistory, btnLogout;
    private static List<Transaction> transactionList = new ArrayList<>();

    SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "MyPrefs";
    private static final String USERNAME_KEY = "username";
    private static final String is_logged_in = "isLoggedIn";

    private static String username;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        dbHelper = new DatabaseHelper(this);

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        username = sharedPreferences.getString(USERNAME_KEY, "User");
        TextView welcomeTextView = findViewById(R.id.tvWelcome);
        welcomeTextView.setText("Welcome, " + username);

        btnSendMoney = findViewById(R.id.sendMoneyButton);
        btnDeposit = findViewById(R.id.depositButton);
        btnCheckBalance = findViewById(R.id.checkBalanceButton);
        btnTransactionHistory = findViewById(R.id.historyButton);
        btnLogout = findViewById(R.id.btnLogout);

        btnSendMoney.setOnClickListener(v -> openSendMoneyDialog());

        btnCheckBalance.setOnClickListener(view -> openCheckBalanceDialog());

        btnDeposit.setOnClickListener(view -> openDepositDialog());

        btnTransactionHistory.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, TransactionHistoryActivity.class);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(view -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(USERNAME_KEY);
            editor.putBoolean(is_logged_in, false);
            editor.apply();

            Toast.makeText(HomeActivity.this, "Logged out Successfully", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        // Initialize ActivityResultLaunchers
        takePictureLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                Intent data = result.getData();
                Bundle extras = data.getExtras();
                Bitmap imageBitmap = (Bitmap) extras.get("data");

                if (profileDialog != null && profileDialog.isShowing()) {
                    ImageView profileImageView = profileDialog.findViewById(R.id.profile_image);
                    if (profileImageView != null) {
                        profileImageView.setImageBitmap(imageBitmap);
                    }
                }
            }
        });

        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, "Camera permission is required to take pictures", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.profile) {
            profileShowDialog();
        } else {
            onItemClick((String) item.getTitle());
        }
        return super.onOptionsItemSelected(item);
    }

    public void onItemClick(String itemTitle) {
        Toast.makeText(HomeActivity.this, itemTitle + " Clicked", Toast.LENGTH_SHORT).show();
    }

    private void profileShowDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_profile, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        profileDialog = builder.create();
        profileDialog.show();

        TextView usernameTextView = dialogView.findViewById(R.id.profile_username);
        TextView emailTextView = dialogView.findViewById(R.id.profile_email);
        ImageView profileImageView = dialogView.findViewById(R.id.profile_image);

        // Retrieve username from SharedPreferences
        String username = sharedPreferences.getString(USERNAME_KEY, "User");
        usernameTextView.setText(username);
        emailTextView.setText(username + "@gmail.com");

        profileImageView.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                requestPermissionLauncher.launch(android.Manifest.permission.CAMERA);
            }
        });
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            takePictureLauncher.launch(takePictureIntent);
        }
    }

    private void openSendMoneyDialog() {
        Dialog dialog = new Dialog(HomeActivity.this);
        dialog.setContentView(R.layout.dialog_send_money);
        dialog.setCancelable(true);

        EditText etReceiverUsername = dialog.findViewById(R.id.et_receiver_username);
        EditText etSendAmount = dialog.findViewById(R.id.et_send_amount);
        EditText etPassword = dialog.findViewById(R.id.et_password);
        Button btnSend = dialog.findViewById(R.id.btn_send);

        btnSend.setOnClickListener(view -> {
            String receiverUsername = etReceiverUsername.getText().toString().trim();
            String sendAmountStr = etSendAmount.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (receiverUsername.isEmpty()) {
                etReceiverUsername.setError("Username is required");
                return;
            }

            double sendAmount;
            try {
                sendAmount = Double.parseDouble(sendAmountStr);
                if (sendAmount <= 0) {
                    etSendAmount.setError("Amount should be greater than 0");
                    return;
                }
            } catch (NumberFormatException e) {
                etSendAmount.setError("Invalid amount");
                return;
            }

            if (password.isEmpty()) {
                etPassword.setError("Password is required");
                return;
            }

            // Validate Receiver Existence
            if (!dbHelper.doesUserExist(receiverUsername)) {
                etReceiverUsername.setError("Receiver does not exist");
                return;
            }

            if (!dbHelper.validateUser(username, password)) {
                etPassword.setError("Incorrect password");
                return;
            }

            int userId = dbHelper.getUserIdByUsername(username);
            double senderBalance = dbHelper.getUserBalance(username);

            if (senderBalance < sendAmount) {
                etSendAmount.setError("Insufficient balance");
                return;
            }

            dbHelper.insertTransaction(userId, "send", sendAmount);

            Toast.makeText(HomeActivity.this, "Sent " + sendAmount + " to " + receiverUsername + " Successfully", Toast.LENGTH_SHORT).show();
            addTransaction("Send Money", sendAmount);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void openDepositDialog() {
        Dialog dialog = new Dialog(HomeActivity.this);
        dialog.setContentView(R.layout.dialog_deposit);
        dialog.setCancelable(true);

        EditText etDepositAmount = dialog.findViewById(R.id.et_deposit_amount);
        EditText etPassword = dialog.findViewById(R.id.et_password_deposit);
        Button btnDepositConfirm = dialog.findViewById(R.id.btn_deposit_confirm);

        btnDepositConfirm.setOnClickListener(view -> {
            String depositAmountStr = etDepositAmount.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (depositAmountStr.isEmpty()) {
                etDepositAmount.setError("Amount is required");
                return;
            }

            double depositAmount;
            try {
                depositAmount = Double.parseDouble(depositAmountStr);
                if (depositAmount <= 0) {
                    etDepositAmount.setError("Amount should be greater than 0");
                    return;
                }
            } catch (NumberFormatException e) {
                etDepositAmount.setError("Invalid amount");
                return;
            }

            if (password.isEmpty()) {
                etPassword.setError("Password is required");
                return;
            }

            if (!dbHelper.validateUser(username, password)) {
                etPassword.setError("Incorrect password");
                return;
            }

            int userId = dbHelper.getUserIdByUsername(username);
            boolean isSuccess = dbHelper.insertTransaction(userId, "deposit", depositAmount);

            if (isSuccess) {
                Toast.makeText(HomeActivity.this, "Deposited ₹" + depositAmount + " Successfully", Toast.LENGTH_SHORT).show();
                addTransaction("Deposit", depositAmount);
                dialog.dismiss();
            } else {
                Toast.makeText(HomeActivity.this, "Failed to deposit. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void openCheckBalanceDialog() {
        Dialog dialog = new Dialog(HomeActivity.this);
        dialog.setContentView(R.layout.dialog_check_balance);
        dialog.setCancelable(true);

        EditText etPassword = dialog.findViewById(R.id.et_password);
        Button btnCheckBalance = dialog.findViewById(R.id.btn_send);

        btnCheckBalance.setOnClickListener(view -> {
            String password = etPassword.getText().toString().trim();

            if (password.isEmpty()) {
                etPassword.setError("Password is required");
                return;
            }

            boolean isPasswordCorrect = dbHelper.validateUser(username, password);

            if (isPasswordCorrect) {
                double balance = dbHelper.getUserBalance(username);
                showBalanceDialog(balance);
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void showBalanceDialog(double balance) {
        AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
        builder.setTitle("Your Balance");
        builder.setMessage("Your current balance is: ₹" + balance);
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    private void addTransaction(String type, double amount) {
        String currentDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        transactionList.add(new Transaction(type, amount, currentDate));
        Toast.makeText(this, type + " successful!", Toast.LENGTH_SHORT).show();
    }

    public static List<Transaction> getTransactionList() {
        return transactionList;
    }
}

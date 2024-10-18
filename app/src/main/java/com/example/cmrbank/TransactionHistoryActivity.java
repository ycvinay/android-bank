package com.example.cmrbank;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cmrbank.TransactionAdapter;

import java.util.List;

public class TransactionHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TransactionAdapter adapter;
    private Button btnBackToHome;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_history);

        btnBackToHome = findViewById(R.id.btnBackToHome);
        recyclerView = findViewById(R.id.rvTransactions);

        List<Transaction> transactionList = HomeActivity.getTransactionList();
        adapter = new TransactionAdapter(transactionList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        btnBackToHome.setOnClickListener(v -> {
            Intent intent = new Intent(TransactionHistoryActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
        });
    }
}
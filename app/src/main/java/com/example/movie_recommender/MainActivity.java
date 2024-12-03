package com.example.movie_recommender;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.CRC32;

public class MainActivity extends AppCompatActivity {

    // Firebase Authentication instance
    private FirebaseAuth auth;

    // UI Elements
    private EditText emailInput, passwordInput;
    private Button loginButton, signUpButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Set window insets for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance();

        // Bind UI elements
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        signUpButton = findViewById(R.id.signUpButton);

        // Set up login button logic
        loginButton.setOnClickListener(v -> loginUser());

        // Set up sign-up button logic
        signUpButton.setOnClickListener(v -> signUpUser());
    }

    private void loginUser() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (email.isEmpty()) {
            emailInput.setError("Email is required");
            emailInput.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            passwordInput.setError("Password is required");
            passwordInput.requestFocus();
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            // Retrieve TMDb account_id
                            DatabaseReference database = FirebaseDatabase.getInstance().getReference("users").child(user.getUid());
                            database.child("tmdbAccountId").get().addOnCompleteListener(dataTask -> {
                                if (dataTask.isSuccessful()) {
                                    Object accountIdObj = dataTask.getResult().getValue();
                                    if (accountIdObj != null) {
                                        String accountId = String.valueOf(accountIdObj); // Convert to String
                                        Toast.makeText(this, "Login successful! Account ID: " + accountId, Toast.LENGTH_SHORT).show();
                                        navigateToHome(accountId);
                                    } else {
                                        Toast.makeText(this, "TMDb Account ID not found.", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Toast.makeText(this, "Failed to retrieve TMDb Account ID", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } else {
                        Toast.makeText(this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void signUpUser() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (email.isEmpty()) {
            emailInput.setError("Email is required");
            emailInput.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            passwordInput.setError("Password is required");
            passwordInput.requestFocus();
            return;
        }
        if (password.length() < 6) {
            passwordInput.setError("Password must be at least 6 characters");
            passwordInput.requestFocus();
            return;
        }

        // Firebase sign-up
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Create TMDb session and store account_id
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            createTmdbSessionAndNavigate(user.getUid());
                        }
                    } else {
                        Toast.makeText(this, "Sign-Up failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createTmdbSessionAndNavigate(String userId) {
        String tmdbApiKey = "84c9ef7e66fdc40d8347137e2afcf2eb";
        String guestSessionUrl = "https://api.themoviedb.org/3/authentication/guest_session/new?api_key=" + tmdbApiKey;

        new Thread(() -> {
            try {
                // Request a guest session from TMDb
                URL url = new URL(guestSessionUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Parse the response to get the guest session ID
                JSONObject jsonResponse = new JSONObject(response.toString());
                String guestSessionId = jsonResponse.getString("guest_session_id");

                // Generate a valid 32-bit integer account_id using CRC32
                CRC32 crc = new CRC32();
                crc.update(guestSessionId.getBytes());
                long hashedValue = crc.getValue();

                // Ensure it's within the 32-bit signed integer range
                int accountId = (int) (hashedValue & 0x7FFFFFFF); // Keep it positive

                // Store the hashed account_id in Firebase
                DatabaseReference database = FirebaseDatabase.getInstance().getReference("users");
                database.child(userId).child("tmdbAccountId").setValue(accountId)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                runOnUiThread(() -> {
                                    Toast.makeText(this, "Account linked with TMDb successfully!", Toast.LENGTH_SHORT).show();
                                    navigateToHome(String.valueOf(accountId));
                                });
                            } else {
                                runOnUiThread(() -> Toast.makeText(this, "Failed to link TMDb account: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show());
                            }
                        });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error creating TMDb session", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void navigateToHome(String accountId) {
        // Navigate to the homepage
        Intent intent = new Intent(this, Homepage.class); // Replace with your Homepage Activity
        intent.putExtra("accountId", accountId);
        startActivity(intent);
        finish();
    }
}

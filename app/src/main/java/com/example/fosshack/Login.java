package com.example.fosshack;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;


public class Login extends AppCompatActivity {

    EditText email, passw;
    Button login;
    ImageView google, hide;
    TextView forgot, create_acc;
    ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    private boolean isPasswordVisible = false;
    private boolean doubleBackToExitPressedOnce = false;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        email = findViewById(R.id.emailEditText);
        passw = findViewById(R.id.passwordEditText);
        login = findViewById(R.id.loginbtn);
        forgot = findViewById(R.id.forgot);
        create_acc = findViewById(R.id.register_navigate);
        hide = findViewById(R.id.hide);
        progressBar = findViewById(R.id.progressbar2);
        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();

        hide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPasswordVisible) {
                    // Hide password
                    passw.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    hide.setImageResource(R.drawable.baseline_visibility_off_24);
                } else {
                    // Show password
                    passw.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    hide.setImageResource(R.drawable.baseline_visibility_24);
                }
                // Move the cursor to the end
                passw.setSelection(passw.length());
                isPasswordVisible = !isPasswordVisible;
            }
        });

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            checkUserRole(currentUser.getUid());
        }

        create_acc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToActivity(Register.class);
            }
        });

        forgot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email1 = email.getText().toString().trim();
                if (!email1.isEmpty()) {
                    progressBar.setVisibility(View.VISIBLE); // Show the progress bar
                    resetPassword(email1);
                } else {
                    Toast.makeText(Login.this, "Please enter your email to reset password", Toast.LENGTH_SHORT).show();
                }
            }
        });

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String emailOrPhone = email.getText().toString().trim();
                String password = passw.getText().toString();

                if (emailOrPhone.isEmpty()) {
                    email.setError("Please fill your email");
                    return;
                }

                if (password.isEmpty()) {
                    passw.setError("Please provide the password");
                    return;
                }
                if (password.length() < 8) {
                    passw.setError("Password must be at least 8 characters long");
                    return;
                }


                if (isValidEmail(emailOrPhone)) {
                    loginuser(emailOrPhone, password);
                } else {
                    email.setError("Enter a valid email");
                    return;
                }
            }
        });
    }

    public boolean isValidEmail(String email) {
        String emailPattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$";
        return email.matches(emailPattern);
    }

    private void loginuser(String email, String password) {
        progressBar.setVisibility(View.VISIBLE);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressBar.setVisibility(View.GONE);

                        if (task.isSuccessful()) {
                            // Sign in success
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                checkUserRole(user.getUid());
                            }
                            else {
                                showToast("Failed to retrieve user information");
                            }
                        }
                        else {
                            // If sign in fails, display a message to the user.
                            String errorMessage = task.getException().getMessage();
                            if (errorMessage.contains("no user record")) {
                                showToast("Email does not exist. Please register.");
                            }
                            else {
                                showToast("Login failed. " + errorMessage);
                            }
                        }
                    }
                });
    }

    private void checkUserRole(String uid) {
        // Check if user is in "Admins" node
        databaseReference.child("Admins").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Toast.makeText(Login.this, "Welcome Admin!", Toast.LENGTH_SHORT).show();
                    navigateToActivity(MainActivity2.class);
                } else {
                    // If not found in Admins, check in Doctors
                    databaseReference.child("Doctors").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                Toast.makeText(Login.this, "Welcome Doctor!", Toast.LENGTH_SHORT).show();
                                navigateToActivity(MainActivity.class);
                            } else {
                                Toast.makeText(Login.this, "No valid role assigned. Contact support!", Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(Login.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Login.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToActivity(Class<?> targetActivity) {
        Intent intent = new Intent(Login.this, targetActivity);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void resetPassword(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        progressBar.setVisibility(View.GONE); // Hide the progress bar
                        if (task.isSuccessful()) {
                            Intent intent = new Intent(Intent.ACTION_MAIN);
                            intent.addCategory(Intent.CATEGORY_APP_EMAIL);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            try {
                                startActivity(intent);
                            } catch (android.content.ActivityNotFoundException ex) {
                                Toast.makeText(Login.this, "There is no email client installed.", Toast.LENGTH_SHORT).show();
                            }

                            Toast.makeText(Login.this, "Password reset email sent!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(Login.this, "Failed to send reset email: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void showToast(String message) {
        Toast.makeText(Login.this, message, Toast.LENGTH_SHORT).show();
    }

    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            // If the user presses back again within a short time, close the app
            super.onBackPressed();
            finishAffinity(); // Close all activities
        } else {
            // Inform the user to press back again to exit
            this.doubleBackToExitPressedOnce = true;
            Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show();

            // Reset the flag after a short time (e.g., 2 seconds)
            new android.os.Handler().postDelayed(
                    new Runnable() {
                        @Override
                        public void run() {
                            doubleBackToExitPressedOnce = false;
                        }
                    }, 2000
            );
        }
    }
}
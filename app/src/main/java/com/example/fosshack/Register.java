package com.example.fosshack;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.google.firebase.auth.SignInMethodQueryResult;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Register extends AppCompatActivity {

    private boolean doubleBackToExitPressedOnce = false;
    ImageView admin, doctor, hide;
    TextView selecttext;
    EditText mailorphone, fullname, pass;
    LinearLayout terms;
    Button signUpButton,login;
    ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    private boolean isPasswordVisible = false;
    private boolean isDoctorSelected = false;
    private Handler handler;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.register), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        admin = findViewById(R.id.admin);
        doctor = findViewById(R.id.docter);
        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();
        selecttext = findViewById(R.id.selecttext);
        mailorphone = findViewById(R.id.emailEditText);
        fullname = findViewById(R.id.fullNameEditText);
        pass = findViewById(R.id.passwordEditText);
        terms = findViewById(R.id.termsAndPrivacyCheckBox);
        signUpButton= findViewById(R.id.signUpButton);
        login = findViewById(R.id.logInButton);
        hide = findViewById(R.id.hide);
        progressBar = findViewById(R.id.progressbar1);

        admin.setImageResource(R.drawable.register_admin1);
        doctor.setImageResource(R.drawable.register_doctor1);

        admin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isDoctorSelected = false;
                admin.setImageResource(R.drawable.register_admin);
                doctor.setImageResource(R.drawable.register_doctor1);

                selecttext.setVisibility(View.GONE);
                fullname.setVisibility(View.VISIBLE);
                mailorphone.setVisibility(View.VISIBLE);
                pass.setVisibility(View.VISIBLE);
                hide.setVisibility(View.VISIBLE);
                terms.setVisibility(View.VISIBLE);
                signUpButton.setVisibility(View.VISIBLE);
            }
        });

        doctor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isDoctorSelected = true;
                admin.setImageResource(R.drawable.register_admin1);
                doctor.setImageResource(R.drawable.register_doctor);

                selecttext.setVisibility(View.GONE);
                fullname.setVisibility(View.VISIBLE);
                mailorphone.setVisibility(View.VISIBLE);
                pass.setVisibility(View.VISIBLE);
                hide.setVisibility(View.VISIBLE);
                terms.setVisibility(View.VISIBLE);
                signUpButton.setVisibility(View.VISIBLE);
            }
        });

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToActivity(Login.class);
            }
        });

        hide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPasswordVisible) {
                    // Hide password
                    pass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    hide.setImageResource(R.drawable.baseline_visibility_off_24);
                } else {
                    // Show password
                    pass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    hide.setImageResource(R.drawable.baseline_visibility_24);
                }
                // Move the cursor to the end
                pass.setSelection(pass.length());
                isPasswordVisible = !isPasswordVisible;
            }
        });

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            checkUserRole(currentUser.getUid());
        }

        CheckBox agreeCheckBox = findViewById(R.id.agreeCheckBox);

        agreeCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                signUpButton.setEnabled(isChecked);
                if (isChecked) {
                    signUpButton.setBackgroundResource(R.drawable.btn_gradient_style1);
                } else {
                    signUpButton.setBackgroundResource(R.drawable.btn_gradient_style1_disabled);
                }
            }
        });

        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String name = fullname.getText().toString();
                String emailOrPhone = mailorphone.getText().toString().trim();
                String password = pass.getText().toString();


                if (name.isEmpty()) {
                    fullname.setError("Please fill your name");
                    return;
                }

                if (emailOrPhone.isEmpty()) {
                    mailorphone.setError("Please fill your email");
                    return;
                }

                if (password.isEmpty()) {
                    pass.setError("Please provide the password");
                    return;
                }
                if (password.length() < 8) {
                    pass.setError("Password must be at least 8 characters long");
                    return;
                }


                if (isValidEmail(emailOrPhone)) {
                    registerUser(emailOrPhone, password, name);
                } else {
                    mailorphone.setError("Enter a valid email");
                    return;
                }
            }
        });
    }

    public boolean isValidEmail(String email) {
        String emailPattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$";
        return email.matches(emailPattern);
    }

    private void registerUser(String email, String password, String name) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressBar.setVisibility(View.GONE); // Hide the progress bar

                        if (task.isSuccessful()) {
                            // Sign in success, send verification email
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                user.sendEmailVerification()
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    Toast.makeText(Register.this, "Registration successful! Please check your email for verification.", Toast.LENGTH_SHORT).show();

                                                    // Save user details to Firebase Realtime Database
                                                    String userId = user.getUid();
                                                    String node = "Admins";
                                                    if (isDoctorSelected) {
                                                        node = "Doctors";
                                                    }
                                                    databaseReference.child(node).child(userId).child("name").setValue(name);
                                                    databaseReference.child(node).child(userId).child("email").setValue(email);
                                                    databaseReference.child(node).child(userId).child("password").setValue(password);

                                                    // Open Gmail app
                                                    Intent intent = new Intent(Intent.ACTION_MAIN);
                                                    intent.addCategory(Intent.CATEGORY_APP_EMAIL);
                                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                    try {
                                                        startActivity(intent);
                                                    } catch (android.content.ActivityNotFoundException ex) {
                                                        Toast.makeText(Register.this, "There is no email client installed.", Toast.LENGTH_SHORT).show();
                                                    }

                                                    // Start a runnable to check email verification status
                                                    handler.postDelayed(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            checkEmailVerification(user);
                                                        }
                                                    }, 1000);
                                                } else {
                                                    Toast.makeText(Register.this, "Failed to send verification email.", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                            }
                        } else {
                            // If sign in fails, display a message to the user.
                            Toast.makeText(Register.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void checkEmailVerification(FirebaseUser user) {
        progressBar.setVisibility(View.VISIBLE); // Show the progress bar while checking verification
        user.reload().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                progressBar.setVisibility(View.GONE); // Hide the progress bar

                if (user.isEmailVerified()) {
                    // Email is verified, navigate to Register activity
                    Intent intent = new Intent(Register.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    // Email not verified, check again after some time
                    Toast.makeText(Register.this, "Please verify your email.", Toast.LENGTH_SHORT).show();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            checkEmailVerification(user);
                        }
                    }, 1000);
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
                    Toast.makeText(Register.this, "Welcome Admin!", Toast.LENGTH_SHORT).show();
                    navigateToActivity(MainActivity2.class);
                } else {
                    // If not found in Admins, check in Doctors
                    databaseReference.child("Doctors").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                Toast.makeText(Register.this, "Welcome Doctor!", Toast.LENGTH_SHORT).show();
                                navigateToActivity(MainActivity.class);
                            } else {
                                Toast.makeText(Register.this, "No valid role assigned. Contact support!", Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(Register.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Register.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToActivity(Class<?> targetActivity) {
        Intent intent = new Intent(Register.this, targetActivity);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
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
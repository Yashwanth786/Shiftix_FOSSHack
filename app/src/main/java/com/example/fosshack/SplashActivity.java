package com.example.fosshack;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SplashActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    MotionLayout motionLayout;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash_screen);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.motionLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();
        motionLayout = findViewById(R.id.motionLayout);

        // Delay the animation before transitioning
        new Handler().postDelayed(() -> {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                checkUserRole(currentUser.getUid());
            }
            else {
                motionLayout.transitionToEnd();

                // Move to MainActivity after animation completes
                new Handler().postDelayed(() -> {
                    startActivity(new Intent(SplashActivity.this, Onboarding.class));
                    finish();
                }, 1000);
            }
        }, 2000);
    }

    private void checkUserRole(String uid) {
        // Check if user is in "Admins" node
        databaseReference.child("Admins").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Toast.makeText(SplashActivity.this, "Welcome Admin!", Toast.LENGTH_SHORT).show();
                    navigateToActivity(MainActivity2.class);
                } else {
                    // If not found in Admins, check in Doctors
                    databaseReference.child("Doctors").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                Toast.makeText(SplashActivity.this, "Welcome Doctor!", Toast.LENGTH_SHORT).show();
                                navigateToActivity(MainActivity.class);
                            } else {
                                Toast.makeText(SplashActivity.this, "No valid role assigned. Contact support!", Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(SplashActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SplashActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToActivity(Class<?> targetActivity) {
        Intent intent = new Intent(SplashActivity.this, targetActivity);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

}

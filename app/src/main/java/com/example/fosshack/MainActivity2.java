package com.example.fosshack;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.Calendar;

public class MainActivity2 extends AppCompatActivity {

    private FloatingActionButton fabAdd;
    private LinearLayout workContainer;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    private ArrayList<String> namesList;
    private ArrayAdapter<String> namesAdapter;
    private ImageView logout_button, deletecard;
    private TextView username;

    private String selectedName = "";
    private String selectedDepartment = "";
    private String startDateTime = "";
    private String endDateTime = "";

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main2);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main2), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        fabAdd = findViewById(R.id.fab_add);
        workContainer = findViewById(R.id.workContainer);
        namesList = new ArrayList<>();
        namesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, namesList);
        logout_button = findViewById(R.id.button_logout);
        username = findViewById(R.id.username);
        mAuth = FirebaseAuth.getInstance();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();

            // Reference to the admin's name in database
            DatabaseReference adminRef = FirebaseDatabase.getInstance().getReference("Admins")
                    .child(userId)
                    .child("name");

            // Fetch and set the username
            adminRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String name = snapshot.getValue(String.class);
                        username.setText(name);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("Database Error", "Error fetching username: " + error.getMessage());
                }
            });
        }

        fetchNamesFromFirebase();
        reloadWorkFromDatabase();

        logout_button.setOnClickListener(v -> {
            Toast.makeText(MainActivity2.this, "Logged Out", Toast.LENGTH_SHORT).show();
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(MainActivity2.this, Register.class);
            startActivity(intent);
            finish();
        });

        fabAdd.setOnClickListener(v -> showAddWorkDialog());
    }

    private void fetchNamesFromFirebase() {
        databaseReference = FirebaseDatabase.getInstance().getReference("Doctors");

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                namesList.clear(); // Clear existing data

                for (DataSnapshot doctorSnapshot : snapshot.getChildren()) {
                    if (doctorSnapshot.child("name").exists()) {
                        String doctorName = doctorSnapshot.child("name").getValue(String.class);
                        namesList.add(doctorName);
                    }
                }

                if (namesList.isEmpty()) {
                    Log.e("FirebaseError", "No names found under Doctors node!");
                }

                namesAdapter.notifyDataSetChanged(); // Notify adapter after updating list
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Database error: " + error.getMessage());
            }
        });
    }

    private void showAddWorkDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_work, null);
        builder.setView(dialogView);

        Spinner spinnerNames = dialogView.findViewById(R.id.spinner_names);
        Spinner spinnerDepartments = dialogView.findViewById(R.id.spinner_departments);
        Button btnStartDate = dialogView.findViewById(R.id.btn_start_date);
        Button btnEndDate = dialogView.findViewById(R.id.btn_end_date);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnAddWork = dialogView.findViewById(R.id.btn_add_work);

        spinnerNames.setAdapter(namesAdapter);

        btnStartDate.setOnClickListener(v -> pickDateTime(true, btnStartDate));
        btnEndDate.setOnClickListener(v -> pickDateTime(false, btnEndDate));

        AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnAddWork.setOnClickListener(v -> {
            selectedName = spinnerNames.getSelectedItem().toString();
            selectedDepartment = spinnerDepartments.getSelectedItem().toString();

            if (selectedName.isEmpty() || selectedDepartment.isEmpty() || startDateTime.isEmpty() || endDateTime.isEmpty()) {
                Toast.makeText(this, "All fields must be selected", Toast.LENGTH_SHORT).show();
                return;
            }

            saveWorkToFirebase(selectedName, selectedDepartment, startDateTime, endDateTime);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void pickDateTime(boolean isStart, Button button) {
        Calendar calendar = Calendar.getInstance();
        Calendar selectedDateTime = Calendar.getInstance();

        DatePickerDialog datePicker = new DatePickerDialog(this, (view, year, month, day) -> {
            selectedDateTime.set(year, month, day);

            TimePickerDialog timePicker = new TimePickerDialog(this, (timeView, hour, minute) -> {
                selectedDateTime.set(Calendar.HOUR_OF_DAY, hour);
                selectedDateTime.set(Calendar.MINUTE, minute);

                // Format the date and time properly
                String dateTime = String.format("%02d/%02d/%04d %02d:%02d", day, (month + 1), year, hour, minute);

                if (isStart) {
                    if (selectedDateTime.before(calendar)) {
                        Toast.makeText(this, "Start date/time cannot be in the past!", Toast.LENGTH_SHORT).show();
                    } else {
                        startDateTime = dateTime;
                        button.setText(startDateTime);
                    }
                } else {
                    Calendar startCal = Calendar.getInstance();
                    if (!startDateTime.isEmpty()) {
                        String[] parts = startDateTime.split(" ");
                        String[] dateParts = parts[0].split("/");
                        String[] timeParts = parts[1].split(":");

                        startCal.set(
                                Integer.parseInt(dateParts[2]), // Year
                                Integer.parseInt(dateParts[1]) - 1, // Month (0-based)
                                Integer.parseInt(dateParts[0]), // Day
                                Integer.parseInt(timeParts[0]), // Hour
                                Integer.parseInt(timeParts[1])  // Minute
                        );
                    }

                    if (selectedDateTime.before(startCal)) {
                        Toast.makeText(this, "End date/time must be after start date/time!", Toast.LENGTH_SHORT).show();
                    } else {
                        endDateTime = dateTime;
                        button.setText(endDateTime);
                    }
                }
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);

            timePicker.show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        // Prevent past date selection
        if (isStart) {
            datePicker.getDatePicker().setMinDate(System.currentTimeMillis());
        } else {
            if (!startDateTime.isEmpty()) {
                Calendar startCal = Calendar.getInstance();
                String[] parts = startDateTime.split(" ");
                String[] dateParts = parts[0].split("/");

                startCal.set(
                        Integer.parseInt(dateParts[2]), // Year
                        Integer.parseInt(dateParts[1]) - 1, // Month (0-based)
                        Integer.parseInt(dateParts[0]) // Day
                );

                datePicker.getDatePicker().setMinDate(startCal.getTimeInMillis());
            } else {
                datePicker.getDatePicker().setMinDate(System.currentTimeMillis());
            }
        }

        datePicker.show();
    }


    private void saveWorkToFirebase(String name, String department, String startDateTime, String endDateTime) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Admins").child(userId).child("Work");

        // Generate a unique ID using Firebase's push() method
        String workId = userRef.push().getKey();

        WorkEntry workEntry = new WorkEntry(name, department, startDateTime, endDateTime);
        userRef.child(workId).setValue(workEntry)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(MainActivity2.this, "Work added successfully", Toast.LENGTH_SHORT).show();
                    addWorkLayout(workId, selectedName, selectedDepartment, startDateTime, endDateTime);

                    // Fetch the doctor's UID based on the selected name
                    DatabaseReference doctorsRef = FirebaseDatabase.getInstance().getReference("Doctors");
                    doctorsRef.orderByChild("name").equalTo(name).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                for (DataSnapshot doctorSnapshot : snapshot.getChildren()) {
                                    String doctorUid = doctorSnapshot.getKey();
                                    if (doctorUid != null) {
                                        // Save the work entry under the doctor's "Shifts" node
                                        DatabaseReference shiftsRef = FirebaseDatabase.getInstance().getReference("Doctors")
                                                .child(doctorUid)
                                                .child("Shifts");

                                        shiftsRef.child(workId).setValue(workEntry)
                                                .addOnSuccessListener(aVoid1 -> {
                                                    Log.d("Firebase", "Shift added successfully under doctor's UID");
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.e("Firebase", "Failed to add shift under doctor's UID", e);
                                                });
                                    }
                                }
                            } else {
                                Log.e("Firebase", "No doctor found with the name: " + name);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("Firebase", "Database error: " + error.getMessage());
                        }
                    });
                })
                .addOnFailureListener(e -> Toast.makeText(MainActivity2.this, "Failed to add work", Toast.LENGTH_SHORT).show());
    }

    @SuppressLint("MissingInflatedId")
    private void addWorkLayout(String id, String name, String department, String startDateTime, String endDateTime) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View workView = inflater.inflate(R.layout.item_work, workContainer, false);

        TextView tvName = workView.findViewById(R.id.tv_name);
        TextView tvDepartment = workView.findViewById(R.id.tv_department);
        TextView tvStartDate = workView.findViewById(R.id.tv_start_date);
        TextView tvEndDate = workView.findViewById(R.id.tv_end_date);
        deletecard = workView.findViewById(R.id.delete);

        tvName.setText("Doctor Name: " + name);
        tvDepartment.setText("Department: " + department);
        tvStartDate.setText("Start: " + startDateTime);
        tvEndDate.setText("End: " + endDateTime);

        workView.setTag(id);

        deletecard.setOnClickListener(v -> deleteAppointment(name, workView));

        workContainer.addView(workView);
    }

    private void deleteAppointment(String name, View workView) {
        String id = (String) workView.getTag();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Admins")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child("Work");

        ref.child(id).removeValue().addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
            workContainer.removeView(workView);
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to delete", Toast.LENGTH_SHORT).show();
        });

        DatabaseReference doctorsRef = FirebaseDatabase.getInstance().getReference("Doctors");
        doctorsRef.orderByChild("name").equalTo(name).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot doctorSnapshot : snapshot.getChildren()) {
                         String doctorUid = doctorSnapshot.getKey();
                        if (doctorUid != null) {
                            // Save the work entry under the doctor's "Shifts" node
                            DatabaseReference shiftsRef = FirebaseDatabase.getInstance().getReference("Doctors")
                                    .child(doctorUid)
                                    .child("Shifts");

                            shiftsRef.child(id).removeValue();
                        }
                    }
                } else {
                    Log.e("Firebase", "No doctor found with the name: " + name);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Database error: " + error.getMessage());
            }
        });
    }

    private void reloadWorkFromDatabase() {
        workContainer.removeAllViews(); // Clear existing views

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Admins")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child("Work");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                    // Data found, add all cards
                    for (DataSnapshot data : snapshot.getChildren()) {
                        WorkEntry entry = data.getValue(WorkEntry.class);
                        if (entry != null) {
                            addWorkLayout(data.getKey(), entry.name, entry.department, entry.startDateTime, entry.endDateTime);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity2.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    private static class WorkEntry {
        public String name, department, startDateTime, endDateTime;

        public WorkEntry() {
        }

        public WorkEntry(String name, String department, String startDateTime, String endDateTime) {
            this.name = name;
            this.department = department;
            this.startDateTime = startDateTime;
            this.endDateTime = endDateTime;
        }
    }
}

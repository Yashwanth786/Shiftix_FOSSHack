package com.example.fosshack;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
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
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    private TextView username;
    private ImageView btnLogout;
    private MaterialCalendarView calendarView;
    private DatabaseReference databaseReference;
    private Map<CalendarDay, List<ShiftDetails>> shiftMap = new HashMap<>();

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        username = findViewById(R.id.username);
        btnLogout = findViewById(R.id.button_logout);
        calendarView = findViewById(R.id.calendarView);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();

            // Fetch username from Firebase
            DatabaseReference adminRef = FirebaseDatabase.getInstance().getReference("Doctors")
                    .child(userId)
                    .child("name");

            adminRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String name = snapshot.getValue(String.class);
                        username.setText("Dr. " + name);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("Database Error", "Error fetching username: " + error.getMessage());
                }
            });

            // Fetch and highlight shifts
            fetchAndHighlightShifts(userId);
        }

        btnLogout.setOnClickListener(v -> {
            Toast.makeText(MainActivity.this, "Logged Out", Toast.LENGTH_SHORT).show();
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(MainActivity.this, Register.class));
            finish();
        });

        // Handle date clicks
        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            if (shiftMap.containsKey(date)) {
                List<ShiftDetails> shifts = shiftMap.get(date);
                showShiftPopup(shifts);
            }
        });
    }

    private void fetchAndHighlightShifts(String userId) {
        databaseReference = FirebaseDatabase.getInstance().getReference("Doctors")
                .child(userId)
                .child("Shifts");

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                HashMap<String, Integer> departmentColors = getDepartmentColors();
                HashSet<CalendarDay> highlightedDates = new HashSet<>();

                for (DataSnapshot shiftSnapshot : snapshot.getChildren()) {
                    String department = shiftSnapshot.child("department").getValue(String.class);
                    String startDateTime = shiftSnapshot.child("startDateTime").getValue(String.class);
                    String endDateTime = shiftSnapshot.child("endDateTime").getValue(String.class);

                    if (department == null || startDateTime == null || endDateTime == null) continue;

                    List<CalendarDay> daysInRange = getDatesBetween(startDateTime, endDateTime);
                    highlightedDates.addAll(daysInRange);

                    Integer color = departmentColors.getOrDefault(department, Color.GRAY);

                    for (CalendarDay day : daysInRange) {
                        shiftMap.putIfAbsent(day, new ArrayList<>());
                        shiftMap.get(day).add(new ShiftDetails(department, startDateTime, endDateTime));
                    }

                    calendarView.addDecorator(new DateDecorator(new HashSet<>(daysInRange), color, Color.WHITE));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Database Error", "Error fetching shifts: " + error.getMessage());
            }
        });
    }

    private HashMap<String, Integer> getDepartmentColors() {
        HashMap<String, Integer> departmentColors = new HashMap<>();
        departmentColors.put("Cardiology", Color.RED);
        departmentColors.put("Neurology", Color.BLUE);
        departmentColors.put("Orthopedics", Color.GREEN);
        departmentColors.put("Pediatrics", Color.YELLOW);
        departmentColors.put("Oncology", Color.MAGENTA);
        return departmentColors;
    }

    private List<CalendarDay> getDatesBetween(String startDateStr, String endDateStr) {
        List<CalendarDay> dates = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        try {
            long startMillis = sdf.parse(startDateStr).getTime();
            long endMillis = sdf.parse(endDateStr).getTime();
            long oneDay = 24 * 60 * 60 * 1000; // 1 day in milliseconds

            for (long time = startMillis; time <= endMillis; time += oneDay) {
                java.util.Calendar calendar = java.util.Calendar.getInstance();
                calendar.setTimeInMillis(time);
                int year = calendar.get(java.util.Calendar.YEAR);
                int month = calendar.get(java.util.Calendar.MONTH);
                int day = calendar.get(java.util.Calendar.DAY_OF_MONTH);
                dates.add(CalendarDay.from(year, month, day));
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return dates;
    }

    private void showShiftPopup(List<ShiftDetails> shifts) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Shift Details");

        StringBuilder shiftInfo = new StringBuilder();
        for (ShiftDetails shift : shifts) {
            shiftInfo.append("Department: ").append(shift.getDepartment()).append("\n")
                    .append("Start: ").append(shift.getStartDateTime()).append("\n")
                    .append("End: ").append(shift.getEndDateTime()).append("\n\n");
        }

        builder.setMessage(shiftInfo.toString());
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
}

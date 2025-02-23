package com.example.fosshack;

public class ShiftDetails {
    private String department;
    private String startDateTime;
    private String endDateTime;

    public ShiftDetails(String department, String startDateTime, String endDateTime) {
        this.department = department;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
    }

    public String getDepartment() {
        return department;
    }

    public String getStartDateTime() {
        return startDateTime;
    }

    public String getEndDateTime() {
        return endDateTime;
    }
}

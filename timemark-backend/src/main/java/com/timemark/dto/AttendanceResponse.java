package com.timemark.dto;

import com.timemark.entity.Attendance;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
public class AttendanceResponse {
    private final Long id;
    private final Long employeeId;
    private final String employeeName;
    private final LocalDate date;
    private final LocalTime checkInTime;
    private final LocalTime checkOutTime;
    private final String status;

    public AttendanceResponse(Attendance a) {
        this.id = a.getId();
        this.employeeId = a.getEmployee().getId();
        this.employeeName = a.getEmployee().getFullName();
        this.date = a.getDate();
        this.checkInTime = a.getCheckInTime();
        this.checkOutTime = a.getCheckOutTime();
        this.status = a.getStatus().name();
    }
}

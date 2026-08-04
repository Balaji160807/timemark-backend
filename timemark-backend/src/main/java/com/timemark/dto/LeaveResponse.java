package com.timemark.dto;

import com.timemark.entity.LeaveRequest;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class LeaveResponse {
    private final Long id;
    private final Long employeeId;
    private final String employeeName;
    private final String type;
    private final LocalDate fromDate;
    private final LocalDate toDate;
    private final Integer days;
    private final String reason;
    private final String status;

    public LeaveResponse(LeaveRequest lr) {
        this.id = lr.getId();
        this.employeeId = lr.getEmployee().getId();
        this.employeeName = lr.getEmployee().getFullName();
        this.type = lr.getType().name();
        this.fromDate = lr.getFromDate();
        this.toDate = lr.getToDate();
        this.days = lr.getDays();
        this.reason = lr.getReason();
        this.status = lr.getStatus().name();
    }
}

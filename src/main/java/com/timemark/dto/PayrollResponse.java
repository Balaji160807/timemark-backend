package com.timemark.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PayrollResponse {
    private Long employeeId;
    private String employeeName;
    private String month;          // e.g. "2026-07"
    private int workingDays;       // weekdays elapsed so far this month
    private int presentDays;       // present + late
    private int lateDays;
    private int leaveDays;         // approved leave overlapping this month
    private int absentDays;
    private double perDayRate;
    private double deduction;
    private double netPay;
}

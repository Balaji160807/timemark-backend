package com.timemark.service;

import com.timemark.dto.PayrollResponse;
import com.timemark.entity.*;
import com.timemark.repository.AttendanceRepository;
import com.timemark.repository.EmployeeRepository;
import com.timemark.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PayrollService {

    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;

    @Value("${app.payroll.working-days-per-month}")
    private int workingDaysPerMonth;

    public PayrollResponse forEmployee(Employee employee, YearMonth month) {
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();
        LocalDate today = LocalDate.now();
        LocalDate periodEnd = today.isBefore(monthEnd) ? today : monthEnd;

        int workingDaysElapsed = countWeekdays(monthStart, periodEnd);

        List<Attendance> records = attendanceRepository.findByEmployeeAndDateBetween(employee, monthStart, periodEnd);
        int presentDays = (int) records.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.PRESENT || a.getStatus() == AttendanceStatus.LATE)
                .count();
        int lateDays = (int) records.stream().filter(a -> a.getStatus() == AttendanceStatus.LATE).count();

        int leaveDays = leaveRequestRepository.findByEmployeeAndStatus(employee, LeaveStatus.APPROVED).stream()
                .mapToInt(lr -> overlapDays(lr.getFromDate(), lr.getToDate(), monthStart, periodEnd))
                .sum();

        int absentDays = Math.max(0, workingDaysElapsed - presentDays - leaveDays);

        double perDayRate = employee.getSalary() == null ? 0 : employee.getSalary() / workingDaysPerMonth;
        double deduction = absentDays * perDayRate;
        double netPay = (employee.getSalary() == null ? 0 : employee.getSalary()) - deduction;

        return new PayrollResponse(
                employee.getId(), employee.getFullName(), month.toString(),
                workingDaysElapsed, presentDays, lateDays, leaveDays, absentDays,
                round2(perDayRate), round2(deduction), round2(netPay)
        );
    }

    public List<PayrollResponse> forAllEmployees(YearMonth month) {
        return employeeRepository.findAll().stream().map(e -> forEmployee(e, month)).toList();
    }

    /** Counts Mon-Fri days between start and end, inclusive. Returns 0 if end is before start. */
    private int countWeekdays(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) return 0;
        int count = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            if (d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY) {
                count++;
            }
        }
        return count;
    }

    /** Days of [aFrom, aTo] that fall within [bFrom, bTo], inclusive on both ends. */
    private int overlapDays(LocalDate aFrom, LocalDate aTo, LocalDate bFrom, LocalDate bTo) {
        LocalDate start = aFrom.isAfter(bFrom) ? aFrom : bFrom;
        LocalDate end = aTo.isBefore(bTo) ? aTo : bTo;
        if (end.isBefore(start)) return 0;
        return (int) (end.toEpochDay() - start.toEpochDay() + 1);
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}

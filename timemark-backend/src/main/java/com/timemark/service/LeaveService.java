package com.timemark.service;

import com.timemark.dto.LeaveRequestCreateDto;
import com.timemark.dto.LeaveResponse;
import com.timemark.entity.Employee;
import com.timemark.entity.LeaveRequest;
import com.timemark.entity.LeaveStatus;
import com.timemark.entity.LeaveType;
import com.timemark.repository.EmployeeRepository;
import com.timemark.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final EmailService emailService;

    public LeaveResponse requestLeave(Employee employee, LeaveRequestCreateDto dto) {
        if (dto.getToDate().isBefore(dto.getFromDate())) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }

        int days = (int) ChronoUnit.DAYS.between(dto.getFromDate(), dto.getToDate()) + 1;
        int available = balanceFor(employee, dto.getType());

        if (days > available) {
            throw new IllegalArgumentException(
                    "Only " + available + " " + dto.getType().name().toLowerCase() + " day(s) remaining");
        }

        LeaveRequest lr = new LeaveRequest();
        lr.setEmployee(employee);
        lr.setType(dto.getType());
        lr.setFromDate(dto.getFromDate());
        lr.setToDate(dto.getToDate());
        lr.setDays(days);
        lr.setReason(dto.getReason());
        lr.setStatus(LeaveStatus.PENDING);

        return new LeaveResponse(leaveRequestRepository.save(lr));
    }

    public List<LeaveResponse> myLeaves(Employee employee) {
        return leaveRequestRepository.findByEmployeeOrderByAppliedOnDesc(employee)
                .stream().map(LeaveResponse::new).toList();
    }

    public List<LeaveResponse> pending() {
        return leaveRequestRepository.findByStatus(LeaveStatus.PENDING)
                .stream().map(LeaveResponse::new).toList();
    }

    public LeaveResponse decide(Long requestId, boolean approve) {
        LeaveRequest lr = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Leave request not found"));

        if (lr.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalStateException("This request has already been decided");
        }

        Employee employee = lr.getEmployee();

        if (approve) {
            deductBalance(employee, lr.getType(), lr.getDays());
            employeeRepository.save(employee);
            lr.setStatus(LeaveStatus.APPROVED);
        } else {
            lr.setStatus(LeaveStatus.REJECTED);
        }

        LeaveResponse response = new LeaveResponse(leaveRequestRepository.save(lr));

        if (employee.getEmail() != null && !employee.getEmail().isBlank()) {
            String subject = "Your leave request was " + lr.getStatus().name().toLowerCase();
            String body = String.format(
                    "Hi %s,%n%nYour %s leave request for %s → %s (%d day%s) has been %s.%n%nReason given: %s",
                    employee.getFullName(), lr.getType(), lr.getFromDate(), lr.getToDate(),
                    lr.getDays(), lr.getDays() > 1 ? "s" : "", lr.getStatus().name().toLowerCase(), lr.getReason());
            emailService.send(employee.getEmail(), subject, body);
        }

        return response;
    }

    private int balanceFor(Employee e, LeaveType type) {
        return switch (type) {
            case CASUAL -> e.getCasualLeaveBalance();
            case SICK -> e.getSickLeaveBalance();
            case ANNUAL -> e.getAnnualLeaveBalance();
        };
    }

    private void deductBalance(Employee e, LeaveType type, int days) {
        switch (type) {
            case CASUAL -> e.setCasualLeaveBalance(Math.max(0, e.getCasualLeaveBalance() - days));
            case SICK -> e.setSickLeaveBalance(Math.max(0, e.getSickLeaveBalance() - days));
            case ANNUAL -> e.setAnnualLeaveBalance(Math.max(0, e.getAnnualLeaveBalance() - days));
        }
    }
}

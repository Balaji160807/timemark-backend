package com.timemark.service;

import com.timemark.dto.LeaveRequestCreateDto;
import com.timemark.dto.LeaveResponse;
import com.timemark.entity.Employee;
import com.timemark.entity.LeaveRequest;
import com.timemark.entity.LeaveStatus;
import com.timemark.entity.LeaveType;
import com.timemark.repository.EmployeeRepository;
import com.timemark.repository.LeaveRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveServiceTest {

    @Mock private LeaveRequestRepository leaveRequestRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private EmailService emailService;

    private LeaveService leaveService;
    private Employee employee;

    @BeforeEach
    void setUp() {
        leaveService = new LeaveService(leaveRequestRepository, employeeRepository, emailService);
        employee = new Employee();
        employee.setId(1L);
        employee.setFullName("Priya Fernando");
        employee.setCasualLeaveBalance(2);
        employee.setSickLeaveBalance(7);
        employee.setAnnualLeaveBalance(14);
    }

    @Test
    void rejectsRequestExceedingAvailableBalance() {
        LeaveRequestCreateDto dto = new LeaveRequestCreateDto();
        dto.setType(LeaveType.CASUAL);
        dto.setFromDate(LocalDate.of(2026, 8, 1));
        dto.setToDate(LocalDate.of(2026, 8, 4)); // 4 days, only 2 available
        dto.setReason("Trip");

        assertThatThrownBy(() -> leaveService.requestLeave(employee, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2 casual day");
    }

    @Test
    void rejectsEndDateBeforeStartDate() {
        LeaveRequestCreateDto dto = new LeaveRequestCreateDto();
        dto.setType(LeaveType.CASUAL);
        dto.setFromDate(LocalDate.of(2026, 8, 5));
        dto.setToDate(LocalDate.of(2026, 8, 1));
        dto.setReason("Oops");

        assertThatThrownBy(() -> leaveService.requestLeave(employee, dto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acceptsValidRequestWithinBalance() {
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> {
            LeaveRequest lr = inv.getArgument(0);
            lr.setId(1L);
            lr.setEmployee(employee);
            return lr;
        });

        LeaveRequestCreateDto dto = new LeaveRequestCreateDto();
        dto.setType(LeaveType.CASUAL);
        dto.setFromDate(LocalDate.of(2026, 8, 1));
        dto.setToDate(LocalDate.of(2026, 8, 2)); // 2 days, exactly at balance
        dto.setReason("Family event");

        LeaveResponse resp = leaveService.requestLeave(employee, dto);

        assertThat(resp.getDays()).isEqualTo(2);
        assertThat(resp.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void approvingDeductsBalanceAndSendsEmail() {
        employee.setEmail("priya@example.com");

        LeaveRequest lr = new LeaveRequest();
        lr.setId(5L);
        lr.setEmployee(employee);
        lr.setType(LeaveType.CASUAL);
        lr.setDays(2);
        lr.setFromDate(LocalDate.of(2026, 8, 1));
        lr.setToDate(LocalDate.of(2026, 8, 2));
        lr.setReason("Family event");
        lr.setStatus(LeaveStatus.PENDING);

        when(leaveRequestRepository.findById(5L)).thenReturn(Optional.of(lr));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        LeaveResponse resp = leaveService.decide(5L, true);

        assertThat(resp.getStatus()).isEqualTo("APPROVED");
        assertThat(employee.getCasualLeaveBalance()).isEqualTo(0); // was 2, minus 2 days
        verify(employeeRepository).save(employee);
        verify(emailService).send(eq("priya@example.com"), anyString(), anyString());
    }

    @Test
    void rejectingDoesNotDeductBalance() {
        LeaveRequest lr = new LeaveRequest();
        lr.setId(6L);
        lr.setEmployee(employee);
        lr.setType(LeaveType.CASUAL);
        lr.setDays(2);
        lr.setFromDate(LocalDate.of(2026, 8, 1));
        lr.setToDate(LocalDate.of(2026, 8, 2));
        lr.setReason("Family event");
        lr.setStatus(LeaveStatus.PENDING);

        when(leaveRequestRepository.findById(6L)).thenReturn(Optional.of(lr));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        LeaveResponse resp = leaveService.decide(6L, false);

        assertThat(resp.getStatus()).isEqualTo("REJECTED");
        assertThat(employee.getCasualLeaveBalance()).isEqualTo(2); // unchanged
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void cannotDecideOnAlreadyDecidedRequest() {
        LeaveRequest lr = new LeaveRequest();
        lr.setId(7L);
        lr.setEmployee(employee);
        lr.setStatus(LeaveStatus.APPROVED);

        when(leaveRequestRepository.findById(7L)).thenReturn(Optional.of(lr));

        assertThatThrownBy(() -> leaveService.decide(7L, true))
                .isInstanceOf(IllegalStateException.class);
    }
}

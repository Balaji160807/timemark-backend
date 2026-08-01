package com.timemark.service;

import com.timemark.dto.PayrollResponse;
import com.timemark.entity.Attendance;
import com.timemark.entity.AttendanceStatus;
import com.timemark.entity.Employee;
import com.timemark.entity.LeaveStatus;
import com.timemark.repository.AttendanceRepository;
import com.timemark.repository.EmployeeRepository;
import com.timemark.repository.LeaveRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Uses a fixed past month (January 2026) so the "workingDaysElapsed" calculation
 * (which is capped at today's date) is always the full month, regardless of when
 * this test actually runs.
 */
@ExtendWith(MockitoExtension.class)
class PayrollServiceTest {

    @Mock private AttendanceRepository attendanceRepository;
    @Mock private LeaveRequestRepository leaveRequestRepository;
    @Mock private EmployeeRepository employeeRepository;

    private PayrollService payrollService;
    private Employee employee;
    private static final YearMonth TEST_MONTH = YearMonth.of(2026, 1);

    @BeforeEach
    void setUp() {
        payrollService = new PayrollService(attendanceRepository, leaveRequestRepository, employeeRepository);
        ReflectionTestUtils.setField(payrollService, "workingDaysPerMonth", 26);

        employee = new Employee();
        employee.setId(1L);
        employee.setFullName("Test Employee");
        employee.setSalary(260000.0); // deliberately divisible by 26 for clean assertions
    }

    private List<LocalDate> weekdaysInTestMonth() {
        List<LocalDate> days = new ArrayList<>();
        for (LocalDate d = TEST_MONTH.atDay(1); !d.isAfter(TEST_MONTH.atEndOfMonth()); d = d.plusDays(1)) {
            if (d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY) {
                days.add(d);
            }
        }
        return days;
    }

    @Test
    void noAttendanceRecordsMeansEveryWorkingDayIsDeductedAsAbsent() {
        when(attendanceRepository.findByEmployeeAndDateBetween(eq(employee), any(), any()))
                .thenReturn(List.of());
        when(leaveRequestRepository.findByEmployeeAndStatus(employee, LeaveStatus.APPROVED))
                .thenReturn(List.of());

        PayrollResponse resp = payrollService.forEmployee(employee, TEST_MONTH);
        int expectedWorkingDays = weekdaysInTestMonth().size();

        assertThat(resp.getWorkingDays()).isEqualTo(expectedWorkingDays);
        assertThat(resp.getAbsentDays()).isEqualTo(expectedWorkingDays);
        assertThat(resp.getPresentDays()).isZero();
        assertThat(resp.getPerDayRate()).isEqualTo(10000.0, offset(0.01));
        assertThat(resp.getDeduction()).isEqualTo(expectedWorkingDays * 10000.0, offset(0.5));
    }

    @Test
    void fullAttendanceEveryWorkingDayMeansNoDeduction() {
        List<Attendance> records = weekdaysInTestMonth().stream().map(d -> {
            Attendance a = new Attendance();
            a.setEmployee(employee);
            a.setDate(d);
            a.setStatus(AttendanceStatus.PRESENT);
            return a;
        }).toList();

        when(attendanceRepository.findByEmployeeAndDateBetween(eq(employee), any(), any()))
                .thenReturn(records);
        when(leaveRequestRepository.findByEmployeeAndStatus(employee, LeaveStatus.APPROVED))
                .thenReturn(List.of());

        PayrollResponse resp = payrollService.forEmployee(employee, TEST_MONTH);

        assertThat(resp.getAbsentDays()).isZero();
        assertThat(resp.getPresentDays()).isEqualTo(weekdaysInTestMonth().size());
        assertThat(resp.getDeduction()).isZero();
        assertThat(resp.getNetPay()).isEqualTo(260000.0, offset(0.01));
    }
}

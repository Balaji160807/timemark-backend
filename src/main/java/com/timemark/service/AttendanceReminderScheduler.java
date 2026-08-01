package com.timemark.service;

import com.timemark.entity.Employee;
import com.timemark.repository.AttendanceRepository;
import com.timemark.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceReminderScheduler {

    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final EmailService emailService;

    @Value("${app.notifications.enabled}")
    private boolean notificationsEnabled;

    /**
     * Runs on the cron defined by app.notifications.reminder-cron (default: weekdays at 09:30).
     * Emails anyone who hasn't checked in yet today. A no-op (just logs) when notifications
     * are disabled, so this is safe to leave running in any environment.
     */
    @Scheduled(cron = "${app.notifications.reminder-cron}")
    public void remindMissingCheckIns() {
        if (!notificationsEnabled) {
            log.debug("Notifications disabled — skipping check-in reminder run.");
            return;
        }

        LocalDate today = LocalDate.now();
        List<Employee> employees = employeeRepository.findAll();

        for (Employee employee : employees) {
            boolean alreadyCheckedIn = attendanceRepository.findByEmployeeAndDate(employee, today).isPresent();
            if (!alreadyCheckedIn && employee.getEmail() != null && !employee.getEmail().isBlank()) {
                emailService.send(
                        employee.getEmail(),
                        "Reminder: you haven't checked in today",
                        String.format("Hi %s,%n%nJust a reminder that you haven't checked in yet today. " +
                                "If you're out sick or on leave, no action needed — otherwise, please check in " +
                                "when you're at the office.", employee.getFullName())
                );
            }
        }
    }
}

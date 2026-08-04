package com.timemark.service;

import com.timemark.dto.AttendanceResponse;
import com.timemark.dto.CheckInRequest;
import com.timemark.entity.Attendance;
import com.timemark.entity.AttendanceStatus;
import com.timemark.entity.Employee;
import com.timemark.repository.AttendanceRepository;
import com.timemark.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private static final LocalTime LATE_THRESHOLD = LocalTime.of(9, 0);

    private final AttendanceRepository attendanceRepository;

    @Value("${app.office.latitude}")
    private double officeLat;

    @Value("${app.office.longitude}")
    private double officeLng;

    @Value("${app.office.radius-meters}")
    private double officeRadiusMeters;

    public AttendanceResponse checkIn(Employee employee, CheckInRequest req) {
        if (req.getLatitude() == null || req.getLongitude() == null) {
            throw new IllegalArgumentException("Location is required to check in. Please allow location access.");
        }

        double distance = GeoUtils.distanceMeters(officeLat, officeLng, req.getLatitude(), req.getLongitude());
        if (distance > officeRadiusMeters) {
            throw new IllegalArgumentException(String.format(
                    "You're %.0fm from the office — check-in must be within %.0fm. Try again once you're on-site.",
                    distance, officeRadiusMeters));
        }

        return recordCheckIn(employee, req.getLatitude(), req.getLongitude());
    }

    /** Checking in via a valid office QR code is itself proof of on-site presence, so no geofencing needed. */
    public AttendanceResponse checkInViaQr(Employee employee) {
        return recordCheckIn(employee, null, null);
    }

    private AttendanceResponse recordCheckIn(Employee employee, Double lat, Double lng) {
        LocalDate today = LocalDate.now();

        attendanceRepository.findByEmployeeAndDate(employee, today).ifPresent(a -> {
            throw new IllegalStateException("Already checked in today");
        });

        LocalTime now = LocalTime.now();
        Attendance attendance = new Attendance();
        attendance.setEmployee(employee);
        attendance.setDate(today);
        attendance.setCheckInTime(now);
        attendance.setCheckInLatitude(lat);
        attendance.setCheckInLongitude(lng);
        attendance.setStatus(now.isAfter(LATE_THRESHOLD) ? AttendanceStatus.LATE : AttendanceStatus.PRESENT);

        return new AttendanceResponse(attendanceRepository.save(attendance));
    }

    public AttendanceResponse checkOut(Employee employee) {
        LocalDate today = LocalDate.now();
        Attendance attendance = attendanceRepository.findByEmployeeAndDate(employee, today)
                .orElseThrow(() -> new IllegalStateException("You haven't checked in today"));

        if (attendance.getCheckOutTime() != null) {
            throw new IllegalStateException("Already checked out today");
        }

        attendance.setCheckOutTime(LocalTime.now());
        return new AttendanceResponse(attendanceRepository.save(attendance));
    }

    public List<AttendanceResponse> myHistory(Employee employee) {
        return attendanceRepository.findByEmployeeOrderByDateDesc(employee)
                .stream().map(AttendanceResponse::new).toList();
    }

    public List<AttendanceResponse> teamToday() {
        return attendanceRepository.findByDate(LocalDate.now())
                .stream().map(AttendanceResponse::new).toList();
    }
}

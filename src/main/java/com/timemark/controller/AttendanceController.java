package com.timemark.controller;

import com.timemark.dto.AttendanceResponse;
import com.timemark.dto.CheckInRequest;
import com.timemark.dto.QrCheckInRequest;
import com.timemark.dto.QrCodeResponse;
import com.timemark.entity.Employee;
import com.timemark.service.AttendanceService;
import com.timemark.service.QrCodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final QrCodeService qrCodeService;

    @PostMapping("/checkin")
    public AttendanceResponse checkIn(@AuthenticationPrincipal Employee employee,
                                       @RequestBody(required = false) CheckInRequest req) {
        return attendanceService.checkIn(employee, req != null ? req : new CheckInRequest());
    }

    @PostMapping("/checkout")
    public AttendanceResponse checkOut(@AuthenticationPrincipal Employee employee) {
        return attendanceService.checkOut(employee);
    }

    @GetMapping("/me")
    public List<AttendanceResponse> myHistory(@AuthenticationPrincipal Employee employee) {
        return attendanceService.myHistory(employee);
    }

    /** HR / Manager / Admin only - enforced in SecurityConfig */
    @GetMapping("/team")
    public List<AttendanceResponse> teamToday() {
        return attendanceService.teamToday();
    }

    /**
     * HR / Manager / Admin only - enforced in SecurityConfig. Generates today's
     * office check-in QR code for display at the entrance. Rotates automatically
     * every day since the token is signed with the current date.
     */
    @GetMapping("/qr-code")
    public QrCodeResponse qrCode() {
        String token = qrCodeService.generateDailyToken();
        return new QrCodeResponse(token, qrCodeService.toBase64Png(token));
    }

    /** Any authenticated employee — scans (or is given) today's office QR code to check in. */
    @PostMapping("/checkin-qr")
    public AttendanceResponse checkInViaQr(@AuthenticationPrincipal Employee employee,
                                            @Valid @RequestBody QrCheckInRequest req) {
        if (!qrCodeService.isValid(req.getToken())) {
            throw new IllegalArgumentException("This QR code is invalid or has expired. Ask HR for today's code.");
        }
        return attendanceService.checkInViaQr(employee);
    }
}

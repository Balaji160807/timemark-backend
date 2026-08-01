package com.timemark.controller;

import com.timemark.dto.PayrollResponse;
import com.timemark.entity.Employee;
import com.timemark.service.PayrollService;
import com.timemark.service.PayslipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/payroll")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;
    private final PayslipService payslipService;

    @GetMapping("/me")
    public PayrollResponse myPayroll(@AuthenticationPrincipal Employee employee,
                                      @RequestParam(required = false) String month) {
        return payrollService.forEmployee(employee, resolveMonth(month));
    }

    /** HR / Manager / Admin only - enforced in SecurityConfig */
    @GetMapping("/team")
    public List<PayrollResponse> teamPayroll(@RequestParam(required = false) String month) {
        return payrollService.forAllEmployees(resolveMonth(month));
    }

    @GetMapping(value = "/me/payslip", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> myPayslip(@AuthenticationPrincipal Employee employee,
                                             @RequestParam(required = false) String month) {
        YearMonth ym = resolveMonth(month);
        PayrollResponse payroll = payrollService.forEmployee(employee, ym);
        byte[] pdf = payslipService.generate(payroll);

        String filename = "payslip-" + employee.getUsername() + "-" + ym + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private YearMonth resolveMonth(String month) {
        return (month == null || month.isBlank()) ? YearMonth.now() : YearMonth.parse(month);
    }
}


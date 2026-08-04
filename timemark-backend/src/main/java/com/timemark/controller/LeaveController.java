package com.timemark.controller;

import com.timemark.dto.LeaveRequestCreateDto;
import com.timemark.dto.LeaveResponse;
import com.timemark.entity.Employee;
import com.timemark.service.LeaveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leave")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    @PostMapping
    public LeaveResponse request(@AuthenticationPrincipal Employee employee,
                                  @Valid @RequestBody LeaveRequestCreateDto dto) {
        return leaveService.requestLeave(employee, dto);
    }

    @GetMapping("/me")
    public List<LeaveResponse> myLeaves(@AuthenticationPrincipal Employee employee) {
        return leaveService.myLeaves(employee);
    }

    /** HR / Manager / Admin only - enforced in SecurityConfig */
    @GetMapping("/pending")
    public List<LeaveResponse> pending() {
        return leaveService.pending();
    }

    @PostMapping("/{id}/approve")
    public LeaveResponse approve(@PathVariable Long id) {
        return leaveService.decide(id, true);
    }

    @PostMapping("/{id}/reject")
    public LeaveResponse reject(@PathVariable Long id) {
        return leaveService.decide(id, false);
    }
}

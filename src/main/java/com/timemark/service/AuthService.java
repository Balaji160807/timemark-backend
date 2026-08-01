package com.timemark.service;

import com.timemark.dto.LoginRequest;
import com.timemark.dto.LoginResponse;
import com.timemark.dto.RegisterRequest;
import com.timemark.entity.Employee;
import com.timemark.repository.EmployeeRepository;
import com.timemark.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public Employee register(RegisterRequest req) {
        if (employeeRepository.existsByUsername(req.getUsername())) {
            throw new IllegalArgumentException("Username already taken");
        }

        Employee employee = new Employee();
        employee.setUsername(req.getUsername());
        employee.setPassword(passwordEncoder.encode(req.getPassword()));
        employee.setFullName(req.getFullName());
        employee.setEmail(req.getEmail());
        employee.setRole(req.getRole());
        employee.setDepartment(req.getDepartment());
        employee.setDesignation(req.getDesignation());
        employee.setSalary(req.getSalary());
        // Default starting leave balances for a new employee
        employee.setCasualLeaveBalance(7);
        employee.setSickLeaveBalance(7);
        employee.setAnnualLeaveBalance(14);

        return employeeRepository.save(employee);
    }

    public LoginResponse login(LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
        );

        Employee employee = employeeRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new IllegalStateException("User vanished after authentication"));

        String token = jwtUtil.generateToken(employee);
        return new LoginResponse(token, employee.getUsername(), employee.getFullName(), employee.getRole().name());
    }
}

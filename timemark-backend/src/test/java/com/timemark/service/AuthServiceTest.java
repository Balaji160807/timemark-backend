package com.timemark.service;

import com.timemark.dto.LoginRequest;
import com.timemark.dto.LoginResponse;
import com.timemark.dto.RegisterRequest;
import com.timemark.entity.Employee;
import com.timemark.entity.Role;
import com.timemark.repository.EmployeeRepository;
import com.timemark.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtUtil jwtUtil;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(employeeRepository, passwordEncoder, authenticationManager, jwtUtil);
    }

    @Test
    void registerRejectsDuplicateUsername() {
        when(employeeRepository.existsByUsername("priya")).thenReturn(true);

        RegisterRequest req = new RegisterRequest();
        req.setUsername("priya");
        req.setPassword("pass123");
        req.setFullName("Priya Fernando");
        req.setRole(Role.EMPLOYEE);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already taken");
    }

    @Test
    void registerGivesNewEmployeeDefaultLeaveBalances() {
        when(employeeRepository.existsByUsername("priya")).thenReturn(false);
        when(passwordEncoder.encode("pass123")).thenReturn("hashed");
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        RegisterRequest req = new RegisterRequest();
        req.setUsername("priya");
        req.setPassword("pass123");
        req.setFullName("Priya Fernando");
        req.setRole(Role.EMPLOYEE);

        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        authService.register(req);
        verify(employeeRepository).save(captor.capture());

        Employee saved = captor.getValue();
        assertThat(saved.getPassword()).isEqualTo("hashed");
        assertThat(saved.getCasualLeaveBalance()).isEqualTo(7);
        assertThat(saved.getSickLeaveBalance()).isEqualTo(7);
        assertThat(saved.getAnnualLeaveBalance()).isEqualTo(14);
    }

    @Test
    void loginReturnsTokenAndProfileOnSuccess() {
        Employee employee = new Employee();
        employee.setUsername("priya");
        employee.setFullName("Priya Fernando");
        employee.setRole(Role.EMPLOYEE);

        when(employeeRepository.findByUsername("priya")).thenReturn(Optional.of(employee));
        when(jwtUtil.generateToken(employee)).thenReturn("fake.jwt.token");

        LoginRequest req = new LoginRequest();
        req.setUsername("priya");
        req.setPassword("pass123");

        LoginResponse resp = authService.login(req);

        assertThat(resp.getToken()).isEqualTo("fake.jwt.token");
        assertThat(resp.getUsername()).isEqualTo("priya");
        assertThat(resp.getRole()).isEqualTo("EMPLOYEE");
        verify(authenticationManager).authenticate(any());
    }
}

package com.timemark.dto;

import com.timemark.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
    @NotBlank
    private String username;

    @NotBlank
    private String password;

    @NotBlank
    private String fullName;

    private String email;

    @NotNull
    private Role role;

    private String department;
    private String designation;
    private Double salary;
}

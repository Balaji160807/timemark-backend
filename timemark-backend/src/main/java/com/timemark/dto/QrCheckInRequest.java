package com.timemark.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QrCheckInRequest {
    @NotBlank
    private String token;
}

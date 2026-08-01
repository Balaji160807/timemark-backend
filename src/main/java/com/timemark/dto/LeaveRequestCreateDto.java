package com.timemark.dto;

import com.timemark.entity.LeaveType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class LeaveRequestCreateDto {
    @NotNull
    private LeaveType type;

    @NotNull
    private LocalDate fromDate;

    @NotNull
    private LocalDate toDate;

    private String reason;
}

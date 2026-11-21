package com.sahilpanmand.tatkal.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalTime;

public record TrainRequest(
        @NotBlank @Pattern(regexp = "\\d{5}", message = "must be 5 digits") String trainNumber,
        @NotBlank String name,
        @NotBlank String source,
        @NotBlank String destination,
        @NotNull LocalTime departureTime,
        @NotNull LocalTime arrivalTime,
        @NotNull @Positive BigDecimal fare,
        @Min(1) @Max(500) int seatCount) {
}

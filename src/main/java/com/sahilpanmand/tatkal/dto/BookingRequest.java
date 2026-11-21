package com.sahilpanmand.tatkal.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BookingRequest(
        @NotNull Long journeyId,
        @NotEmpty @Size(max = 4, message = "you can book 1 to 4 seats") List<@NotNull Long> seatIds) {
}

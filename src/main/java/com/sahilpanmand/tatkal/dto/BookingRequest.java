package com.sahilpanmand.tatkal.dto;

import jakarta.validation.constraints.NotNull;

// One booking = one seat on one journey.
public record BookingRequest(@NotNull Long journeyId, @NotNull Long seatId) {
}

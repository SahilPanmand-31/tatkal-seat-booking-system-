package com.sahilpanmand.tatkal.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record JourneyRequest(@NotNull Long trainId, @NotNull LocalDate journeyDate) {
}

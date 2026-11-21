package com.sahilpanmand.tatkal.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record BookingResponse(String pnr, String status, String trainNumber, String trainName,
                              LocalDate journeyDate, int seatNumber, BigDecimal fare, Instant bookedAt) {
}

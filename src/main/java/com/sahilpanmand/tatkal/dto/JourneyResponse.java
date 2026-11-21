package com.sahilpanmand.tatkal.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

// One search result: a train running on a date, with how many seats are still free.
public record JourneyResponse(Long journeyId, String trainNumber, String trainName, String source, String destination,
                              LocalDate journeyDate, LocalTime departureTime, LocalTime arrivalTime,
                              BigDecimal fare, long availableSeats) {
}

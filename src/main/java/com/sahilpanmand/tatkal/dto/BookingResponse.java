package com.sahilpanmand.tatkal.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// One PNR with all its seats.
public record BookingResponse(String pnr, String status, String trainNumber, String trainName,
                              LocalDate journeyDate, List<Integer> seatNumbers, BigDecimal totalFare) {
}

package com.sahilpanmand.tatkal.dto;

import java.math.BigDecimal;
import java.time.LocalTime;

public record TrainResponse(Long id, String trainNumber, String name, String source, String destination,
                            LocalTime departureTime, LocalTime arrivalTime, BigDecimal fare, long seatCount) {
}

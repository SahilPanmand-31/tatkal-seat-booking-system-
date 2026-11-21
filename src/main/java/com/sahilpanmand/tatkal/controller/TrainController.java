package com.sahilpanmand.tatkal.controller;

import com.sahilpanmand.tatkal.dto.JourneyResponse;
import com.sahilpanmand.tatkal.dto.SeatResponse;
import com.sahilpanmand.tatkal.service.TrainService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

// Public, read-only endpoints
@RestController
@RequestMapping("/api")
public class TrainController {

    private final TrainService trainService;

    public TrainController(TrainService trainService) {
        this.trainService = trainService;
    }

    // e.g. GET /api/trains/search?source=Pune&destination=Mumbai&date=2026-11-22
    @GetMapping("/trains/search")
    public List<JourneyResponse> search(@RequestParam String source,
                                        @RequestParam String destination,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return trainService.search(source, destination, date);
    }

    @GetMapping("/journeys/{id}/seats")
    public List<SeatResponse> seats(@PathVariable Long id) {
        return trainService.getSeats(id);
    }
}

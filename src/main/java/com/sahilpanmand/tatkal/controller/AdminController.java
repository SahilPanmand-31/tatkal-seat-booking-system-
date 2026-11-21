package com.sahilpanmand.tatkal.controller;

import com.sahilpanmand.tatkal.dto.JourneyRequest;
import com.sahilpanmand.tatkal.dto.JourneyResponse;
import com.sahilpanmand.tatkal.dto.TrainRequest;
import com.sahilpanmand.tatkal.dto.TrainResponse;
import com.sahilpanmand.tatkal.service.TrainService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

// Only ADMIN can reach these (see SecurityConfig)
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final TrainService trainService;

    public AdminController(TrainService trainService) {
        this.trainService = trainService;
    }

    @PostMapping("/trains")
    @ResponseStatus(HttpStatus.CREATED)
    public TrainResponse createTrain(@Valid @RequestBody TrainRequest request) {
        return trainService.createTrain(request);
    }

    @PostMapping("/journeys")
    @ResponseStatus(HttpStatus.CREATED)
    public JourneyResponse createJourney(@Valid @RequestBody JourneyRequest request) {
        return trainService.createJourney(request);
    }
}

package com.sahilpanmand.tatkal.service;

import com.sahilpanmand.tatkal.dto.*;
import com.sahilpanmand.tatkal.entity.Journey;
import com.sahilpanmand.tatkal.entity.Seat;
import com.sahilpanmand.tatkal.entity.Train;
import com.sahilpanmand.tatkal.exception.BadRequestException;
import com.sahilpanmand.tatkal.exception.ConflictException;
import com.sahilpanmand.tatkal.exception.ResourceNotFoundException;
import com.sahilpanmand.tatkal.repository.BookingRepository;
import com.sahilpanmand.tatkal.repository.JourneyRepository;
import com.sahilpanmand.tatkal.repository.SeatRepository;
import com.sahilpanmand.tatkal.repository.TrainRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TrainService {

    private final TrainRepository trainRepository;
    private final SeatRepository seatRepository;
    private final JourneyRepository journeyRepository;
    private final BookingRepository bookingRepository;

    public TrainService(TrainRepository trainRepository, SeatRepository seatRepository,
                        JourneyRepository journeyRepository, BookingRepository bookingRepository) {
        this.trainRepository = trainRepository;
        this.seatRepository = seatRepository;
        this.journeyRepository = journeyRepository;
        this.bookingRepository = bookingRepository;
    }

    // Admin: add a train and create its seats 1..seatCount
    @Transactional
    public TrainResponse createTrain(TrainRequest request) {
        if (trainRepository.existsByTrainNumber(request.trainNumber())) {
            throw new ConflictException("Train " + request.trainNumber() + " already exists");
        }
        Train train = trainRepository.save(new Train(request.trainNumber(), request.name(), request.source(),
                request.destination(), request.departureTime(), request.arrivalTime(), request.fare()));
        for (int i = 1; i <= request.seatCount(); i++) {
            seatRepository.save(new Seat(train, i));
        }
        return new TrainResponse(train.getId(), train.getTrainNumber(), train.getName(), train.getSource(),
                train.getDestination(), train.getDepartureTime(), train.getArrivalTime(), train.getFare(),
                request.seatCount());
    }

    // Admin: schedule a train on a date
    @Transactional
    public JourneyResponse createJourney(JourneyRequest request) {
        Train train = trainRepository.findById(request.trainId())
                .orElseThrow(() -> new ResourceNotFoundException("Train not found: " + request.trainId()));
        if (request.journeyDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Journey date cannot be in the past");
        }
        if (journeyRepository.existsByTrainIdAndJourneyDate(train.getId(), request.journeyDate())) {
            throw new ConflictException("Journey already exists for this train and date");
        }
        Journey journey = journeyRepository.save(new Journey(train, request.journeyDate()));
        return toResponse(journey);
    }

    @Transactional(readOnly = true)
    public List<JourneyResponse> search(String source, String destination, LocalDate date) {
        return journeyRepository.search(source.trim(), destination.trim(), date).stream()
                .map(this::toResponse)
                .toList();
    }

    // Seat map for one journey: every seat of the train, marked free or booked
    @Transactional(readOnly = true)
    public List<SeatResponse> getSeats(Long journeyId) {
        Journey journey = journeyRepository.findById(journeyId)
                .orElseThrow(() -> new ResourceNotFoundException("Journey not found: " + journeyId));
        Set<Long> booked = new HashSet<>(bookingRepository.findBookedSeatIds(journeyId));
        return seatRepository.findByTrainIdOrderBySeatNumber(journey.getTrain().getId()).stream()
                .map(seat -> new SeatResponse(seat.getId(), seat.getSeatNumber(), !booked.contains(seat.getId())))
                .toList();
    }

    private JourneyResponse toResponse(Journey journey) {
        Train t = journey.getTrain();
        long available = seatRepository.countByTrainId(t.getId())
                - bookingRepository.countByJourneyIdAndActiveTrue(journey.getId());
        return new JourneyResponse(journey.getId(), t.getTrainNumber(), t.getName(), t.getSource(),
                t.getDestination(), journey.getJourneyDate(), t.getDepartureTime(), t.getArrivalTime(),
                t.getFare(), available);
    }
}

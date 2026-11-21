package com.sahilpanmand.tatkal.service;

import com.sahilpanmand.tatkal.dto.JourneyResponse;
import com.sahilpanmand.tatkal.dto.SeatResponse;
import com.sahilpanmand.tatkal.entity.BookingStatus;
import com.sahilpanmand.tatkal.entity.Journey;
import com.sahilpanmand.tatkal.entity.Train;
import com.sahilpanmand.tatkal.exception.ResourceNotFoundException;
import com.sahilpanmand.tatkal.repository.BookingRepository;
import com.sahilpanmand.tatkal.repository.JourneyRepository;
import com.sahilpanmand.tatkal.repository.SeatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TrainService {

    private final SeatRepository seatRepository;
    private final JourneyRepository journeyRepository;
    private final BookingRepository bookingRepository;

    public TrainService(SeatRepository seatRepository, JourneyRepository journeyRepository,
                        BookingRepository bookingRepository) {
        this.seatRepository = seatRepository;
        this.journeyRepository = journeyRepository;
        this.bookingRepository = bookingRepository;
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
        Set<Long> booked = new HashSet<>(
                bookingRepository.findSeatIdsByJourneyIdAndStatus(journeyId, BookingStatus.CONFIRMED));
        return seatRepository.findByTrainIdOrderBySeatNumber(journey.getTrain().getId()).stream()
                .map(seat -> new SeatResponse(seat.getId(), seat.getSeatNumber(), !booked.contains(seat.getId())))
                .toList();
    }

    private JourneyResponse toResponse(Journey journey) {
        Train t = journey.getTrain();
        long available = seatRepository.countByTrainId(t.getId())
                - bookingRepository.countByJourneyIdAndStatus(journey.getId(), BookingStatus.CONFIRMED);
        return new JourneyResponse(journey.getId(), t.getTrainNumber(), t.getName(), t.getSource(),
                t.getDestination(), journey.getJourneyDate(), t.getDepartureTime(), t.getArrivalTime(),
                t.getFare(), available);
    }
}

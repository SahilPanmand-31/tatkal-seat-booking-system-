package com.sahilpanmand.tatkal.service;

import com.sahilpanmand.tatkal.dto.BookingRequest;
import com.sahilpanmand.tatkal.dto.BookingResponse;
import com.sahilpanmand.tatkal.entity.*;
import com.sahilpanmand.tatkal.exception.BadRequestException;
import com.sahilpanmand.tatkal.exception.ConflictException;
import com.sahilpanmand.tatkal.exception.ResourceNotFoundException;
import com.sahilpanmand.tatkal.repository.BookingRepository;
import com.sahilpanmand.tatkal.repository.JourneyRepository;
import com.sahilpanmand.tatkal.repository.SeatRepository;
import com.sahilpanmand.tatkal.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.*;

@Service
public class BookingService {

    private static final int MAX_SEATS_PER_PNR = 4;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final BookingRepository bookingRepository;
    private final SeatRepository seatRepository;
    private final JourneyRepository journeyRepository;
    private final UserRepository userRepository;

    public BookingService(BookingRepository bookingRepository, SeatRepository seatRepository,
                          JourneyRepository journeyRepository, UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.seatRepository = seatRepository;
        this.journeyRepository = journeyRepository;
        this.userRepository = userRepository;
    }

    /**
     * Books 1-4 seats under one PNR. All or nothing: if any seat is taken, nothing is saved.
     *
     * How double booking is prevented:
     * 1. findAndLockSeats = SELECT ... FOR UPDATE. Only one transaction can hold the lock on a seat row;
     *    others wanting the same seat wait until it commits.
     * 2. While holding the lock we check whether the seat already has a CONFIRMED booking.
     *    The waiting transaction runs this check only after the first one committed, so it sees that booking.
     * 3. Backup: UNIQUE (journey_id, seat_id, active) on bookings rejects a second confirmed row anyway.
     */
    @Transactional
    public BookingResponse book(String email, BookingRequest request) {
        List<Long> seatIds = request.seatIds();
        if (seatIds == null || seatIds.isEmpty() || seatIds.size() > MAX_SEATS_PER_PNR) {
            throw new BadRequestException("You can book 1 to " + MAX_SEATS_PER_PNR + " seats");
        }
        if (new HashSet<>(seatIds).size() != seatIds.size()) {
            throw new BadRequestException("Same seat selected twice");
        }

        User user = findUser(email);
        Journey journey = journeyRepository.findById(request.journeyId())
                .orElseThrow(() -> new ResourceNotFoundException("Journey not found: " + request.journeyId()));

        // Step 1: lock the seat rows (other bookings for these seats wait here)
        List<Seat> seats = seatRepository.findAndLockSeats(journey.getTrain().getId(), seatIds);
        if (seats.size() != seatIds.size()) {
            throw new ResourceNotFoundException("One or more seats do not belong to this train");
        }

        // Step 2: with the lock held, check nobody has booked them already
        List<Booking> taken = bookingRepository.findActiveBookings(journey.getId(), seatIds);
        if (!taken.isEmpty()) {
            throw new ConflictException("Seat(s) already booked: " + taken.stream()
                    .map(b -> b.getSeat().getSeatNumber()).sorted().toList());
        }

        // Step 3: save one row per seat, all with the same PNR
        String pnr = generatePnr();
        BigDecimal fare = journey.getTrain().getFare();
        List<Booking> bookings = new ArrayList<>();
        for (Seat seat : seats) {
            bookings.add(new Booking(pnr, user, journey, seat, fare));
        }
        bookingRepository.saveAllAndFlush(bookings);
        return toResponse(bookings);
    }

    // Only the person who booked can see a PNR. For anyone else we say "not found"
    // so we don't reveal that the PNR exists.
    @Transactional(readOnly = true)
    public BookingResponse getByPnr(String email, String pnr) {
        return toResponse(findOwnBooking(email, pnr));
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> myBookings(String email) {
        // group the seat rows by PNR, keeping newest first
        Map<String, List<Booking>> byPnr = new LinkedHashMap<>();
        for (Booking b : bookingRepository.findByUserEmailOrderByBookedAtDesc(email)) {
            byPnr.computeIfAbsent(b.getPnr(), k -> new ArrayList<>()).add(b);
        }
        return byPnr.values().stream().map(this::toResponse).toList();
    }

    // Cancels every seat on the PNR. Cancelled seats become free for others.
    @Transactional
    public BookingResponse cancel(String email, String pnr) {
        List<Booking> bookings = findOwnBooking(email, pnr);
        if (bookings.get(0).getStatus() == BookingStatus.CANCELLED) {
            throw new ConflictException("Booking " + pnr + " is already cancelled");
        }
        bookings.forEach(Booking::cancel);
        bookingRepository.saveAll(bookings);
        return toResponse(bookings);
    }

    private List<Booking> findOwnBooking(String email, String pnr) {
        List<Booking> bookings = bookingRepository.findByPnrOrderBySeatId(pnr);
        if (bookings.isEmpty() || !bookings.get(0).getUser().getEmail().equals(email)) {
            throw new ResourceNotFoundException("Booking not found: " + pnr);
        }
        return bookings;
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    // 10-digit random PNR, retried in the rare case it already exists
    private String generatePnr() {
        String pnr;
        do {
            pnr = String.valueOf(1_000_000_000L + (long) (RANDOM.nextDouble() * 9_000_000_000L));
        } while (bookingRepository.existsByPnr(pnr));
        return pnr;
    }

    private BookingResponse toResponse(List<Booking> bookings) {
        Booking first = bookings.get(0);
        Train train = first.getJourney().getTrain();
        List<Integer> seatNumbers = bookings.stream().map(b -> b.getSeat().getSeatNumber()).sorted().toList();
        BigDecimal total = bookings.stream().map(Booking::getFare).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new BookingResponse(first.getPnr(), first.getStatus().name(), train.getTrainNumber(),
                train.getName(), first.getJourney().getJourneyDate(), seatNumbers, total);
    }
}

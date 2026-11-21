package com.sahilpanmand.tatkal.service;

import com.sahilpanmand.tatkal.dto.BookingRequest;
import com.sahilpanmand.tatkal.dto.BookingResponse;
import com.sahilpanmand.tatkal.entity.*;
import com.sahilpanmand.tatkal.exception.ConflictException;
import com.sahilpanmand.tatkal.exception.ResourceNotFoundException;
import com.sahilpanmand.tatkal.repository.BookingRepository;
import com.sahilpanmand.tatkal.repository.JourneyRepository;
import com.sahilpanmand.tatkal.repository.SeatRepository;
import com.sahilpanmand.tatkal.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

@Service
public class BookingService {

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
     * Books one seat on one journey.
     *
     * How double booking is prevented:
     * 1. findAndLockSeat runs SELECT ... FOR UPDATE, so only one transaction at a time can hold the seat row.
     *    Everyone else asking for the same seat waits until that transaction commits.
     * 2. With the lock held, we check whether the seat already has a CONFIRMED booking.
     *    A request that had to wait runs this check after the first one committed, so it sees
     *    that booking and gets 409 Conflict.
     * 3. The lock is released automatically when this @Transactional method commits or rolls back.
     */
    @Transactional
    public BookingResponse book(String email, BookingRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Journey journey = journeyRepository.findById(request.journeyId())
                .orElseThrow(() -> new ResourceNotFoundException("Journey not found: " + request.journeyId()));

        // Step 1: lock the seat row (other requests for this seat wait here)
        Seat seat = seatRepository.findAndLockSeat(journey.getTrain().getId(), request.seatId())
                .orElseThrow(() -> new ResourceNotFoundException("Seat not found on this train: " + request.seatId()));

        // Step 2: with the lock held, make sure nobody has booked it already
        if (bookingRepository.existsByJourneyIdAndSeatIdAndStatus(journey.getId(), seat.getId(), BookingStatus.CONFIRMED)) {
            throw new ConflictException("Seat " + seat.getSeatNumber() + " is already booked");
        }

        // Step 3: save the booking
        Booking booking = new Booking(generatePnr(), user, journey, seat, journey.getTrain().getFare());
        return toResponse(bookingRepository.save(booking));
    }

    @Transactional(readOnly = true)
    public BookingResponse getByPnr(String email, String pnr) {
        return toResponse(findOwnBooking(email, pnr));
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> myBookings(String email) {
        return bookingRepository.findByUserEmailOrderByBookedAtDesc(email).stream()
                .map(this::toResponse)
                .toList();
    }

    // Cancelling frees the seat: it no longer has a CONFIRMED booking, so someone else can book it.
    @Transactional
    public BookingResponse cancel(String email, String pnr) {
        Booking booking = findOwnBooking(email, pnr);
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new ConflictException("Booking " + pnr + " is already cancelled");
        }
        booking.cancel();
        return toResponse(bookingRepository.save(booking));
    }

    // Only the person who booked can see a PNR. For anyone else we say "not found"
    // so we don't reveal that the PNR exists.
    private Booking findOwnBooking(String email, String pnr) {
        return bookingRepository.findByPnr(pnr)
                .filter(b -> b.getUser().getEmail().equals(email))
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + pnr));
    }

    // 10-digit random PNR, retried in the rare case it already exists
    private String generatePnr() {
        String pnr;
        do {
            pnr = String.valueOf(1_000_000_000L + (long) (RANDOM.nextDouble() * 9_000_000_000L));
        } while (bookingRepository.existsByPnr(pnr));
        return pnr;
    }

    private BookingResponse toResponse(Booking b) {
        Train train = b.getJourney().getTrain();
        return new BookingResponse(b.getPnr(), b.getStatus().name(), train.getTrainNumber(), train.getName(),
                b.getJourney().getJourneyDate(), b.getSeat().getSeatNumber(), b.getFare(), b.getBookedAt());
    }
}

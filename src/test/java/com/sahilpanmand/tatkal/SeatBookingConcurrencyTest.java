package com.sahilpanmand.tatkal;

import com.sahilpanmand.tatkal.dto.BookingRequest;
import com.sahilpanmand.tatkal.entity.*;
import com.sahilpanmand.tatkal.repository.*;
import com.sahilpanmand.tatkal.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 30 different users try to book the SAME seat at the SAME moment.
 * Exactly one must succeed; the database must hold exactly one confirmed booking.
 */
@SpringBootTest
class SeatBookingConcurrencyTest {

    private static final int THREADS = 30;

    @Autowired BookingService bookingService;
    @Autowired UserRepository userRepository;
    @Autowired TrainRepository trainRepository;
    @Autowired SeatRepository seatRepository;
    @Autowired JourneyRepository journeyRepository;
    @Autowired BookingRepository bookingRepository;

    @Test
    void onlyOneUserGetsTheSeat() throws Exception {
        Train train = trainRepository.save(new Train("99999", "Test Express", "Pune", "Mumbai",
                LocalTime.of(8, 0), LocalTime.of(11, 0), new BigDecimal("300.00")));
        Seat seat = seatRepository.save(new Seat(train, 1));
        Journey journey = journeyRepository.save(new Journey(train, LocalDate.now().plusDays(1)));

        List<String> emails = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            String email = "user" + i + "@test.com";
            userRepository.save(new User("User " + i, email, "hash", Role.USER));
            emails.add(email);
        }

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch startTogether = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        for (String email : emails) {
            futures.add(pool.submit(() -> {
                try {
                    startTogether.await();
                    bookingService.book(email, new BookingRequest(journey.getId(), List.of(seat.getId())));
                    success.incrementAndGet();
                } catch (Exception e) {
                    failed.incrementAndGet();   // ConflictException (seat taken) or lock timeout
                }
                return null;
            }));
        }

        startTogether.countDown();          // release all threads at once
        for (Future<?> f : futures) {
            f.get(60, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertThat(success.get()).isEqualTo(1);
        assertThat(failed.get()).isEqualTo(THREADS - 1);
        assertThat(bookingRepository.countByJourneyIdAndActiveTrue(journey.getId())).isEqualTo(1);
    }
}

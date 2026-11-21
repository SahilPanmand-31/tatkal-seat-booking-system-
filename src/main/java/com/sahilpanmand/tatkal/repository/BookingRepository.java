package com.sahilpanmand.tatkal.repository;

import com.sahilpanmand.tatkal.entity.Booking;
import com.sahilpanmand.tatkal.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Is this seat already booked (CONFIRMED) on this journey?
    boolean existsByJourneyIdAndSeatIdAndStatus(Long journeyId, Long seatId, BookingStatus status);

    @Query("select b.seat.id from Booking b where b.journey.id = :journeyId and b.status = :status")
    List<Long> findSeatIdsByJourneyIdAndStatus(@Param("journeyId") Long journeyId, @Param("status") BookingStatus status);

    long countByJourneyIdAndStatus(Long journeyId, BookingStatus status);

    Optional<Booking> findByPnr(String pnr);

    List<Booking> findByUserEmailOrderByBookedAtDesc(String email);

    boolean existsByPnr(String pnr);
}

package com.sahilpanmand.tatkal.repository;

import com.sahilpanmand.tatkal.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Confirmed bookings for these seats on this journey (empty list = all seats free)
    @Query("select b from Booking b where b.journey.id = :journeyId and b.seat.id in :seatIds and b.active = true")
    List<Booking> findActiveBookings(@Param("journeyId") Long journeyId, @Param("seatIds") Collection<Long> seatIds);

    @Query("select b.seat.id from Booking b where b.journey.id = :journeyId and b.active = true")
    List<Long> findBookedSeatIds(@Param("journeyId") Long journeyId);

    long countByJourneyIdAndActiveTrue(Long journeyId);

    List<Booking> findByPnrOrderBySeatId(String pnr);

    List<Booking> findByUserEmailOrderByBookedAtDesc(String email);

    boolean existsByPnr(String pnr);
}

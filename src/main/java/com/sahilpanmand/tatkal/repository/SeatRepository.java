package com.sahilpanmand.tatkal.repository;

import com.sahilpanmand.tatkal.entity.Seat;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByTrainIdOrderBySeatNumber(Long trainId);

    long countByTrainId(Long trainId);

    /**
     * The heart of the project. Runs SELECT ... FOR UPDATE on the requested seat rows.
     * - The first transaction gets the row locks; every other transaction asking for the
     *   same seats WAITS here until the first one commits or rolls back.
     * - ORDER BY id: everyone locks rows in the same order, so two multi-seat bookings
     *   can never deadlock each other.
     * - lock.timeout: give up after 5 seconds instead of waiting forever.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    @Query("select s from Seat s where s.train.id = :trainId and s.id in :seatIds order by s.id")
    List<Seat> findAndLockSeats(@Param("trainId") Long trainId, @Param("seatIds") Collection<Long> seatIds);
}

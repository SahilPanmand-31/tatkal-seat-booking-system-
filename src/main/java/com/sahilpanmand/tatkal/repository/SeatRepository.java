package com.sahilpanmand.tatkal.repository;

import com.sahilpanmand.tatkal.entity.Seat;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByTrainIdOrderBySeatNumber(Long trainId);

    long countByTrainId(Long trainId);

    /**
     * The heart of the project: runs SELECT ... FOR UPDATE on one seat row.
     * The first transaction gets the lock; any other transaction asking for the same seat
     * WAITS here until the first one commits or rolls back.
     * lock.timeout = 5000 ms, so a request gives up instead of waiting forever.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    @Query("select s from Seat s where s.train.id = :trainId and s.id = :seatId")
    Optional<Seat> findAndLockSeat(@Param("trainId") Long trainId, @Param("seatId") Long seatId);
}

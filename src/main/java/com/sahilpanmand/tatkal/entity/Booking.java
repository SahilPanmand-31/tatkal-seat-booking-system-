package com.sahilpanmand.tatkal.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One row = one seat on one journey. A PNR can cover up to 4 seats, so several rows share a PNR.
 *
 * The database guarantees one active booking per seat per journey with
 * UNIQUE (journey_id, seat_id, active):
 *   - CONFIRMED rows have active = TRUE, so a second confirmed row for the same seat is rejected.
 *   - CANCELLED rows have active = NULL. NULLs never clash in a unique constraint,
 *     so a seat can be cancelled and booked again any number of times.
 */
@Entity
@Table(name = "bookings",
        uniqueConstraints = @UniqueConstraint(name = "uk_one_active_booking_per_seat",
                columnNames = {"journey_id", "seat_id", "active"}),
        indexes = @Index(name = "idx_booking_pnr", columnList = "pnr"))
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String pnr;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "journey_id", nullable = false)
    private Journey journey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private BookingStatus status;

    // TRUE while confirmed, NULL once cancelled (see class comment)
    private Boolean active;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal fare;

    @Column(nullable = false)
    private Instant bookedAt;

    private Instant cancelledAt;

    protected Booking() {
    }

    public Booking(String pnr, User user, Journey journey, Seat seat, BigDecimal fare) {
        this.pnr = pnr;
        this.user = user;
        this.journey = journey;
        this.seat = seat;
        this.fare = fare;
        this.status = BookingStatus.CONFIRMED;
        this.active = Boolean.TRUE;
        this.bookedAt = Instant.now();
    }

    public void cancel() {
        this.status = BookingStatus.CANCELLED;
        this.active = null;
        this.cancelledAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getPnr() { return pnr; }
    public User getUser() { return user; }
    public Journey getJourney() { return journey; }
    public Seat getSeat() { return seat; }
    public BookingStatus getStatus() { return status; }
    public Boolean getActive() { return active; }
    public BigDecimal getFare() { return fare; }
    public Instant getBookedAt() { return bookedAt; }
    public Instant getCancelledAt() { return cancelledAt; }
}

package com.sahilpanmand.tatkal.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

// One booking = one seat on one journey, identified by a 10-digit PNR.
@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
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

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal fare;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private BookingStatus status;

    @Column(nullable = false)
    private Instant bookedAt;

    protected Booking() {
        // needed by JPA
    }

    public Booking(String pnr, User user, Journey journey, Seat seat, BigDecimal fare) {
        this.pnr = pnr;
        this.user = user;
        this.journey = journey;
        this.seat = seat;
        this.fare = fare;
        this.status = BookingStatus.CONFIRMED;
        this.bookedAt = Instant.now();
    }

    public void cancel() {
        this.status = BookingStatus.CANCELLED;
    }

    public Long getId() { return id; }
    public String getPnr() { return pnr; }
    public User getUser() { return user; }
    public Journey getJourney() { return journey; }
    public Seat getSeat() { return seat; }
    public BigDecimal getFare() { return fare; }
    public BookingStatus getStatus() { return status; }
    public Instant getBookedAt() { return bookedAt; }
}

package com.sahilpanmand.tatkal.entity;

import jakarta.persistence.*;

// A physical seat on a train. Whether it is booked depends on the journey (date),
// so that information lives in Booking, not here.
@Entity
@Table(name = "seats",
        uniqueConstraints = @UniqueConstraint(name = "uk_seat_train_number", columnNames = {"train_id", "seat_number"}))
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "train_id", nullable = false)
    private Train train;

    @Column(name = "seat_number", nullable = false)
    private int seatNumber;

    protected Seat() {
    }

    public Seat(Train train, int seatNumber) {
        this.train = train;
        this.seatNumber = seatNumber;
    }

    public Long getId() { return id; }
    public Train getTrain() { return train; }
    public int getSeatNumber() { return seatNumber; }
}

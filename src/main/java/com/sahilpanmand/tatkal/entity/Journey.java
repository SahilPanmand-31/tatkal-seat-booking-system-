package com.sahilpanmand.tatkal.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

// One run of a train on one date, e.g. 12124 Deccan Queen on 2026-11-21.
@Entity
@Table(name = "journeys",
        uniqueConstraints = @UniqueConstraint(name = "uk_journey_train_date", columnNames = {"train_id", "journey_date"}))
public class Journey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "train_id", nullable = false)
    private Train train;

    @Column(name = "journey_date", nullable = false)
    private LocalDate journeyDate;

    protected Journey() {
    }

    public Journey(Train train, LocalDate journeyDate) {
        this.train = train;
        this.journeyDate = journeyDate;
    }

    public Long getId() { return id; }
    public Train getTrain() { return train; }
    public LocalDate getJourneyDate() { return journeyDate; }
}

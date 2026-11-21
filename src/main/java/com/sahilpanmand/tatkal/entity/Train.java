package com.sahilpanmand.tatkal.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Table(name = "trains")
public class Train {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 5)
    private String trainNumber;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 60)
    private String source;

    @Column(nullable = false, length = 60)
    private String destination;

    @Column(nullable = false)
    private LocalTime departureTime;

    @Column(nullable = false)
    private LocalTime arrivalTime;

    // Tatkal fare for one seat
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal fare;

    protected Train() {
    }

    public Train(String trainNumber, String name, String source, String destination,
                 LocalTime departureTime, LocalTime arrivalTime, BigDecimal fare) {
        this.trainNumber = trainNumber;
        this.name = name;
        this.source = source;
        this.destination = destination;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.fare = fare;
    }

    public Long getId() { return id; }
    public String getTrainNumber() { return trainNumber; }
    public String getName() { return name; }
    public String getSource() { return source; }
    public String getDestination() { return destination; }
    public LocalTime getDepartureTime() { return departureTime; }
    public LocalTime getArrivalTime() { return arrivalTime; }
    public BigDecimal getFare() { return fare; }
}

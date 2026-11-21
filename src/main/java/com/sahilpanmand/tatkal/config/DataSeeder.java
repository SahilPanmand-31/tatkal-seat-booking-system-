package com.sahilpanmand.tatkal.config;

import com.sahilpanmand.tatkal.entity.*;
import com.sahilpanmand.tatkal.repository.JourneyRepository;
import com.sahilpanmand.tatkal.repository.SeatRepository;
import com.sahilpanmand.tatkal.repository.TrainRepository;
import com.sahilpanmand.tatkal.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

// On startup: create the admin user and two sample Pune -> Mumbai trains (only if the DB is empty).
// Train timings and fares are sample data for the demo, not live IRCTC data.
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private static final int SEATS_PER_TRAIN = 20;
    private static final int DAYS_AHEAD = 3;

    private final UserRepository userRepository;
    private final TrainRepository trainRepository;
    private final SeatRepository seatRepository;
    private final JourneyRepository journeyRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public DataSeeder(UserRepository userRepository, TrainRepository trainRepository, SeatRepository seatRepository,
                      JourneyRepository journeyRepository, PasswordEncoder passwordEncoder,
                      @Value("${app.admin.email}") String adminEmail,
                      @Value("${app.admin.password}") String adminPassword) {
        this.userRepository = userRepository;
        this.trainRepository = trainRepository;
        this.seatRepository = seatRepository;
        this.journeyRepository = journeyRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!userRepository.existsByEmail(adminEmail)) {
            userRepository.save(new User("Admin", adminEmail, passwordEncoder.encode(adminPassword), Role.ADMIN));
        }
        if (trainRepository.count() > 0) {
            return;
        }
        addTrain(new Train("12124", "Deccan Queen", "Pune", "Mumbai",
                LocalTime.of(7, 15), LocalTime.of(10, 25), new BigDecimal("450.00")));
        addTrain(new Train("11010", "Sinhagad Express", "Pune", "Mumbai",
                LocalTime.of(6, 5), LocalTime.of(10, 0), new BigDecimal("400.00")));
    }

    private void addTrain(Train train) {
        trainRepository.save(train);
        for (int i = 1; i <= SEATS_PER_TRAIN; i++) {
            seatRepository.save(new Seat(train, i));
        }
        for (int d = 1; d <= DAYS_AHEAD; d++) {
            journeyRepository.save(new Journey(train, LocalDate.now().plusDays(d)));
        }
    }
}

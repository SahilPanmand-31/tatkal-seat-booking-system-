package com.sahilpanmand.tatkal.service;

import com.sahilpanmand.tatkal.dto.BookingRequest;
import com.sahilpanmand.tatkal.dto.BookingResponse;
import com.sahilpanmand.tatkal.entity.*;
import com.sahilpanmand.tatkal.exception.BadRequestException;
import com.sahilpanmand.tatkal.exception.ConflictException;
import com.sahilpanmand.tatkal.repository.BookingRepository;
import com.sahilpanmand.tatkal.repository.JourneyRepository;
import com.sahilpanmand.tatkal.repository.SeatRepository;
import com.sahilpanmand.tatkal.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock BookingRepository bookingRepository;
    @Mock SeatRepository seatRepository;
    @Mock JourneyRepository journeyRepository;
    @Mock UserRepository userRepository;
    @InjectMocks BookingService bookingService;

    private User user;
    private Journey journey;
    private Seat seat1;
    private Seat seat2;

    @BeforeEach
    void setUp() {
        user = new User("Sahil", "sahil@test.com", "hash", Role.USER);
        Train train = new Train("12124", "Deccan Queen", "Pune", "Mumbai",
                LocalTime.of(7, 15), LocalTime.of(10, 25), new BigDecimal("450.00"));
        journey = new Journey(train, LocalDate.now().plusDays(1));
        seat1 = new Seat(train, 1);
        seat2 = new Seat(train, 2);
    }

    @Test
    void booksTwoSeatsUnderOnePnr() {
        when(userRepository.findByEmail("sahil@test.com")).thenReturn(Optional.of(user));
        when(journeyRepository.findById(1L)).thenReturn(Optional.of(journey));
        when(seatRepository.findAndLockSeats(any(), any())).thenReturn(List.of(seat1, seat2));
        when(bookingRepository.findActiveBookings(any(), any())).thenReturn(List.of());

        BookingResponse response = bookingService.book("sahil@test.com", new BookingRequest(1L, List.of(10L, 11L)));

        assertThat(response.pnr()).hasSize(10);
        assertThat(response.seatNumbers()).containsExactly(1, 2);
        assertThat(response.totalFare()).isEqualByComparingTo("900.00");
        assertThat(response.status()).isEqualTo("CONFIRMED");
        verify(bookingRepository).saveAllAndFlush(any());
    }

    @Test
    void rejectsSeatThatIsAlreadyBooked() {
        when(userRepository.findByEmail("sahil@test.com")).thenReturn(Optional.of(user));
        when(journeyRepository.findById(1L)).thenReturn(Optional.of(journey));
        when(seatRepository.findAndLockSeats(any(), any())).thenReturn(List.of(seat1));
        Booking existing = new Booking("1234567890", user, journey, seat1, new BigDecimal("450.00"));
        when(bookingRepository.findActiveBookings(any(), any())).thenReturn(List.of(existing));

        assertThatThrownBy(() -> bookingService.book("sahil@test.com", new BookingRequest(1L, List.of(10L))))
                .isInstanceOf(ConflictException.class);
        verify(bookingRepository, never()).saveAllAndFlush(any());
    }

    @Test
    void rejectsMoreThanFourSeats() {
        BookingRequest request = new BookingRequest(1L, List.of(1L, 2L, 3L, 4L, 5L));

        assertThatThrownBy(() -> bookingService.book("sahil@test.com", request))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(seatRepository, bookingRepository);
    }

    @Test
    void cannotCancelAnotherUsersBooking() {
        User other = new User("Other", "other@test.com", "hash", Role.USER);
        Booking booking = new Booking("1234567890", other, journey, seat1, new BigDecimal("450.00"));
        when(bookingRepository.findByPnrOrderBySeatId("1234567890")).thenReturn(List.of(booking));

        assertThatThrownBy(() -> bookingService.cancel("sahil@test.com", "1234567890"))
                .isInstanceOf(com.sahilpanmand.tatkal.exception.ResourceNotFoundException.class);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }
}

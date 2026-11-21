package com.sahilpanmand.tatkal.controller;

import com.sahilpanmand.tatkal.dto.BookingRequest;
import com.sahilpanmand.tatkal.dto.BookingResponse;
import com.sahilpanmand.tatkal.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

// Needs a valid JWT. principal.getName() is the logged-in user's email (set by JwtFilter).
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse book(@Valid @RequestBody BookingRequest request, Principal principal) {
        return bookingService.book(principal.getName(), request);
    }

    @GetMapping("/my")
    public List<BookingResponse> myBookings(Principal principal) {
        return bookingService.myBookings(principal.getName());
    }

    @GetMapping("/{pnr}")
    public BookingResponse getByPnr(@PathVariable String pnr, Principal principal) {
        return bookingService.getByPnr(principal.getName(), pnr);
    }

    @PostMapping("/{pnr}/cancel")
    public BookingResponse cancel(@PathVariable String pnr, Principal principal) {
        return bookingService.cancel(principal.getName(), pnr);
    }
}

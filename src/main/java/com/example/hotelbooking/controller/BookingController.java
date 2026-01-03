package com.example.hotelbooking.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.hotelbooking.dto.booking.BookingDetailDTO;
import com.example.hotelbooking.dto.booking.BookingRequestDTO;
import com.example.hotelbooking.service.BookingService;
import com.example.hotelbooking.util.ApiResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BookingDetailDTO>> createBooking(
            @RequestBody BookingRequestDTO bookingRequestDTO) {

        BookingDetailDTO bookingDetailDTO = bookingService.createBooking(bookingRequestDTO);

        ApiResponse<BookingDetailDTO> response = new ApiResponse<>(true, "Booking created successfully",
                bookingDetailDTO);

        return ResponseEntity.ok(response);
    }

}

package com.example.hotelbooking.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.hotelbooking.dto.booking.BookingDetailDTO;
import com.example.hotelbooking.dto.booking.BookingRequestDTO;
import com.example.hotelbooking.dto.booking.BookingSummaryDTO;
import com.example.hotelbooking.enums.BookingStatusEnum;
import com.example.hotelbooking.security.CustomerUserDetails;
import com.example.hotelbooking.service.BookingService;
import com.example.hotelbooking.util.ApiResponse;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/bookings")
public class BookingController {

        final BookingService bookingService;

        public BookingController(BookingService bookingService) {
                this.bookingService = bookingService;
        }

        @PostMapping
        public ResponseEntity<ApiResponse<BookingDetailDTO>> createBooking(

                        @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
                        @RequestBody BookingRequestDTO bookingRequestDTO) {

                String username = customerUserDetails.getUsername();

                BookingDetailDTO bookingDetailDTO = bookingService.createBooking(username, bookingRequestDTO);

                ApiResponse<BookingDetailDTO> response = new ApiResponse<>(true, "Booking created successfully",
                                bookingDetailDTO);

                return ResponseEntity.ok(response);
        }

        @PreAuthorize("hasRole('HOST') or hasRole('CUSTOMER')")
        @GetMapping("/{bookingId}")
        public ResponseEntity<ApiResponse<BookingDetailDTO>> getBookingById(
                        @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
                        @PathVariable Long bookingId) {

                final String providerId = customerUserDetails.getProviderId();

                BookingDetailDTO bookingDetailDTO = bookingService.getBookingById(
                                providerId, bookingId);

                ApiResponse<BookingDetailDTO> response = new ApiResponse<>(true, "Booking retrieved successfully",
                                bookingDetailDTO);

                return ResponseEntity.ok(response);
        }

        @PreAuthorize("hasRole('HOST')")
        @GetMapping("/accommodation/{accommodationId}")
        public ResponseEntity<ApiResponse<List<BookingSummaryDTO>>> getBookingByAccommodationId(
                        @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
                        @PathVariable Long accommodationId,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {

                String providerId = customerUserDetails.getProviderId();

                List<BookingSummaryDTO> bookingSummaryDTO = bookingService.getBookingByAccommodationId(providerId,
                                accommodationId, page,
                                size);

                ApiResponse<List<BookingSummaryDTO>> response = new ApiResponse<>(true,
                                "Booking retrieved successfully",
                                bookingSummaryDTO);

                return ResponseEntity.ok(response);
        }

        @PreAuthorize("hasRole('HOST')")
        @PatchMapping("/{bookingId}/status")
        public ResponseEntity<ApiResponse<BookingDetailDTO>> updateBookingStatus(
                        @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
                        @PathVariable Long bookingId,
                        @RequestParam BookingStatusEnum status) {

                String providerId = customerUserDetails.getProviderId();

                BookingDetailDTO bookingDetailDTO = bookingService.updateBookingStatus(providerId,
                                bookingId, status);

                ApiResponse<BookingDetailDTO> response = new ApiResponse<>(true, "Booking status updated successfully",
                                bookingDetailDTO);

                return ResponseEntity.ok(response);
        }

        // Lấy các đơn hàng theo tháng của khách hàng
        @PreAuthorize("hasRole('CUSTOMER')")
        @GetMapping("/me")
        public ResponseEntity<ApiResponse<List<BookingSummaryDTO>>> getMyBookingsByMonth(
                        @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
                        @RequestParam(required = false) Integer day,
                        @RequestParam(required = false) Integer month,
                        @RequestParam(required = false) Integer year,
                        @RequestParam(required = false) BookingStatusEnum status,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {

                String username = customerUserDetails.getUsername();

                List<BookingSummaryDTO> bookingSummaryDTO = bookingService.getBookingsByCustomerAndMonth(
                                username, day, month, year, status, page, size);

                ApiResponse<List<BookingSummaryDTO>> response = new ApiResponse<>(true,
                                "Bookings retrieved successfully",
                                bookingSummaryDTO);

                return ResponseEntity.ok(response);
        }

}

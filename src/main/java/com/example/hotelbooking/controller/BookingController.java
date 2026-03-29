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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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

        @PatchMapping("/{bookingId}/cancel")
        public ResponseEntity<ApiResponse<BookingDetailDTO>> cancelBooking(
                        @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
                        @PathVariable Long bookingId) {

                String username = customerUserDetails.getUsername();

                BookingDetailDTO bookingDetailDTO = bookingService.cancelBookingByCustomer(username, bookingId);

                ApiResponse<BookingDetailDTO> response = new ApiResponse<>(true, "Booking cancelled successfully",
                                bookingDetailDTO);

                return ResponseEntity.ok(response);
        }

        // Các endpoint check booking của host

        @PreAuthorize("hasRole('HOST')")
        @GetMapping("/host/accommodation/{accommodationId}")
        public ResponseEntity<ApiResponse<List<BookingSummaryDTO>>> getBookingsForHostByAccommodation(
                        @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
                        @PathVariable Long accommodationId,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam BookingStatusEnum status) {

                String providerId = customerUserDetails.getProviderId();

                List<BookingSummaryDTO> bookingSummaryDTO = bookingService.getBookingsByAccommodationAndStatus(
                                providerId, accommodationId, status, page, size);

                ApiResponse<List<BookingSummaryDTO>> response = new ApiResponse<>(true,
                                "Bookings retrieved successfully",
                                bookingSummaryDTO);

                return ResponseEntity.ok(response);
        }

        // @PreAuthorize("hasRole('HOST')")
        // @PatchMapping("/host/{bookingId}/status")
        // public ResponseEntity<ApiResponse<BookingDetailDTO>>
        // updateBookingStatusByHost(
        // @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
        // @PathVariable Long bookingId,
        // @RequestParam BookingStatusEnum status) {

        // String providerId = customerUserDetails.getProviderId();

        // BookingDetailDTO bookingDetailDTO =
        // bookingService.updateBookingStatusByHost(providerId,
        // bookingId, status);

        // ApiResponse<BookingDetailDTO> response = new ApiResponse<>(true,
        // "Booking status updated successfully",
        // bookingDetailDTO);

        // return ResponseEntity.ok(response);
        // }

        // @PreAuthorize("hasRole('HOST')")

        // Các endpoint thống kê
        // Số lượng khách hôm nay
        @GetMapping("/host/{accommodationId}/today-guests")
        public ResponseEntity<ApiResponse<Long>> getTodayGuests(
                        @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
                        @PathVariable Long accommodationId) {

                String providerId = customerUserDetails.getProviderId();

                Long todayGuests = bookingService.getTodayGuests(
                                providerId, accommodationId);

                ApiResponse<Long> response = new ApiResponse<>(true,
                                "Today's guests retrieved successfully",
                                todayGuests);

                return ResponseEntity.ok(response);
        }

        // Số lượng khách hôm nay đã checkin

        @GetMapping("/host/{accommodationId}/today-checkins")
        public ResponseEntity<ApiResponse<Long>> getTodayCheckIns(
                        @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
                        @PathVariable Long accommodationId) {

                String providerId = customerUserDetails.getProviderId();

                Long todayCheckIns = bookingService.getTodayCheckIns(
                                providerId, accommodationId);

                ApiResponse<Long> response = new ApiResponse<>(true,
                                "Today's check-ins retrieved successfully",
                                todayCheckIns);

                return ResponseEntity.ok(response);
        }

        // Doanh thu hôm nay

        @GetMapping("/host/{accommodationId}/today-revenue")
        public ResponseEntity<ApiResponse<Double>> getTodayRevenue(
                        @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
                        @PathVariable Long accommodationId) {

                String providerId = customerUserDetails.getProviderId();

                Double todayRevenue = bookingService.getTodayRevenue(
                                providerId, accommodationId);

                ApiResponse<Double> response = new ApiResponse<>(true,
                                "Today's revenue retrieved successfully",
                                todayRevenue);

                return ResponseEntity.ok(response);
        }

        // Doanh thu tháng này

        @GetMapping("/host/{accommodationId}/month-revenue")
        public ResponseEntity<ApiResponse<Double>> getMonthRevenue(
                        @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
                        @PathVariable Long accommodationId) {

                String providerId = customerUserDetails.getProviderId();

                Double monthRevenue = bookingService.getMonthRevenue(
                                providerId, accommodationId);

                ApiResponse<Double> response = new ApiResponse<>(true,
                                "This month's revenue retrieved successfully",
                                monthRevenue);

                return ResponseEntity.ok(response);
        }

        // Các endpoint báo cáo doanh thu

        // Doanh thu theo ngày trong khoảng thời gian
        @GetMapping("/host/{accommodationId}/revenue")
        public ResponseEntity<ApiResponse<Double>> getRevenueInDateRange(
                        @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
                        @PathVariable Long accommodationId,
                        @RequestParam LocalDate startDate,
                        @RequestParam LocalDate endDate) {

                String providerId = customerUserDetails.getProviderId();

                Double revenue = bookingService.getRevenueInDateRange(
                                providerId, accommodationId, startDate, endDate);

                ApiResponse<Double> response = new ApiResponse<>(true,
                                "Revenue in date range retrieved successfully",
                                revenue);

                return ResponseEntity.ok(response);
        }

        // Doanh thu theo tháng
        @GetMapping("/host/{accommodationId}/monthly-revenue")
        public ResponseEntity<ApiResponse<List<Map<String, Double>>>> getMonthlyRevenue(
                        @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
                        @PathVariable Long accommodationId,
                        @RequestParam int year) {

                String providerId = customerUserDetails.getProviderId();

                List<Map<String, Double>> monthlyRevenue = bookingService.getMonthlyRevenue(
                                providerId, accommodationId, year);

                ApiResponse<List<Map<String, Double>>> response = new ApiResponse<>(true,
                                "Monthly revenue retrieved successfully",
                                monthlyRevenue);

                return ResponseEntity.ok(response);
        }

        // Doanh thu theo năm
        @GetMapping("/host/{accommodationId}/yearly-revenue")
        public ResponseEntity<ApiResponse<List<Map<String, Double>>>> getYearlyRevenue(
                        @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
                        @PathVariable Long accommodationId) {

                String providerId = customerUserDetails.getProviderId();

                List<Map<String, Double>> yearlyRevenue = bookingService.getYearlyRevenue(
                                providerId, accommodationId);

                ApiResponse<List<Map<String, Double>>> response = new ApiResponse<>(true,
                                "Yearly revenue retrieved successfully",
                                yearlyRevenue);

                return ResponseEntity.ok(response);
        }

        // thống kê statis
        @GetMapping("/host/{accommodationId}/statistics")
        public ResponseEntity<ApiResponse<Map<String, Object>>> getBookingStatistics(
                        @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
                        @PathVariable Long accommodationId,
                        @RequestParam LocalDate startDate,
                        @RequestParam LocalDate endDate) {

                String providerId = customerUserDetails.getProviderId();

                Map<String, Object> statistics = bookingService.getBookingStatistics(
                                providerId, accommodationId, startDate, endDate);

                ApiResponse<Map<String, Object>> response = new ApiResponse<>(true,
                                "Booking statistics retrieved successfully",
                                statistics);

                return ResponseEntity.ok(response);
        }

        // Doanh thu theo loại phòng
        // {
        // "roomTypeId": 1,
        // "roomTypeName": "Deluxe",
        // "totalBookings": 40,
        // "totalRevenue": 80000000.0,
        // "averagePrice": 2000000.0,
        // "occupancyRate": 85.0
        // }
        @GetMapping("/host/{accommodationId}/revenue-by-room-type")
        public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRevenueByRoomType(
                        @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
                        @PathVariable Long accommodationId,
                        @RequestParam LocalDate startDate,
                        @RequestParam LocalDate endDate) {

                String providerId = customerUserDetails.getProviderId();

                List<Map<String, Object>> revenueByRoomType = bookingService.getRevenueByRoomType(
                                providerId, accommodationId, startDate, endDate);

                ApiResponse<List<Map<String, Object>>> response = new ApiResponse<>(true,
                                "Revenue by room type retrieved successfully",
                                revenueByRoomType);

                return ResponseEntity.ok(response);
        }

        // 4 endpoint báo cáo theo khoảng thời gian
        // Tổng doanh thu
        @GetMapping("/host/{accommodationId}/report/total-revenue")
        public ResponseEntity<ApiResponse<Double>> getTotalRevenueInDateRange(
                        @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
                        @PathVariable Long accommodationId,
                        @RequestParam LocalDate startDate,
                        @RequestParam LocalDate endDate) {

                String providerId = customerUserDetails.getProviderId();

                Double totalRevenue = bookingService.getTotalRevenueInDateRange(
                                providerId, accommodationId, startDate, endDate);

                ApiResponse<Double> response = new ApiResponse<>(true,
                                "Total revenue retrieved successfully",
                                totalRevenue);

                return ResponseEntity.ok(response);
        }

        // Tổng phòng đặt
        @GetMapping("/host/{accommodationId}/report/total-bookings")
        public ResponseEntity<ApiResponse<Long>> getTotalBookingsInDateRange(
                        @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
                        @PathVariable Long accommodationId,
                        @RequestParam LocalDate startDate,
                        @RequestParam LocalDate endDate) {

                String providerId = customerUserDetails.getProviderId();

                Long totalBookings = bookingService.getTotalBookingsInDateRange(
                                providerId, accommodationId, startDate, endDate);

                ApiResponse<Long> response = new ApiResponse<>(true,
                                "Total bookings retrieved successfully",
                                totalBookings);

                return ResponseEntity.ok(response);
        }

        // Tổng phòng hủy
        @GetMapping("/host/{accommodationId}/report/total-canceled")
        public ResponseEntity<ApiResponse<Long>> getTotalCanceledBookingsInDateRange(
                        @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
                        @PathVariable Long accommodationId,
                        @RequestParam LocalDate startDate,
                        @RequestParam LocalDate endDate) {

                String providerId = customerUserDetails.getProviderId();

                Long totalCanceled = bookingService.getTotalCanceledBookingsInDateRange(
                                providerId, accommodationId, startDate, endDate);

                ApiResponse<Long> response = new ApiResponse<>(true,
                                "Total canceled bookings retrieved successfully",
                                totalCanceled);

                return ResponseEntity.ok(response);
        }

        // Tổng đêm ở
        @GetMapping("/host/{accommodationId}/report/total-nights")
        public ResponseEntity<ApiResponse<Long>> getTotalNightsInDateRange(
                        @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
                        @PathVariable Long accommodationId,
                        @RequestParam LocalDate startDate,
                        @RequestParam LocalDate endDate) {

                String providerId = customerUserDetails.getProviderId();

                Long totalNights = bookingService.getTotalNightsInDateRange(
                                providerId, accommodationId, startDate, endDate);

                ApiResponse<Long> response = new ApiResponse<>(true,
                                "Total nights retrieved successfully",
                                totalNights);

                return ResponseEntity.ok(response);
        }

}

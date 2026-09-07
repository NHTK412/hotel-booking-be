package com.example.hotelbooking.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.hotelbooking.dto.booking.BookingDetailDTO;
import com.example.hotelbooking.dto.booking.BookingRequestDTO;
import com.example.hotelbooking.dto.booking.BookingSummaryDTO;
import com.example.hotelbooking.enums.BookingStatusEnum;
import com.example.hotelbooking.security.CustomUserDetails;
import com.example.hotelbooking.service.BookingService;
import com.example.hotelbooking.util.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "4. Đặt Phòng & Báo Cáo Doanh Thu (Bookings & Revenue)", description = "Các API đặt phòng, quản lý trạng thái đơn đặt, hủy phòng và báo cáo thống kê doanh thu cho Host")
@RestController
@RequestMapping("/bookings")
public class BookingController {

        private final BookingService bookingService;

        public BookingController(BookingService bookingService) {
                this.bookingService = bookingService;
        }

        @Operation(summary = "Tạo đơn đặt phòng mới (Khách hàng)", description = "Khách hàng tạo đơn đặt phòng theo ngày Check-in/Check-out và thông tin liên hệ")
        @PostMapping
        public ResponseEntity<ApiResponse<BookingDetailDTO>> createBooking(
                        @AuthenticationPrincipal CustomUserDetails customerUserDetails,
                        @Valid @RequestBody BookingRequestDTO bookingRequestDTO) {

                String username = customerUserDetails.getUsername();
                BookingDetailDTO bookingDetailDTO = bookingService.createBooking(username, bookingRequestDTO);

                ApiResponse<BookingDetailDTO> response = new ApiResponse<>(true, "Đặt phòng thành công",
                                bookingDetailDTO);

                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Xem chi tiết đơn đặt phòng theo ID (Host & Khách hàng)", description = "Lấy thông tin chi tiết đơn đặt phòng bao gồm khách hàng, phòng đã đặt, giá tiền và trạng thái")
        @PreAuthorize("hasRole('HOST') or hasRole('CUSTOMER')")
        @GetMapping("/{bookingId}")
        public ResponseEntity<ApiResponse<BookingDetailDTO>> getBookingById(
                        @AuthenticationPrincipal CustomUserDetails customerUserDetails,
                        @PathVariable Long bookingId) {

                final String providerId = customerUserDetails.getProviderId();
                BookingDetailDTO bookingDetailDTO = bookingService.getBookingById(providerId, bookingId);

                ApiResponse<BookingDetailDTO> response = new ApiResponse<>(true, "Lấy thông tin đặt phòng thành công",
                                bookingDetailDTO);

                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Lấy danh sách đơn đặt theo Khách sạn (Chủ khách sạn - Host)", description = "Truy xuất danh sách tất cả các đơn đặt phòng của một khách sạn có phân trang")
        @PreAuthorize("hasRole('HOST')")
        @GetMapping("/accommodation/{accommodationId}")
        public ResponseEntity<ApiResponse<List<BookingSummaryDTO>>> getBookingByAccommodationId(
                        @AuthenticationPrincipal CustomUserDetails customerUserDetails,
                        @PathVariable Long accommodationId,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {

                String providerId = customerUserDetails.getProviderId();
                List<BookingSummaryDTO> bookingSummaryDTO = bookingService.getBookingByAccommodationId(providerId,
                                accommodationId, page, size);

                ApiResponse<List<BookingSummaryDTO>> response = new ApiResponse<>(true,
                                "Lấy danh sách đơn đặt phòng thành công",
                                bookingSummaryDTO);

                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Cập nhật trạng thái đơn đặt phòng (Host)", description = "Host cập nhật trạng thái đơn (Ví dụ: CHECKED_IN, CHECKED_OUT, CANCELED)")
        @PreAuthorize("hasRole('HOST')")
        @PatchMapping("/{bookingId}/status")
        public ResponseEntity<ApiResponse<BookingDetailDTO>> updateBookingStatus(
                        @AuthenticationPrincipal CustomUserDetails customerUserDetails,
                        @PathVariable Long bookingId,
                        @RequestParam BookingStatusEnum status) {

                String providerId = customerUserDetails.getProviderId();
                BookingDetailDTO bookingDetailDTO = bookingService.updateBookingStatus(providerId, bookingId, status);

                ApiResponse<BookingDetailDTO> response = new ApiResponse<>(true, "Cập nhật trạng thái thành công",
                                bookingDetailDTO);

                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Xem lịch sử đơn đặt phòng của tôi (Khách hàng)", description = "Khách hàng tra cứu lịch sử các đơn đặt phòng của mình kèm bộ lọc ngày, tháng, năm, trạng thái")
        @PreAuthorize("hasRole('CUSTOMER')")
        @GetMapping("/me")
        public ResponseEntity<ApiResponse<List<BookingSummaryDTO>>> getMyBookingsByMonth(
                        @AuthenticationPrincipal CustomUserDetails customerUserDetails,
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
                                "Lấy danh sách đơn đặt phòng thành công",
                                bookingSummaryDTO);

                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Hủy đơn đặt phòng (Khách hàng)", description = "Khách hàng thực hiện hủy phòng cho đơn đặt đang chờ hoặc đã thanh toán")
        @PatchMapping("/{bookingId}/cancel")
        public ResponseEntity<ApiResponse<BookingDetailDTO>> cancelBooking(
                        @AuthenticationPrincipal CustomUserDetails customerUserDetails,
                        @PathVariable Long bookingId) {

                String username = customerUserDetails.getUsername();
                BookingDetailDTO bookingDetailDTO = bookingService.cancelBookingByCustomer(username, bookingId);

                ApiResponse<BookingDetailDTO> response = new ApiResponse<>(true, "Hủy đơn đặt phòng thành công",
                                bookingDetailDTO);

                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Lọc danh sách đơn đặt theo trạng thái (Host)", description = "Lọc các đơn đặt phòng theo trạng thái cụ thể")
        @PreAuthorize("hasRole('HOST')")
        @GetMapping("/host/accommodation/{accommodationId}")
        public ResponseEntity<ApiResponse<List<BookingSummaryDTO>>> getBookingsForHostByAccommodation(
                        @AuthenticationPrincipal CustomUserDetails customerUserDetails,
                        @PathVariable Long accommodationId,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam BookingStatusEnum status) {

                String providerId = customerUserDetails.getProviderId();
                List<BookingSummaryDTO> bookingSummaryDTO = bookingService.getBookingsByAccommodationAndStatus(
                                providerId, accommodationId, status, page, size);

                ApiResponse<List<BookingSummaryDTO>> response = new ApiResponse<>(true,
                                "Lấy danh sách đơn đặt phòng thành công",
                                bookingSummaryDTO);

                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Số lượng khách đang lưu trú hôm nay (Host)", description = "Đếm tổng số lượt khách hiện đang ở tại khách sạn trong ngày hôm nay")
        @PreAuthorize("hasRole('HOST')")
        @GetMapping("/host/{accommodationId}/today-guests")
        public ResponseEntity<ApiResponse<Long>> getTodayGuests(
                        @AuthenticationPrincipal CustomUserDetails customerUserDetails,
                        @PathVariable Long accommodationId) {

                String providerId = customerUserDetails.getProviderId();
                Long todayGuests = bookingService.getTodayGuests(providerId, accommodationId);

                ApiResponse<Long> response = new ApiResponse<>(true,
                                "Lấy số lượng khách hôm nay thành công",
                                todayGuests);

                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Số lượt Check-in hôm nay (Host)", description = "Đếm số đơn đặt có lịch nhận phòng trong ngày hôm nay")
        @PreAuthorize("hasRole('HOST')")
        @GetMapping("/host/{accommodationId}/today-checkins")
        public ResponseEntity<ApiResponse<Long>> getTodayCheckIns(
                        @AuthenticationPrincipal CustomUserDetails customerUserDetails,
                        @PathVariable Long accommodationId) {

                String providerId = customerUserDetails.getProviderId();
                Long todayCheckIns = bookingService.getTodayCheckIns(providerId, accommodationId);

                ApiResponse<Long> response = new ApiResponse<>(true,
                                "Lấy số lượt Check-in hôm nay thành công",
                                todayCheckIns);

                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Doanh thu trong ngày hôm nay (Host)", description = "Tính tổng số tiền thu được từ các đơn đặt phòng trong ngày hôm nay")
        @PreAuthorize("hasRole('HOST')")
        @GetMapping("/host/{accommodationId}/today-revenue")
        public ResponseEntity<ApiResponse<Double>> getTodayRevenue(
                        @AuthenticationPrincipal CustomUserDetails customerUserDetails,
                        @PathVariable Long accommodationId) {

                String providerId = customerUserDetails.getProviderId();
                Double todayRevenue = bookingService.getTodayRevenue(providerId, accommodationId);

                ApiResponse<Double> response = new ApiResponse<>(true,
                                "Lấy doanh thu hôm nay thành công",
                                todayRevenue);

                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Doanh thu trong tháng hiện tại (Host)", description = "Tính tổng doanh thu thu được trong tháng hiện tại")
        @PreAuthorize("hasRole('HOST')")
        @GetMapping("/host/{accommodationId}/month-revenue")
        public ResponseEntity<ApiResponse<Double>> getMonthRevenue(
                        @AuthenticationPrincipal CustomUserDetails customerUserDetails,
                        @PathVariable Long accommodationId) {

                String providerId = customerUserDetails.getProviderId();
                Double monthRevenue = bookingService.getMonthRevenue(providerId, accommodationId);

                ApiResponse<Double> response = new ApiResponse<>(true,
                                "Lấy doanh thu tháng thành công",
                                monthRevenue);

                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Doanh thu trong khoảng thời gian tùy chọn (Host)", description = "Tính tổng doanh thu giữa ngày bắt đầu và ngày kết thúc")
        @PreAuthorize("hasRole('HOST')")
        @GetMapping("/host/{accommodationId}/revenue")
        public ResponseEntity<ApiResponse<Double>> getRevenueInDateRange(
                        @AuthenticationPrincipal CustomUserDetails customerUserDetails,
                        @PathVariable Long accommodationId,
                        @RequestParam LocalDate startDate,
                        @RequestParam LocalDate endDate) {

                String providerId = customerUserDetails.getProviderId();
                Double revenue = bookingService.getRevenueInDateRange(
                                providerId, accommodationId, startDate, endDate);

                ApiResponse<Double> response = new ApiResponse<>(true,
                                "Lấy doanh thu theo khoảng thời gian thành công",
                                revenue);

                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Biểu đồ doanh thu 12 tháng trong năm (Host)", description = "Thống kê doanh thu chi tiết từng tháng (Tháng 1 -> Tháng 12) của năm được chọn")
        @PreAuthorize("hasRole('HOST')")
        @GetMapping("/host/{accommodationId}/monthly-revenue")
        public ResponseEntity<ApiResponse<List<Map<String, Double>>>> getMonthlyRevenue(
                        @AuthenticationPrincipal CustomUserDetails customerUserDetails,
                        @PathVariable Long accommodationId,
                        @RequestParam int year) {

                String providerId = customerUserDetails.getProviderId();
                List<Map<String, Double>> monthlyRevenue = bookingService.getMonthlyRevenue(
                                providerId, accommodationId, year);

                ApiResponse<List<Map<String, Double>>> response = new ApiResponse<>(true,
                                "Lấy doanh thu các tháng thành công",
                                monthlyRevenue);

                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Thống kê so sánh doanh thu các năm (Host)", description = "Thống kê doanh thu phân chia theo từng năm")
        @PreAuthorize("hasRole('HOST')")
        @GetMapping("/host/{accommodationId}/yearly-revenue")
        public ResponseEntity<ApiResponse<List<Map<String, Double>>>> getYearlyRevenue(
                        @AuthenticationPrincipal CustomUserDetails customerUserDetails,
                        @PathVariable Long accommodationId) {

                String providerId = customerUserDetails.getProviderId();
                List<Map<String, Double>> yearlyRevenue = bookingService.getYearlyRevenue(
                                providerId, accommodationId);

                ApiResponse<List<Map<String, Double>>> response = new ApiResponse<>(true,
                                "Lấy doanh thu các năm thành công",
                                yearlyRevenue);

                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Báo cáo tổng quan chỉ số kinh doanh (Host)", description = "Thống kê số đơn hoàn thành, số đơn hủy, tổng doanh thu và giá trị trung bình mỗi đơn")
        @PreAuthorize("hasRole('HOST')")
        @GetMapping("/host/{accommodationId}/statistics")
        public ResponseEntity<ApiResponse<Map<String, Object>>> getBookingStatistics(
                        @AuthenticationPrincipal CustomUserDetails customerUserDetails,
                        @PathVariable Long accommodationId,
                        @RequestParam LocalDate startDate,
                        @RequestParam LocalDate endDate) {

                String providerId = customerUserDetails.getProviderId();
                Map<String, Object> statistics = bookingService.getBookingStatistics(
                                providerId, accommodationId, startDate, endDate);

                ApiResponse<Map<String, Object>> response = new ApiResponse<>(true,
                                "Lấy dữ liệu thống kê tổng quan thành công",
                                statistics);

                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Phân tích doanh thu theo từng loại phòng (Host)", description = "Báo cáo đóng góp doanh thu và số lượng đơn đặt của từng loại phòng")
        @PreAuthorize("hasRole('HOST')")
        @GetMapping("/host/{accommodationId}/revenue-by-room-type")
        public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRevenueByRoomType(
                        @AuthenticationPrincipal CustomUserDetails customerUserDetails,
                        @PathVariable Long accommodationId,
                        @RequestParam LocalDate startDate,
                        @RequestParam LocalDate endDate) {

                String providerId = customerUserDetails.getProviderId();
                List<Map<String, Object>> revenueByRoomType = bookingService.getRevenueByRoomType(
                                providerId, accommodationId, startDate, endDate);

                ApiResponse<List<Map<String, Object>>> response = new ApiResponse<>(true,
                                "Lấy doanh thu theo loại phòng thành công",
                                revenueByRoomType);

                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Báo cáo tổng doanh thu (Host)", description = "Lấy tổng doanh thu trong khoảng thời gian")
        @PreAuthorize("hasRole('HOST')")
        @GetMapping("/host/{accommodationId}/report/total-revenue")
        public ResponseEntity<ApiResponse<Double>> getTotalRevenueInDateRange(
                        @AuthenticationPrincipal CustomUserDetails customerUserDetails,
                        @PathVariable Long accommodationId,
                        @RequestParam LocalDate startDate,
                        @RequestParam LocalDate endDate) {

                String providerId = customerUserDetails.getProviderId();
                Double totalRevenue = bookingService.getTotalRevenueInDateRange(
                                providerId, accommodationId, startDate, endDate);

                ApiResponse<Double> response = new ApiResponse<>(true,
                                "Lấy tổng doanh thu thành công",
                                totalRevenue);

                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Báo cáo tổng số lượng đơn đặt (Host)", description = "Lấy tổng số lượng đơn đặt trong khoảng thời gian")
        @PreAuthorize("hasRole('HOST')")
        @GetMapping("/host/{accommodationId}/report/total-bookings")
        public ResponseEntity<ApiResponse<Long>> getTotalBookingsInDateRange(
                        @AuthenticationPrincipal CustomUserDetails customerUserDetails,
                        @PathVariable Long accommodationId,
                        @RequestParam LocalDate startDate,
                        @RequestParam LocalDate endDate) {

                String providerId = customerUserDetails.getProviderId();
                Long totalBookings = bookingService.getTotalBookingsInDateRange(
                                providerId, accommodationId, startDate, endDate);

                ApiResponse<Long> response = new ApiResponse<>(true,
                                "Lấy tổng số lượng đơn đặt thành công",
                                totalBookings);

                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Báo cáo tổng số đơn đặt đã hủy (Host)", description = "Lấy tổng số đơn đặt phòng bị hủy trong khoảng thời gian")
        @PreAuthorize("hasRole('HOST')")
        @GetMapping("/host/{accommodationId}/report/total-canceled")
        public ResponseEntity<ApiResponse<Long>> getTotalCanceledBookingsInDateRange(
                        @AuthenticationPrincipal CustomUserDetails customerUserDetails,
                        @PathVariable Long accommodationId,
                        @RequestParam LocalDate startDate,
                        @RequestParam LocalDate endDate) {

                String providerId = customerUserDetails.getProviderId();
                Long totalCanceled = bookingService.getTotalCanceledBookingsInDateRange(
                                providerId, accommodationId, startDate, endDate);

                ApiResponse<Long> response = new ApiResponse<>(true,
                                "Lấy tổng số đơn bị hủy thành công",
                                totalCanceled);

                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Báo cáo tổng số đêm lưu trú (Host)", description = "Tính tổng số đêm khách ở tại khách sạn trong khoảng thời gian")
        @PreAuthorize("hasRole('HOST')")
        @GetMapping("/host/{accommodationId}/report/total-nights")
        public ResponseEntity<ApiResponse<Long>> getTotalNightsInDateRange(
                        @AuthenticationPrincipal CustomUserDetails customerUserDetails,
                        @PathVariable Long accommodationId,
                        @RequestParam LocalDate startDate,
                        @RequestParam LocalDate endDate) {

                String providerId = customerUserDetails.getProviderId();
                Long totalNights = bookingService.getTotalNightsInDateRange(
                                providerId, accommodationId, startDate, endDate);

                ApiResponse<Long> response = new ApiResponse<>(true,
                                "Lấy tổng số đêm lưu trú thành công",
                                totalNights);

                return ResponseEntity.ok(response);
        }
}

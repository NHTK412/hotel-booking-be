package com.example.hotelbooking.controller;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.hotelbooking.dto.accommodation.AccommodationDetailDTO;
import com.example.hotelbooking.dto.accommodation.AccommodationRequestDTO;
import com.example.hotelbooking.dto.accommodation.AccommodationSummaryDTO;
import com.example.hotelbooking.enums.AccommodationTypeEnum;
import com.example.hotelbooking.security.CustomUserDetails;
import com.example.hotelbooking.service.AccommodationService;
import com.example.hotelbooking.util.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "2. Khách Sạn & Chỗ Nghỉ (Accommodations)", description = "Các API tra cứu danh sách chỗ nghỉ, xem chi tiết phòng, tìm kiếm khách sạn gần nhất và quản lý khách sạn")
@RestController
@RequestMapping("/accommodations")
public class AccommodationController {

        private final AccommodationService accommodationService;

        public AccommodationController(AccommodationService accommodationService) {
                this.accommodationService = accommodationService;
        }

        @Operation(summary = "Lấy danh sách khách sạn (Công khai)", description = "Truy xuất danh sách khách sạn có phân trang, lọc theo loại hình (Khách sạn, Resort, Villa...), địa điểm và đánh giá sao")
        @GetMapping
        public ResponseEntity<ApiResponse<List<AccommodationSummaryDTO>>> getAllAccommodations(
                        @RequestParam(defaultValue = "0") Integer page,
                        @RequestParam(defaultValue = "10") Integer size,
                        @RequestParam(required = false) AccommodationTypeEnum type,
                        @RequestParam(required = false) Long locationId,
                        @RequestParam(required = false) Boolean sortBy) {

                List<AccommodationSummaryDTO> accommodationSummaryDTOs = accommodationService
                                .getAllAccommodation(PageRequest.of(page, size), type, locationId, sortBy);

                ApiResponse<List<AccommodationSummaryDTO>> response = new ApiResponse<>(true,
                                "Lấy danh sách khách sạn thành công",
                                accommodationSummaryDTOs);

                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Xem chi tiết khách sạn (Công khai / Khách hàng)", description = "Lấy thông tin chi tiết của một khách sạn theo ID bao gồm các loại phòng, tiện nghi, hình ảnh và trạng thái yêu thích")
        @GetMapping("/{accommodationId}")
        public ResponseEntity<ApiResponse<AccommodationDetailDTO>> getAccommodationById(
                        @AuthenticationPrincipal CustomUserDetails userDetails,
                        @PathVariable Long accommodationId) {

                final String providerId = userDetails != null ? userDetails.getUsername() : null;

                AccommodationDetailDTO accommodationDetailDTO = accommodationService
                                .getAccommodationById(providerId, accommodationId);

                ApiResponse<AccommodationDetailDTO> response = new ApiResponse<>(true,
                                "Lấy thông tin khách sạn thành công",
                                accommodationDetailDTO);

                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Thêm mới khách sạn (Chỉ dành cho Admin)", description = "Tạo mới một khách sạn/chỗ nghỉ trong hệ thống")
        @PreAuthorize("hasRole('ADMIN')")
        @PostMapping
        public ResponseEntity<ApiResponse<AccommodationDetailDTO>> createAccommodation(
                        @Valid @RequestBody AccommodationRequestDTO accommodationRequestDTO) {

                AccommodationDetailDTO createdAccommodation = accommodationService
                                .createAccommodation(accommodationRequestDTO);

                ApiResponse<AccommodationDetailDTO> response = new ApiResponse<>(true,
                                "Tạo mới khách sạn thành công",
                                createdAccommodation);

                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Khóa / Xóa mềm khách sạn (Chỉ dành cho Admin)", description = "Khóa hoặc xóa mềm khách sạn theo ID")
        @PreAuthorize("hasRole('ADMIN')")
        @DeleteMapping("/{accommodationId}")
        public ResponseEntity<ApiResponse<AccommodationDetailDTO>> deleteAccommodation(
                        @PathVariable Long accommodationId) {

                AccommodationDetailDTO deletedAccommodation = accommodationService
                                .deleteAccommodation(accommodationId);

                ApiResponse<AccommodationDetailDTO> response = new ApiResponse<>(true,
                                "Khóa khách sạn thành công",
                                deletedAccommodation);

                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Cập nhật thông tin khách sạn (Chỉ dành cho Host)", description = "Chỉnh sửa tên, mô tả, địa chỉ, hình ảnh và tiện ích của khách sạn do Host quản lý")
        @PreAuthorize("hasRole('HOST')")
        @PutMapping("/{accommodationId}")
        public ResponseEntity<ApiResponse<AccommodationDetailDTO>> updateAccommodation(
                        @AuthenticationPrincipal CustomUserDetails userDetails,
                        @PathVariable Long accommodationId,
                        @Valid @RequestBody AccommodationRequestDTO accommodationRequestDTO) {

                final String providerId = userDetails != null ? userDetails.getUsername() : null;

                AccommodationDetailDTO updatedAccommodation = accommodationService
                                .updateAccommodation(providerId, accommodationId, accommodationRequestDTO);

                ApiResponse<AccommodationDetailDTO> response = new ApiResponse<>(true,
                                "Cập nhật khách sạn thành công",
                                updatedAccommodation);

                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Lấy danh sách khách sạn yêu thích (Khách hàng)", description = "Xem danh sách các khách sạn mà khách hàng hiện tại đã đánh dấu yêu thích")
        @PreAuthorize("hasAnyRole('CUSTOMER')")
        @GetMapping("/favorite")
        public ResponseEntity<ApiResponse<List<AccommodationSummaryDTO>>> getAllByFavorite(
                        @AuthenticationPrincipal CustomUserDetails customerUserDetails,
                        @RequestParam(defaultValue = "0") Integer page,
                        @RequestParam(defaultValue = "10") Integer size) {

                String providerId = customerUserDetails.getUsername();

                List<AccommodationSummaryDTO> accommodationSummaryDTOs = accommodationService
                                .getAllByFavorite(PageRequest.of(page, size), providerId);

                ApiResponse<List<AccommodationSummaryDTO>> response = new ApiResponse<>(true,
                                "Lấy danh sách yêu thích thành công",
                                accommodationSummaryDTOs);

                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Thêm/Hủy đánh dấu khách sạn yêu thích (Khách hàng)", description = "Bật hoặc tắt trạng thái yêu thích cho một khách sạn cụ thể")
        @PreAuthorize("hasAnyRole('CUSTOMER')")
        @PutMapping("/favorite/{accommodationId}")
        public ResponseEntity<ApiResponse<AccommodationDetailDTO>> updateFavoriteAccommodation(
                        @AuthenticationPrincipal CustomUserDetails customerUserDetails,
                        @PathVariable Long accommodationId,
                        @RequestParam(required = true, defaultValue = "false") Boolean isFavorite) {

                final String providerId = customerUserDetails.getUsername();

                AccommodationDetailDTO updatedAccommodation = accommodationService
                                .updateFavoriteAccommodation(providerId, accommodationId, isFavorite);

                ApiResponse<AccommodationDetailDTO> response = new ApiResponse<>(true,
                                "Cập nhật trạng thái yêu thích thành công",
                                updatedAccommodation);

                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Tìm khách sạn gần đây theo tọa độ (Công khai)", description = "Tìm kiếm các chỗ nghỉ gần vị trí người dùng bằng thuật toán GeoHash dựa trên Vĩ độ (Latitude) và Kinh độ (Longitude)")
        @GetMapping("/nearby")
        public ResponseEntity<ApiResponse<List<AccommodationSummaryDTO>>> getNearbyAccommodations(
                        @RequestParam Double latitude,
                        @RequestParam Double longitude,
                        @RequestParam(required = false, defaultValue = "5") Integer precision,
                        @RequestParam(required = false) String type) {

                List<AccommodationSummaryDTO> nearbyAccommodations = accommodationService
                                .findNearbyAccommodations(latitude, longitude, precision, type);

                ApiResponse<List<AccommodationSummaryDTO>> response = new ApiResponse<>(true,
                                "Tìm kiếm khách sạn gần nhất thành công",
                                nearbyAccommodations);

                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Tìm kiếm khách sạn theo từ khóa (Công khai)", description = "Tìm kiếm khách sạn theo tên hoặc mô tả có phân trang")
        @GetMapping("/search")
        public ResponseEntity<ApiResponse<List<AccommodationSummaryDTO>>> searchAccommodations(
                        @RequestParam String keyword,
                        @RequestParam(defaultValue = "0") Integer page,
                        @RequestParam(defaultValue = "10") Integer size) {

                List<AccommodationSummaryDTO> accommodationSummaryDTOs = accommodationService
                                .searchAccommodations(keyword, PageRequest.of(page, size));

                ApiResponse<List<AccommodationSummaryDTO>> response = new ApiResponse<>(true,
                                "Tìm kiếm khách sạn thành công",
                                accommodationSummaryDTOs);

                return ResponseEntity.ok(response);
        }
}

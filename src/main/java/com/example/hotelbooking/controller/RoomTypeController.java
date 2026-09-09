package com.example.hotelbooking.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.hotelbooking.dto.room.RoomRequestDTO;
import com.example.hotelbooking.dto.room.RoomSummaryDTO;
import com.example.hotelbooking.dto.room.UpdateRoomDTO;
import com.example.hotelbooking.dto.roomtype.RoomTypeDetailDTO;
import com.example.hotelbooking.enums.StatusEnum;
import com.example.hotelbooking.dto.roomtype.RoomTypeRequestDTO;
import com.example.hotelbooking.dto.roomtype.RoomTypeSummaryDTO;
import com.example.hotelbooking.security.CustomUserDetails;
import com.example.hotelbooking.service.RoomTypeService;
import com.example.hotelbooking.util.ApiResponse;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "3. Loại Phòng & Quản Lý Phòng (Room Types)", description = "Các API tìm kiếm phòng trống theo ngày, thông tin loại phòng, giá cả, chiết khấu và quản lý phòng vật lý")
@RestController
@RequestMapping("/room-types")
public class RoomTypeController {

        private final RoomTypeService roomTypeService;

        public RoomTypeController(RoomTypeService roomTypeService) {
                this.roomTypeService = roomTypeService;
        }

        @Operation(summary = "Xem chi tiết loại phòng (Công khai)", description = "Lấy thông tin đầy đủ của loại phòng bao gồm giá, sức chứa, số giường và các tiện nghi phòng")
        @GetMapping("/{roomTypeId}")
        public ResponseEntity<ApiResponse<RoomTypeDetailDTO>> getRoomTypeById(@PathVariable Long roomTypeId) {
                RoomTypeDetailDTO updatedRoomType = roomTypeService.getRoomTypeById(roomTypeId);

                ApiResponse<RoomTypeDetailDTO> response = new ApiResponse<>(true,
                                "Lấy thông tin loại phòng thành công",
                                updatedRoomType);

                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Lấy danh sách các phòng vật lý thuộc loại phòng", description = "Xem danh sách các mã phòng thực tế thuộc về loại phòng này (hỗ trợ lọc phòng đã xóa isDeleted=true/false)")
        @GetMapping("/{roomTypeId}/rooms")
        public ResponseEntity<ApiResponse<List<RoomSummaryDTO>>> getRoomsByRoomType(
                        @PathVariable Long roomTypeId,
                        @RequestParam(required = false, defaultValue = "false") Boolean isDeleted) {
                List<RoomSummaryDTO> rooms = roomTypeService.getRoomsByRoomType(roomTypeId, isDeleted);

                ApiResponse<List<RoomSummaryDTO>> response = new ApiResponse<>(true,
                                "Lấy danh sách phòng thành công",
                                rooms);

                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Tìm kiếm phòng còn trống theo ngày (Công khai)", description = "Tìm kiếm loại phòng theo địa điểm, ngày Check-in/Check-out, số lượng khách và số phòng ngủ")
        @GetMapping("/search")
        public ResponseEntity<ApiResponse<List<RoomTypeSummaryDTO>>> getAllRoomTypes(
                        @RequestParam(required = false) Long locationId,
                        @RequestParam(required = false) String checkInDate,
                        @RequestParam(required = false) String checkOutDate,
                        @RequestParam(required = false) Integer capacity,
                        @RequestParam(required = false) Integer bedroom,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {

                Pageable pageable = PageRequest.of(page, size);

                LocalDate parsedCheckInDate = parseDateOrNull(checkInDate);
                LocalDate parsedCheckOutDate = parseDateOrNull(checkOutDate);

                List<RoomTypeSummaryDTO> roomTypes = roomTypeService.getAllRoomTypes(
                                locationId,
                                parsedCheckInDate,
                                parsedCheckOutDate,
                                capacity,
                                bedroom,
                                pageable);

                ApiResponse<List<RoomTypeSummaryDTO>> response = new ApiResponse<>(true,
                                "Tìm kiếm loại phòng thành công",
                                roomTypes);

                return ResponseEntity.ok(response);
        }

        private LocalDate parseDateOrNull(String value) {
                return value == null || value.isBlank() ? null : LocalDate.parse(value);
        }

        @Operation(summary = "Lấy danh sách loại phòng của một khách sạn (Công khai)", description = "Xem toàn bộ các loại phòng mà khách sạn đang cung cấp")
        @GetMapping("/accommodations/{accommodationId}")
        public ResponseEntity<ApiResponse<List<RoomTypeSummaryDTO>>> getRoomTypesByAccommodation(
                        @PathVariable Long accommodationId,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {

                Pageable pageable = PageRequest.of(page, size);

                List<RoomTypeSummaryDTO> roomTypes = roomTypeService.getRoomTypesByAccommodation(
                                accommodationId,
                                pageable);

                ApiResponse<List<RoomTypeSummaryDTO>> response = new ApiResponse<>(true,
                                "Lấy danh sách loại phòng thành công",
                                roomTypes);

                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Lấy danh sách loại phòng của Host (Chọn cơ sở hoặc tất cả, hỗ trợ lọc đã xóa)", description = "Host xem danh sách loại phòng theo cơ sở được chọn (truyền accommodationId) hoặc toàn bộ cơ sở Host quản lý (nếu không truyền). Truyền isDeleted=true để xem danh sách loại phòng đã bị xóa.")
        @PreAuthorize("hasAnyRole('HOST')")
        @GetMapping("/host")
        public ResponseEntity<ApiResponse<List<RoomTypeSummaryDTO>>> getHostRoomTypes(
                        @AuthenticationPrincipal CustomUserDetails customerUserDetails,
                        @RequestParam(required = false) Long accommodationId,
                        @RequestParam(required = false, defaultValue = "false") Boolean isDeleted,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {
                Pageable pageable = PageRequest.of(page, size);
                List<RoomTypeSummaryDTO> roomTypes = roomTypeService.getHostRoomTypes(
                                customerUserDetails.getProviderId(),
                                accommodationId,
                                isDeleted,
                                pageable);

                ApiResponse<List<RoomTypeSummaryDTO>> response = new ApiResponse<>(true,
                                "Lấy danh sách loại phòng của Host thành công",
                                roomTypes);

                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Tạo loại phòng mới (Chủ khách sạn - Host)", description = "Thêm mới một danh mục loại phòng kèm giá, số người tối đa và tiện ích")
        @PreAuthorize("hasAnyRole('HOST')")
        @PostMapping
        public ResponseEntity<ApiResponse<RoomTypeDetailDTO>> createRoomType(
                        @AuthenticationPrincipal CustomUserDetails customerUserDetails,
                        @Valid @RequestBody RoomTypeRequestDTO roomTypeRequestDTO) {
                RoomTypeDetailDTO createdRoomType = roomTypeService.createRoomType(
                                customerUserDetails.getProviderId(),
                                roomTypeRequestDTO);

                ApiResponse<RoomTypeDetailDTO> response = new ApiResponse<>(true,
                                "Tạo loại phòng thành công",
                                createdRoomType);

                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Cập nhật loại phòng (Chủ khách sạn - Host)", description = "Chỉnh sửa thông số, tiện nghi và hình ảnh của loại phòng")
        @PreAuthorize("hasAnyRole('HOST')")
        @PutMapping("/{roomTypeId}")
        public ResponseEntity<ApiResponse<RoomTypeDetailDTO>> updateRoomType(
                        @PathVariable Long roomTypeId,
                        @AuthenticationPrincipal CustomUserDetails customerUserDetails,
                        @Valid @RequestBody RoomTypeRequestDTO roomTypeRequestDTO) {
                RoomTypeDetailDTO updatedRoomType = roomTypeService
                                .updateRoomType(customerUserDetails.getProviderId(), roomTypeId, roomTypeRequestDTO);

                ApiResponse<RoomTypeDetailDTO> response = new ApiResponse<>(true,
                                "Cập nhật loại phòng thành công",
                                updatedRoomType);

                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Cập nhật nhanh Giá & Giảm giá (Host)", description = "Cập nhật giá niêm yết và tỷ lệ chiết khấu cho loại phòng")
        @PreAuthorize("hasAnyRole('HOST')")
        @PatchMapping("/{roomTypeId}")
        public ResponseEntity<ApiResponse<RoomTypeDetailDTO>> patchRoomType(
                        @PathVariable Long roomTypeId,
                        @AuthenticationPrincipal CustomUserDetails customerUserDetails,
                        @RequestParam Double price,
                        @RequestParam Double discount) {
                RoomTypeDetailDTO updatedRoomType = roomTypeService
                                .patchRoomType(customerUserDetails.getProviderId(), roomTypeId, price, discount);
                ApiResponse<RoomTypeDetailDTO> response = new ApiResponse<>(true,
                                "Cập nhật giá và chiết khấu thành công",
                                updatedRoomType);

                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Xóa loại phòng (Chủ khách sạn - Host)", description = "Xóa loại phòng khỏi hệ thống")
        @PreAuthorize("hasAnyRole('HOST')")
        @DeleteMapping("/{roomTypeId}")
        public ResponseEntity<ApiResponse<RoomTypeDetailDTO>> deleteRoomType(
                        @PathVariable Long roomTypeId,
                        @AuthenticationPrincipal CustomUserDetails customerUserDetails) {
                RoomTypeDetailDTO deletedRoomType = roomTypeService
                                .deleteRoomType(customerUserDetails.getProviderId(), roomTypeId);

                ApiResponse<RoomTypeDetailDTO> response = new ApiResponse<>(true,
                                "Xóa loại phòng thành công",
                                deletedRoomType);
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Khôi phục loại phòng đã xóa (Host)", description = "Khôi phục loại phòng từ trạng thái đã xóa mềm về hoạt động bình thường")
        @PreAuthorize("hasAnyRole('HOST')")
        @PatchMapping("/{roomTypeId}/restore")
        public ResponseEntity<ApiResponse<RoomTypeDetailDTO>> restoreRoomType(
                        @PathVariable Long roomTypeId,
                        @AuthenticationPrincipal CustomUserDetails customerUserDetails) {
                RoomTypeDetailDTO restoredRoomType = roomTypeService
                                .restoreRoomType(customerUserDetails.getProviderId(), roomTypeId);

                ApiResponse<RoomTypeDetailDTO> response = new ApiResponse<>(true,
                                "Khôi phục loại phòng thành công",
                                restoredRoomType);
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Cập nhật trạng thái hoạt động loại phòng (Host)", description = "Chuyển đổi trạng thái ACTIVE (hoạt động bình thường) hoặc INACTIVE (tạm ngưng nhận khách, khách chỉ xem không đặt được)")
        @PreAuthorize("hasAnyRole('HOST')")
        @PatchMapping("/{roomTypeId}/status")
        public ResponseEntity<ApiResponse<RoomTypeDetailDTO>> updateRoomTypeStatus(
                        @PathVariable Long roomTypeId,
                        @RequestParam StatusEnum status,
                        @AuthenticationPrincipal CustomUserDetails customerUserDetails) {
                RoomTypeDetailDTO updatedRoomType = roomTypeService
                                .updateRoomTypeStatus(customerUserDetails.getProviderId(), roomTypeId, status);

                ApiResponse<RoomTypeDetailDTO> response = new ApiResponse<>(true,
                                "Cập nhật trạng thái loại phòng thành công",
                                updatedRoomType);
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Thêm phòng vật lý vào loại phòng (Host)", description = "Khởi tạo các số phòng vật lý (Ví dụ: Phòng 101, 102...) gán vào loại phòng")
        @PreAuthorize("hasAnyRole('HOST')")
        @PostMapping("/{roomTypeId}/rooms")
        public ResponseEntity<ApiResponse<List<RoomSummaryDTO>>> addRoomsToRoomType(
                        @PathVariable Long roomTypeId,
                        @AuthenticationPrincipal CustomUserDetails customerUserDetails,
                        @Valid @RequestBody RoomRequestDTO roomRequestDTO) {
                List<RoomSummaryDTO> addedRooms = roomTypeService.addRoomsToRoomType(
                                customerUserDetails.getProviderId(),
                                roomTypeId,
                                roomRequestDTO);

                ApiResponse<List<RoomSummaryDTO>> response = new ApiResponse<>(true,
                                "Thêm phòng vật lý thành công",
                                addedRooms);
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Cập nhật phòng vật lý (Host)", description = "Chỉnh sửa tên/số phòng và trạng thái hoạt động của phòng vật lý")
        @PreAuthorize("hasAnyRole('HOST')")
        @PutMapping("/{roomTypeId}/rooms/{roomId}")
        public ResponseEntity<ApiResponse<RoomSummaryDTO>> updateRoom(
                        @PathVariable Long roomTypeId,
                        @PathVariable Long roomId,
                        @AuthenticationPrincipal CustomUserDetails customerUserDetails,
                        @Valid @RequestBody UpdateRoomDTO updateRoomDTO) {
                RoomSummaryDTO updatedRoom = roomTypeService.updateRoom(
                                customerUserDetails.getProviderId(),
                                roomTypeId,
                                roomId,
                                updateRoomDTO);

                ApiResponse<RoomSummaryDTO> response = new ApiResponse<>(true,
                                "Cập nhật phòng vật lý thành công",
                                updatedRoom);
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Xóa phòng vật lý (Host)", description = "Xóa danh sách các phòng theo ID phòng")
        @PreAuthorize("hasAnyRole('HOST')")
        @DeleteMapping("/{roomTypeId}/rooms")
        public ResponseEntity<ApiResponse<List<RoomSummaryDTO>>> deleteRoomsFromRoomType(
                        @PathVariable Long roomTypeId,
                        @AuthenticationPrincipal CustomUserDetails customerUserDetails,
                        @RequestBody List<Long> roomIds) {
                List<RoomSummaryDTO> deletedRooms = roomTypeService.deleteRoomsFromRoomType(
                                customerUserDetails.getProviderId(),
                                roomTypeId,
                                roomIds);

                ApiResponse<List<RoomSummaryDTO>> response = new ApiResponse<>(true,
                                "Xóa phòng vật lý thành công",
                                deletedRooms);
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Khôi phục phòng vật lý đã xóa (Host)", description = "Khôi phục danh sách các phòng vật lý theo ID phòng từ thùng rác về hoạt động")
        @PreAuthorize("hasAnyRole('HOST')")
        @PatchMapping("/{roomTypeId}/rooms/restore")
        public ResponseEntity<ApiResponse<List<RoomSummaryDTO>>> restoreRoomsFromRoomType(
                        @PathVariable Long roomTypeId,
                        @AuthenticationPrincipal CustomUserDetails customerUserDetails,
                        @RequestBody List<Long> roomIds) {
                List<RoomSummaryDTO> restoredRooms = roomTypeService.restoreRoomsFromRoomType(
                                customerUserDetails.getProviderId(),
                                roomTypeId,
                                roomIds);

                ApiResponse<List<RoomSummaryDTO>> response = new ApiResponse<>(true,
                                "Khôi phục phòng vật lý thành công",
                                restoredRooms);
                return ResponseEntity.ok(response);
        }
}

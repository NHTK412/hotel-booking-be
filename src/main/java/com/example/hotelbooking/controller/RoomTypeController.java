package com.example.hotelbooking.controller;

import java.time.LocalDate;
import java.util.List;

// import org.hibernate.query.Page;
// org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.hotelbooking.dto.room.RoomRequestDTO;
import com.example.hotelbooking.dto.room.RoomSummaryDTO;
import com.example.hotelbooking.dto.roomtype.RoomTypeDetailDTO;
import com.example.hotelbooking.dto.roomtype.RoomTypeRequestDTO;
import com.example.hotelbooking.dto.roomtype.RoomTypeSummaryDTO;
import com.example.hotelbooking.security.CustomerUserDetails;
import com.example.hotelbooking.service.RoomTypeService;
import com.example.hotelbooking.util.ApiResponse;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/room-types")
public class RoomTypeController {

        final RoomTypeService roomTypeService;

        public RoomTypeController(RoomTypeService roomTypeService) {
                this.roomTypeService = roomTypeService;
        }

        // Public endpoints
        @GetMapping("/{roomTypeId}")
        public ResponseEntity<ApiResponse<RoomTypeDetailDTO>> getRoomTypeById(@PathVariable Long roomTypeId) {
                RoomTypeDetailDTO updatedRoomType = roomTypeService
                                .getRoomTypeById(roomTypeId);

                ApiResponse<RoomTypeDetailDTO> response = new ApiResponse<>(true,
                                "Room type retrieved successfully",
                                updatedRoomType);

                return ResponseEntity.ok(response);
        }

        @GetMapping("/{roomTypeId}/rooms")
        public ResponseEntity<ApiResponse<List<RoomSummaryDTO>>> getRoomsByRoomType(
                        @PathVariable Long roomTypeId) {
                List<RoomSummaryDTO> rooms = roomTypeService.getRoomsByRoomType(roomTypeId);

                ApiResponse<List<RoomSummaryDTO>> response = new ApiResponse<>(true,
                                "Rooms retrieved successfully",
                                rooms);

                return ResponseEntity.ok(response);
        }

        @GetMapping("/search")
        public ResponseEntity<ApiResponse<List<RoomTypeSummaryDTO>>> getAllRoomTypes(
                        // @RequestParam(required = false) String district,
                        // @RequestParam(required = false) String city,
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
                                // district,
                                // city,
                                locationId,
                                parsedCheckInDate,
                                parsedCheckOutDate,
                                capacity,
                                bedroom,
                                pageable);

                ApiResponse<List<RoomTypeSummaryDTO>> response = new ApiResponse<>(true,
                                "Room types retrieved successfully",
                                roomTypes);

                return ResponseEntity.ok(response);
        }

        private LocalDate parseDateOrNull(String value) {
                return value == null || value.isBlank() ? null : LocalDate.parse(value);
        }

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
                                "Room types retrieved successfully",
                                roomTypes);

                return ResponseEntity.ok(response);
        }

        @GetMapping("/accommodations/{accommodationId}/page")
        public ResponseEntity<ApiResponse<Page<RoomTypeSummaryDTO>>> getRoomTypesPageByAccommodation(
                        @PathVariable Long accommodationId,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {

                Pageable pageable = PageRequest.of(page, size);

                Page<RoomTypeSummaryDTO> roomTypes = roomTypeService.getRoomTypesPageByAccommodation(
                                accommodationId,
                                pageable);

                ApiResponse<Page<RoomTypeSummaryDTO>> response = new ApiResponse<>(true,
                                "Room types retrieved successfully",
                                roomTypes);

                return ResponseEntity.ok(response);
        }

        // Host room type management
        @PreAuthorize("hasAnyRole('HOST')")
        @PostMapping
        public ResponseEntity<ApiResponse<RoomTypeDetailDTO>> createRoomType(
                        @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
                        @RequestBody RoomTypeRequestDTO roomTypeRequestDTO) {
                RoomTypeDetailDTO createdRoomType = roomTypeService.createRoomType(
                                customerUserDetails.getProviderId(),
                                roomTypeRequestDTO);

                ApiResponse<RoomTypeDetailDTO> response = new ApiResponse<>(true,
                                "Room type created successfully",
                                createdRoomType);

                return ResponseEntity.ok(response);
        }

        @PreAuthorize("hasAnyRole('HOST')")
        @PutMapping("/{roomTypeId}")
        public ResponseEntity<ApiResponse<RoomTypeDetailDTO>> updateRoomType(
                        @PathVariable Long roomTypeId,
                        @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
                        @RequestBody RoomTypeRequestDTO roomTypeRequestDTO) {
                RoomTypeDetailDTO updatedRoomType = roomTypeService
                                .updateRoomType(customerUserDetails.getProviderId(), roomTypeId, roomTypeRequestDTO);

                ApiResponse<RoomTypeDetailDTO> response = new ApiResponse<>(true,
                                "Room type updated successfully",
                                updatedRoomType);

                return ResponseEntity.ok(response);
        }

        @PreAuthorize("hasAnyRole('HOST')")
        @PatchMapping("/{roomTypeId}")
        public ResponseEntity<ApiResponse<RoomTypeDetailDTO>> patchRoomType(
                        @PathVariable Long roomTypeId,
                        @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
                        @RequestParam Double price,
                        @RequestParam Double discount) {
                RoomTypeDetailDTO updatedRoomType = roomTypeService
                                .patchRoomType(customerUserDetails.getProviderId(), roomTypeId, price, discount);
                ApiResponse<RoomTypeDetailDTO> response = new ApiResponse<>(true,
                                "Room type patched successfully",
                                updatedRoomType);

                return ResponseEntity.ok(response);
        }

        @PreAuthorize("hasAnyRole('HOST')")
        @DeleteMapping("/{roomTypeId}")
        public ResponseEntity<ApiResponse<RoomTypeDetailDTO>> deleteRoomType(
                        @PathVariable Long roomTypeId,
                        @AuthenticationPrincipal CustomerUserDetails customerUserDetails) {
                RoomTypeDetailDTO deletedRoomType = roomTypeService
                                .deleteRoomType(customerUserDetails.getProviderId(), roomTypeId);

                ApiResponse<RoomTypeDetailDTO> response = new ApiResponse<>(true,
                                "Room type deleted successfully",
                                deletedRoomType);
                return ResponseEntity.ok(response);
        }

        // Host room management
        @PreAuthorize("hasAnyRole('HOST')")
        @PostMapping("/{roomTypeId}/rooms")
        public ResponseEntity<ApiResponse<List<RoomSummaryDTO>>> addRoomsToRoomType(
                        @PathVariable Long roomTypeId,
                        @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
                        @RequestBody RoomRequestDTO roomRequestDTO) {
                List<RoomSummaryDTO> addedRooms = roomTypeService.addRoomsToRoomType(
                                customerUserDetails.getProviderId(),
                                roomTypeId,
                                roomRequestDTO);

                ApiResponse<List<RoomSummaryDTO>> response = new ApiResponse<>(true,
                                "Rooms added to room type successfully",
                                addedRooms);
                return ResponseEntity.ok(response);
        }

        @PreAuthorize("hasAnyRole('HOST')")
        @DeleteMapping("/{roomTypeId}/rooms")
        public ResponseEntity<ApiResponse<List<RoomSummaryDTO>>> deleteRoomsFromRoomType(
                        @PathVariable Long roomTypeId,
                        @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
                        @RequestBody List<Long> roomIds) {
                List<RoomSummaryDTO> deletedRooms = roomTypeService.deleteRoomsFromRoomType(
                                customerUserDetails.getProviderId(),
                                roomTypeId,
                                roomIds);

                ApiResponse<List<RoomSummaryDTO>> response = new ApiResponse<>(true,
                                "Rooms deleted from room type successfully",
                                deletedRooms);

                return ResponseEntity.ok(response);
        }

}

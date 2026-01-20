package com.example.hotelbooking.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.function.EntityResponse;

import com.example.hotelbooking.dto.accommodation.AccommodationDetailDTO;
import com.example.hotelbooking.dto.accommodation.AccommodationRequestDTO;
import com.example.hotelbooking.dto.accommodation.AccommodationSummaryDTO;
import com.example.hotelbooking.enums.AccommodationTypeEnum;
import com.example.hotelbooking.security.CustomerUserDetails;
import com.example.hotelbooking.service.AccommodationService;
import com.example.hotelbooking.util.ApiResponse;

import java.util.List;

import org.springframework.context.annotation.Role;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/accommodations")
public class AccommodationController {

        private final AccommodationService accommodationService;

        public AccommodationController(AccommodationService accommodationService) {
                this.accommodationService = accommodationService;
        }

        @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
        @GetMapping
        public ResponseEntity<ApiResponse<List<AccommodationSummaryDTO>>> getAllAccommodations(
                        @RequestParam(defaultValue = "0") Integer page,
                        @RequestParam(defaultValue = "10") Integer size,
                        @RequestParam(required = false) AccommodationTypeEnum type) {
                // return new String();
                // return "Hello World";

                List<AccommodationSummaryDTO> accommodationSummaryDTOs = accommodationService
                                .getAllAccommodation(org.springframework.data.domain.PageRequest.of(page, size), type);

                ApiResponse<List<AccommodationSummaryDTO>> response = new ApiResponse<>(true,
                                "Accommodations fetched successfully",
                                accommodationSummaryDTOs);

                return ResponseEntity.ok(response);
        }

        @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
        @GetMapping("/{accommodationId}")
        public ResponseEntity<ApiResponse<AccommodationDetailDTO>> getAccommodationById(
                        @PathVariable Long accommodationId) {

                AccommodationDetailDTO accommodationDetailDTO = accommodationService
                                .getAccommodationById(accommodationId);

                ApiResponse<AccommodationDetailDTO> response = new ApiResponse<>(true,
                                "Accommodation fetched successfully",
                                accommodationDetailDTO);

                return ResponseEntity.ok(response);
        }

        @PreAuthorize("hasAnyRole('ADMIN')")
        @PostMapping
        public ResponseEntity<ApiResponse<AccommodationDetailDTO>> createAccommodation(
                        @RequestBody AccommodationRequestDTO accommodationRequestDTO) {

                AccommodationDetailDTO createdAccommodation = accommodationService
                                .createAccommodation(accommodationRequestDTO);

                ApiResponse<AccommodationDetailDTO> response = new ApiResponse<>(true,
                                "Accommodation created successfully",
                                createdAccommodation);

                return ResponseEntity.ok(response);
        }

        @PreAuthorize("hasAnyRole('ADMIN', 'HOST')")
        @DeleteMapping("/{accommodationId}")
        public ResponseEntity<ApiResponse<AccommodationDetailDTO>> deleteAccommodation(
                        @PathVariable Long accommodationId) {

                AccommodationDetailDTO deletedAccommodation = accommodationService
                                .deleteAccommodation(accommodationId);

                ApiResponse<AccommodationDetailDTO> response = new ApiResponse<>(true,
                                "Accommodation deleted successfully",
                                deletedAccommodation);

                return ResponseEntity.ok(response);
        }

        @PreAuthorize("hasAnyRole('ADMIN', 'HOST')")
        @PutMapping("/{accommodationId}")
        public ResponseEntity<ApiResponse<AccommodationDetailDTO>> updateAccommodation(
                        @PathVariable Long accommodationId,
                        @RequestBody AccommodationRequestDTO accommodationRequestDTO) {

                AccommodationDetailDTO updatedAccommodation = accommodationService
                                .updateAccommodation(accommodationId, accommodationRequestDTO);

                ApiResponse<AccommodationDetailDTO> response = new ApiResponse<>(true,
                                "Accommodation updated successfully",
                                updatedAccommodation);

                return ResponseEntity.ok(response);
        }

        @PreAuthorize("hasAnyRole('USER')")
        @GetMapping("/favorite")
        public ResponseEntity<ApiResponse<List<AccommodationSummaryDTO>>> getAllByFavorite(
                        @RequestParam(defaultValue = "0") Integer page,
                        @RequestParam(defaultValue = "10") Integer size) {
                // return new String();
                // return "Hello World";

                List<AccommodationSummaryDTO> accommodationSummaryDTOs = accommodationService
                                .getAllByFavorite(org.springframework.data.domain.PageRequest.of(page, size),
                                                Long.valueOf(4));

                ApiResponse<List<AccommodationSummaryDTO>> response = new ApiResponse<>(true,
                                "Accommodations fetched successfully",
                                accommodationSummaryDTOs);

                return ResponseEntity.ok(response);
        }

        @PreAuthorize("hasAnyRole('USER')")
        @PutMapping("/favorite/{accommodationId}")
        public ResponseEntity<ApiResponse<AccommodationDetailDTO>> updateFavoriteAccommodation(
                        @PathVariable Long accommodationId,
                        @RequestParam(required = true, defaultValue = "false") Boolean isFavorite) {

                AccommodationDetailDTO updatedAccommodation = accommodationService
                                .updateFavoriteAccommodation(accommodationId, isFavorite);

                ApiResponse<AccommodationDetailDTO> response = new ApiResponse<>(true,
                                "Accommodation updated successfully",
                                updatedAccommodation);

                return ResponseEntity.ok(response);
        }

        @PreAuthorize("hasAnyRole('USER')")
        @GetMapping("/nearby")
        public ResponseEntity<ApiResponse<List<AccommodationSummaryDTO>>> getNearbyAccommodations(
                        @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
                        @RequestParam Double latitude,
                        @RequestParam Double longitude,
                        @RequestParam(required = false, defaultValue = "5") Integer precision,
                        @RequestParam(required = false) String type) {

                List<AccommodationSummaryDTO> nearbyAccommodations = accommodationService
                                .findNearbyAccommodations(latitude, longitude, precision, type);

                ApiResponse<List<AccommodationSummaryDTO>> response = new ApiResponse<>(true,
                                "Nearby accommodations fetched successfully",
                                nearbyAccommodations);

                return ResponseEntity.ok(response);
        }

        @GetMapping("/search")
        public ResponseEntity<ApiResponse<List<AccommodationSummaryDTO>>> searchAccommodations(
                        @RequestParam String keyword,
                        @RequestParam(defaultValue = "0") Integer page,
                        @RequestParam(defaultValue = "10") Integer size) {

                List<AccommodationSummaryDTO> accommodationSummaryDTOs = accommodationService
                                .searchAccommodations(keyword,
                                                org.springframework.data.domain.PageRequest.of(page, size));

                ApiResponse<List<AccommodationSummaryDTO>> response = new ApiResponse<>(true,

                                "Accommodations fetched successfully",
                                accommodationSummaryDTOs);

                return ResponseEntity.ok(response);

        }

}

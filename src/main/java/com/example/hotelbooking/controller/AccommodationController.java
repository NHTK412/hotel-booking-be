package com.example.hotelbooking.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.hotelbooking.dto.accommodation.AccommodationDetailDTO;
import com.example.hotelbooking.dto.accommodation.AccommodationRequestDTO;
import com.example.hotelbooking.dto.accommodation.AccommodationSummaryDTO;
import com.example.hotelbooking.service.AccommodationService;
import com.example.hotelbooking.util.ApiResponse;

import java.util.List;

import org.springframework.http.ResponseEntity;
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

    @GetMapping
    public ResponseEntity<ApiResponse<List<AccommodationSummaryDTO>>> getAllAccommodations(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        // return new String();
        // return "Hello World";

        List<AccommodationSummaryDTO> accommodationSummaryDTOs = accommodationService
                .getAllAccommodation(org.springframework.data.domain.PageRequest.of(page, size));

        ApiResponse<List<AccommodationSummaryDTO>> response = new ApiResponse<>(true,
                "Accommodations fetched successfully",
                accommodationSummaryDTOs);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{accommodationId}")
    public ResponseEntity<ApiResponse<AccommodationDetailDTO>> getAccommodationById(
            @RequestParam Long accommodationId) {

        AccommodationDetailDTO accommodationDetailDTO = accommodationService.getAccommodationById(accommodationId);

        ApiResponse<AccommodationDetailDTO> response = new ApiResponse<>(true,
                "Accommodation fetched successfully",
                accommodationDetailDTO);

        return ResponseEntity.ok(response);
    }

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

}

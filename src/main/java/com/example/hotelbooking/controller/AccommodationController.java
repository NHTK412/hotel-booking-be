package com.example.hotelbooking.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.hotelbooking.dto.accommodation.AccommodationDetailDTO;
import com.example.hotelbooking.dto.accommodation.AccommodationSummaryDTO;
import com.example.hotelbooking.service.AccommodationService;
import com.example.hotelbooking.util.ApiResponse;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

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

        var accommodationDetailDTO = accommodationService.getAccommodationById(accommodationId);

        ApiResponse<AccommodationDetailDTO> response = new ApiResponse<>(true,
                "Accommodation fetched successfully",
                accommodationDetailDTO);

        return ResponseEntity.ok(response);
    }

}

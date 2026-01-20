package com.example.hotelbooking.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.function.EntityResponse;

import com.example.hotelbooking.dto.location.LocationResponseDTO;
import com.example.hotelbooking.service.LocationService;
import com.example.hotelbooking.util.ApiResponse;

@RestController
@RequestMapping("/locations")
class LocationController {

    final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<LocationResponseDTO>>> searchLocation(@RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<LocationResponseDTO> locations = locationService.getLocationByKeyword(keyword, page, size);

        final ApiResponse<List<LocationResponseDTO>> response = new ApiResponse<List<LocationResponseDTO>>(
                true,
                "Search location successfully",
                locations);

        return ResponseEntity.ok(response);
    }
}
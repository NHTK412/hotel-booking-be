package com.example.hotelbooking.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.hotelbooking.dto.location.LocationResponseDTO;
import com.example.hotelbooking.service.LocationService;
import com.example.hotelbooking.util.ApiResponse;
import com.github.davidmoten.geo.GeoHash;

@RestController
@RequestMapping("/locations")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<LocationResponseDTO>>> searchLocation(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<LocationResponseDTO> locations = locationService.getLocationByKeyword(keyword, page, size);

        ApiResponse<List<LocationResponseDTO>> response = new ApiResponse<>(
                true,
                "Search location successfully",
                locations);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<LocationResponseDTO>> getCurrentLocation(
            @RequestParam(required = false) String subAdministrativeArea,
            @RequestParam(required = false) String administrativeArea,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude) {

        LocationResponseDTO location = locationService.getCurrentLocation(subAdministrativeArea, administrativeArea,
                latitude, longitude);

        ApiResponse<LocationResponseDTO> response = new ApiResponse<>(
                true,
                "Get current location successfully",
                location);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/calculator")
    public ResponseEntity<ApiResponse<String>> calculateDistanceAndDuration(
            @RequestParam Double lat,
            @RequestParam Double lng) {

        String prefix = GeoHash.encodeHash(lat, lng, 12);
        return ResponseEntity.ok(new ApiResponse<>(true, "Calculate distance and duration successfully", prefix));
    }

    @GetMapping("/{locationId}")
    public ResponseEntity<ApiResponse<LocationResponseDTO>> getLocationById(
            @PathVariable Long locationId) {

        LocationResponseDTO location = locationService.getLocationById(locationId);

        ApiResponse<LocationResponseDTO> response = new ApiResponse<>(
                true,
                "Get location by id successfully",
                location);

        return ResponseEntity.ok(response);
    }
}
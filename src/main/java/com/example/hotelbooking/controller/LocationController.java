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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "6. Địa Điểm & Tọa Độ (Locations & Geocoding)", description = "Các API tìm kiếm địa điểm hành chính, xác định vị trí hiện tại và mã hóa tọa độ GeoHash")
@RestController
@RequestMapping("/locations")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @Operation(summary = "Tìm kiếm địa điểm theo từ khóa (Công khai)", description = "Tìm kiếm tỉnh, thành phố, quận, huyện theo từ khóa nhập vào")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<LocationResponseDTO>>> searchLocation(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<LocationResponseDTO> locations = locationService.getLocationByKeyword(keyword, page, size);

        ApiResponse<List<LocationResponseDTO>> response = new ApiResponse<>(
                true,
                "Tìm kiếm địa điểm thành công",
                locations);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Xác định địa điểm hiện tại (Công khai)", description = "Chuyển đổi tọa độ GPS hoặc tên quận/tỉnh thành bản ghi địa điểm chuẩn trong CSDL")
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
                "Lấy địa điểm hiện tại thành công",
                location);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Tính toán mã GeoHash từ tọa độ (Công khai)", description = "Mã hóa cặp tọa độ Vĩ độ và Kinh độ thành chuỗi GeoHash 12 ký tự phục vụ tìm kiếm không gian")
    @GetMapping("/calculator")
    public ResponseEntity<ApiResponse<String>> calculateDistanceAndDuration(
            @RequestParam Double lat,
            @RequestParam Double lng) {

        String prefix = GeoHash.encodeHash(lat, lng, 12);
        return ResponseEntity.ok(new ApiResponse<>(true, "Tính toán GeoHash thành công", prefix));
    }

    @Operation(summary = "Xem chi tiết địa điểm theo ID (Công khai)", description = "Lấy thông tin chi tiết địa điểm bằng khóa chính locationId")
    @GetMapping("/{locationId}")
    public ResponseEntity<ApiResponse<LocationResponseDTO>> getLocationById(
            @PathVariable Long locationId) {

        LocationResponseDTO location = locationService.getLocationById(locationId);

        ApiResponse<LocationResponseDTO> response = new ApiResponse<>(
                true,
                "Lấy thông tin địa điểm thành công",
                location);

        return ResponseEntity.ok(response);
    }
}
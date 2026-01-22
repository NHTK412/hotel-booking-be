package com.example.hotelbooking.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import com.example.hotelbooking.dto.location.LocationResponseDTO;
import com.example.hotelbooking.exception.customer.NotFoundException;
import com.example.hotelbooking.model.Location;
import com.example.hotelbooking.repository.LocationRepository;

@Service
public class LocationService {

    final LocationRepository locationRepository;

    public LocationService(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    public List<LocationResponseDTO> getLocationByKeyword(String keyword, int page, int size) {

        // List<Location> locations = locationRepository.findByKeyword(keyword);

        Page<Location> pageLocations = locationRepository.findByKeyword(keyword,
                org.springframework.data.domain.PageRequest.of(page, size));

        return pageLocations
                .stream()
                .map(
                        (l) -> LocationResponseDTO
                                .builder()
                                .locationId(l.getLocationId())
                                .provinceName(l.getProvinceName())
                                .districtName(l.getDistrictName())
                                .latitude(l.getLatitude())
                                .longitude(l.getLongitude())
                                .searchVector(l.getSearchVector())
                                .build())
                .toList();

    }

    public LocationResponseDTO getCurrentLocation(String subAdministrativeArea, String administrativeArea) {
        // return locationRepository.findCurrentLocation(subAdministrativeArea,
        // administrativeArea);

        // -- CASE 1: ƯU TIÊN TRUY VẤN THEO district_name bằng Subadministtravite
        // SELECT *
        // FROM locations l
        // WHERE l.district_name LIKE "%Tân Bình%";

        // -- Case 2: Nếu case 1 trả về [] thì truy vấn theo province_name bằng
        // Administrative
        // SELECT *
        // FROM locations l
        // WHERE l.province_name LIKE "%Hồ Chí Minh%";

        // -- Case 3: Nếu case 1 trả về > 1 thì truy vấn theo district_name và province
        // bằng Subadministtravite và Administrative
        // SELECT *
        // FROM locations l
        // WHERE l.district_name LIKE "%Tân Bình%" AND l.province_name LIKE "%Hồ Chí
        // Minh%";

        String sub = normalize(subAdministrativeArea);
        String ad = normalize(administrativeArea);

        List<Location> locations = new ArrayList<>();

        // Case 1
        locations = locationRepository.findByDistrictNameContainingIgnoreCase(sub);

        if (locations.isEmpty()) {
            // Case 2
            locations = locationRepository.findByProvinceNameContainingIgnoreCase(ad);
        } else if (locations.size() > 1) {
            // Case 3
            locations = locationRepository.findCurrentLocation(sub, ad);

        }

        if (locations.isEmpty()) {
            throw new NotFoundException("Location not found");
        }

        return locations.stream().findFirst().map(
                (l) -> LocationResponseDTO
                        .builder()
                        .locationId(l.getLocationId())
                        .provinceName(l.getProvinceName())
                        .districtName(l.getDistrictName())
                        .latitude(l.getLatitude())
                        .longitude(l.getLongitude())
                        .searchVector(l.getSearchVector())
                        .build())
                .orElse(null);

    }

    private String normalize(String value) {
        if (value == null)
            return null;
        value = value.trim();
        return value.isEmpty() ? null : value;
    }

    public LocationResponseDTO getLocationById(Long locationId) {
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new NotFoundException("Location not found"));

        return LocationResponseDTO.builder()
                .locationId(location.getLocationId())
                .provinceName(location.getProvinceName())
                .districtName(location.getDistrictName())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .searchVector(location.getSearchVector())
                .build();
    }

}

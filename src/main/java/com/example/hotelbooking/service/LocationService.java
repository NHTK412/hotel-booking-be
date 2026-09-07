package com.example.hotelbooking.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.example.hotelbooking.dto.location.LocationResponseDTO;
import com.example.hotelbooking.exception.NotFoundException;
import com.example.hotelbooking.model.Location;
import com.example.hotelbooking.repository.LocationRepository;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class LocationService {

    private final LocationRepository locationRepository;

    public LocationService(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    public List<LocationResponseDTO> getLocationByKeyword(String keyword, int page, int size) {
        Page<Location> pageLocations = locationRepository.findByKeyword(keyword, PageRequest.of(page, size));

        return pageLocations
                .stream()
                .map(l -> LocationResponseDTO
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

    public LocationResponseDTO getCurrentLocation(String subAdministrativeArea, String administrativeArea,
            Double latitude, Double longitude) {

        if (latitude != null && longitude != null) {
            String geoHash = com.github.davidmoten.geo.GeoHash.encodeHash(latitude, longitude, 5);
            List<Location> locations = locationRepository.findNearby(geoHash);
            if (!locations.isEmpty()) {
                Location l = locations.get(0);
                return LocationResponseDTO
                        .builder()
                        .locationId(l.getLocationId())
                        .provinceName(l.getProvinceName())
                        .districtName(l.getDistrictName())
                        .latitude(l.getLatitude())
                        .longitude(l.getLongitude())
                        .searchVector(l.getSearchVector())
                        .build();
            }
            throw new NotFoundException("Location not found");
        } else {
            String sub = normalize(subAdministrativeArea);
            String ad = normalize(administrativeArea);

            List<Location> locations = new ArrayList<>();

            locations = locationRepository.findByDistrictNameContainingIgnoreCase(sub);

            if (locations.isEmpty()) {
                locations = locationRepository.findByProvinceNameContainingIgnoreCase(ad);
            } else if (locations.size() > 1) {
                locations = locationRepository.findCurrentLocation(sub, ad);
            }

            if (locations.isEmpty()) {
                throw new NotFoundException("Location not found");
            }

            return locations.stream().findFirst().map(
                    l -> LocationResponseDTO
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

        return mapToDTO(location);
    }

    public List<String> getAllProvinces() {
        return locationRepository.findAllDistinctProvinces();
    }

    public List<LocationResponseDTO> getDistrictsByProvince(String provinceName) {
        if (provinceName == null || provinceName.trim().isEmpty()) {
            return List.of();
        }
        List<Location> locations = locationRepository.findByProvinceNameIgnoreCase(provinceName.trim());
        return locations.stream()
                .map(this::mapToDTO)
                .toList();
    }

    public List<LocationResponseDTO> getAllLocations() {
        List<Location> locations = locationRepository.findAllOrdered();
        return locations.stream()
                .map(this::mapToDTO)
                .toList();
    }

    private LocationResponseDTO mapToDTO(Location l) {
        return LocationResponseDTO.builder()
                .locationId(l.getLocationId())
                .provinceName(l.getProvinceName())
                .districtName(l.getDistrictName())
                .latitude(l.getLatitude())
                .longitude(l.getLongitude())
                .searchVector(l.getSearchVector())
                .build();
    }
}

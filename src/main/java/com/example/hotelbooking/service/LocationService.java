package com.example.hotelbooking.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import com.example.hotelbooking.dto.location.LocationResponseDTO;
import com.example.hotelbooking.model.Location;
import com.example.hotelbooking.repository.LocationRepository;

@Service
public class LocationService {

    final LocationRepository locationRepository;

    public LocationService(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    public List<LocationResponseDTO> getLocationByKeyword(String keyword,int page, int size) {

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

}

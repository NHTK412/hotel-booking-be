package com.example.hotelbooking.service;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.hotelbooking.dto.accommodation.AccommodationSummaryDTO;
import com.example.hotelbooking.model.Accommodations;
import com.example.hotelbooking.repository.AccommodationRepository;

@Service
public class AccommodationService {

    private final AccommodationRepository accommodationRepository;

    public AccommodationService(AccommodationRepository accommodationRepository) {
        this.accommodationRepository = accommodationRepository;
    }

    public List<AccommodationSummaryDTO> getAllAccommodation(Pageable pageable) {
        // return null;

        List<Accommodations> accommodations = accommodationRepository.findAll(pageable).toList();

        return accommodations.stream().map((accommodation) -> {
            return AccommodationSummaryDTO.builder()
                    .accommodationId(accommodation.getAccommodationId())
                    .accommodationName(accommodation.getAccommodationName())
                    .address(accommodation.getAddress())
                    .type(accommodation.getType())
                    .build();
        }).toList();
    }
}

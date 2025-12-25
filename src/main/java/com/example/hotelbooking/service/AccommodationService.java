package com.example.hotelbooking.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.hotelbooking.dto.accommodation.AccommodationDetailDTO;
import com.example.hotelbooking.dto.accommodation.AccommodationDetailDTO.AccommodationDetailDTOBuilder;
import com.example.hotelbooking.dto.roomtype.RoomTypeSummaryDTO;
import com.example.hotelbooking.dto.accommodation.AccommodationSummaryDTO;
import com.example.hotelbooking.model.Accommodations;
import com.example.hotelbooking.model.RoomTypes;
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

    public AccommodationDetailDTO getAccommodationById(Long accommodationId) {
        Accommodations accommodation = accommodationRepository.findById(accommodationId)
                .orElseThrow(() -> new RuntimeException("Accommodation not found"));

        AccommodationDetailDTOBuilder builder = AccommodationDetailDTO
                .builder()
                .accommodationId(accommodation.getAccommodationId())
                .accommodationName(accommodation.getAccommodationName())
                .description(accommodation.getDescription())
                .address(accommodation.getAddress())
                .city(accommodation.getCity())
                .latitude(accommodation.getLatitude())
                .longitude(accommodation.getLongitude())
                .type(accommodation.getType());

        Double starRating = 0.0;

        // List<RoomTypeSummaryDTO> roomTypeSummaries =
        // accommodation.getRooms().stream().map((room) -> {
        // return RoomTypeSummaryDTO.builder()
        // .roomtypeId(room.getRoomtypeId())
        // .name(room.getName())
        // .star(room.getStar())
        // .price(room.getPrice())
        // .image(room.getImage())
        // .build();
        // }).toList();

        List<RoomTypeSummaryDTO> roomTypeSummaries = new ArrayList<>();

        List<RoomTypes> rooms = accommodation.getRooms();
        if (rooms != null && !rooms.isEmpty()) {
            double totalStars = 0.0;
            for (RoomTypes room : rooms) {

                roomTypeSummaries.add(RoomTypeSummaryDTO.builder()
                        .roomtypeId(room.getRoomtypeId())
                        .name(room.getName())
                        .star(room.getStar())
                        .price(room.getPrice())
                        .image(room.getImage())
                        .build());

                totalStars += room.getStar();
            }
            starRating = totalStars / rooms.size();
        }

        builder
                .roomTypes(roomTypeSummaries)
                .starRating(starRating);

        return builder
                .starRating(starRating)
                .build();
    }
}

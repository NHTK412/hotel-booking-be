package com.example.hotelbooking.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.hotelbooking.dto.accommodation.AccommodationDetailDTO;
import com.example.hotelbooking.dto.accommodation.AccommodationRequestDTO;
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

                List<Accommodations> accommodations = accommodationRepository.findByIsDeletedFalse(pageable).toList();

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

                return convertToDetailDTO(accommodation);
        }

        public AccommodationDetailDTO createAccommodation(AccommodationRequestDTO accommodationRequestDTO) {
                Accommodations accommodation = new Accommodations();

                accommodation.setAccommodationName(accommodationRequestDTO.getAccommodationName());
                accommodation.setDescription(accommodationRequestDTO.getDescription());
                accommodation.setAddress(accommodationRequestDTO.getAddress());
                accommodation.setCity(accommodationRequestDTO.getCity());
                accommodation.setLatitude(accommodationRequestDTO.getLatitude());
                accommodation.setLongitude(accommodationRequestDTO.getLongitude());
                accommodation.setImage(accommodationRequestDTO.getImage());
                accommodation.setType(accommodationRequestDTO.getType());

                accommodationRepository.save(accommodation);

                return convertToDetailDTO(accommodation);
        }

        public AccommodationDetailDTO deleteAccommodation(Long accommodationId) {
                Accommodations accommodation = accommodationRepository.findById(accommodationId)
                                .orElseThrow(() -> new RuntimeException("Accommodation not found"));

                accommodation.setIsDeleted(true);
                accommodationRepository.save(accommodation);

                return convertToDetailDTO(accommodation);

        }

        public AccommodationDetailDTO updateAccommodation(Long accommodationId,
                        AccommodationRequestDTO accommodationRequestDTO) {
                Accommodations accommodation = accommodationRepository.findById(accommodationId)
                                .orElseThrow(() -> new RuntimeException("Accommodation not found"));

                accommodation.setAccommodationName(accommodationRequestDTO.getAccommodationName());
                accommodation.setDescription(accommodationRequestDTO.getDescription());
                accommodation.setAddress(accommodationRequestDTO.getAddress());
                accommodation.setCity(accommodationRequestDTO.getCity());
                accommodation.setLatitude(accommodationRequestDTO.getLatitude());
                accommodation.setLongitude(accommodationRequestDTO.getLongitude());
                accommodation.setImage(accommodationRequestDTO.getImage());
                accommodation.setType(accommodationRequestDTO.getType());

                accommodationRepository.save(accommodation);

                return convertToDetailDTO(accommodation);
        }

        private AccommodationDetailDTO convertToDetailDTO(Accommodations accommodation) {
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
                                if (room.getIsDeleted()) {
                                        continue;
                                }
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

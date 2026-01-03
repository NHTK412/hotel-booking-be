package com.example.hotelbooking.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.hotelbooking.dto.accommodation.AccommodationDetailDTO;
import com.example.hotelbooking.dto.accommodation.AccommodationRequestDTO;
import com.example.hotelbooking.dto.accommodation.AccommodationDetailDTO.AccommodationDetailDTOBuilder;
import com.example.hotelbooking.dto.roomtype.RoomTypeSummaryDTO;
import com.example.hotelbooking.enums.AccommodationTypeEnum;
import com.example.hotelbooking.exception.customer.NotFoundException;
import com.example.hotelbooking.dto.accommodation.AccommodationSummaryDTO;
import com.example.hotelbooking.model.Accommodations;
import com.example.hotelbooking.model.RoomTypes;
import com.example.hotelbooking.model.Users;
import com.example.hotelbooking.repository.AccommodationRepository;
import com.example.hotelbooking.repository.UserRepository;

@Service
public class AccommodationService {

        private final AccommodationRepository accommodationRepository;
        private final UserRepository userRepository;

        public AccommodationService(AccommodationRepository accommodationRepository, UserRepository userRepository) {
                this.accommodationRepository = accommodationRepository;
                this.userRepository = userRepository;
        }

        public List<AccommodationSummaryDTO> getAllAccommodation(Pageable pageable, AccommodationTypeEnum type) {
                // return null;

                List<Accommodations> accommodations = null;

                if (type != null) {
                        accommodations = accommodationRepository
                                        .findByIsDeletedFalseAndType(pageable, type)
                                        .getContent();
                } else {
                        accommodations = accommodationRepository
                                        .findByIsDeletedFalse(pageable)
                                        .getContent();
                }

                return accommodations.stream().map((accommodation) -> {

                        Double averageRating = 0.0;
                        Double minPricePerNight = Double.MAX_VALUE;

                        for (RoomTypes room : accommodation.getRooms()) {
                                if (room.getIsDeleted()) {
                                        continue;
                                }
                                averageRating += room.getStar();
                                if (room.getPrice() < minPricePerNight) {
                                        minPricePerNight = room.getPrice();
                                }
                        }

                        averageRating = accommodation.getRooms().isEmpty() ? 0.0
                                        : averageRating / accommodation.getRooms().size();

                        return AccommodationSummaryDTO.builder()
                                        .accommodationId(accommodation.getAccommodationId())
                                        .accommodationName(accommodation.getAccommodationName())
                                        .address(accommodation.getAddress())
                                        .type(accommodation.getType().getDescription())
                                        .image(accommodation.getImage())
                                        // .averageRating(accommodation.get)
                                        .averageRating(averageRating)
                                        .minPricePerNight(minPricePerNight == Double.MAX_VALUE ? 0.0 : minPricePerNight)
                                        .build();
                }).toList();
        }

        public List<AccommodationSummaryDTO> getAllByFavorite(Pageable pageable, Long userId) {
                // return null;

                List<Accommodations> accommodations = accommodationRepository
                                .findByIsDeletedFalseAndFavoritedByUsers_id(pageable, userId).toList();

                // if (type != null) {
                // accommodations = accommodationRepository
                // .findByIsDeletedFalseAndType(pageable, type)
                // .getContent();
                // } else {
                // accommodations = accommodationRepository
                // .findByIsDeletedFalse(pageable)
                // .getContent();
                // }

                return accommodations.stream().map((accommodation) -> {

                        Double averageRating = 0.0;
                        Double minPricePerNight = Double.MAX_VALUE;

                        for (RoomTypes room : accommodation.getRooms()) {
                                if (room.getIsDeleted()) {
                                        continue;
                                }
                                averageRating += room.getStar();
                                if (room.getPrice() < minPricePerNight) {
                                        minPricePerNight = room.getPrice();
                                }
                        }

                        averageRating = accommodation.getRooms().isEmpty() ? 0.0
                                        : averageRating / accommodation.getRooms().size();

                        return AccommodationSummaryDTO.builder()
                                        .accommodationId(accommodation.getAccommodationId())
                                        .accommodationName(accommodation.getAccommodationName())
                                        .address(accommodation.getAddress())
                                        .type(accommodation.getType().getDescription())
                                        .image(accommodation.getImage())
                                        // .averageRating(accommodation.get)
                                        .averageRating(averageRating)
                                        .minPricePerNight(minPricePerNight == Double.MAX_VALUE ? 0.0 : minPricePerNight)
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
                                .image(accommodation.getImage())

                                .type(accommodation.getType().getDescription());

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

                List<Users> favoritedByUsers = accommodation.getFavoritedByUsers();

                Users user = userRepository.findById(Long.valueOf(4)).orElse(null);

                boolean isFavorite = (user != null && favoritedByUsers.contains(user)) ? true : false;

                builder
                                .roomTypes(roomTypeSummaries)
                                .starRating(starRating)
                                .isFavorite(isFavorite);

                return builder.build();
        }

        public AccommodationDetailDTO updateFavoriteAccommodation(Long accommodationId, Boolean isFavorite) {
                Accommodations accommodation = accommodationRepository.findById(accommodationId)
                                .orElseThrow(() -> new NotFoundException("Accommodation not found"));

                Users user = userRepository.findById(Long.valueOf(4))
                                .orElseThrow(() -> new NotFoundException("User not found"));

                List<Users> favoritedByUsers = accommodation.getFavoritedByUsers();

                if (isFavorite) {
                        if (!favoritedByUsers.contains(user)) {
                                favoritedByUsers.add(user);
                        }
                } else {
                        favoritedByUsers.remove(user);
                }

                accommodation.setFavoritedByUsers(favoritedByUsers);
                accommodationRepository.save(accommodation);

                return convertToDetailDTO(accommodation);
        }
}

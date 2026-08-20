package com.example.hotelbooking.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.hotelbooking.dto.accommodation.AccommodationDetailDTO;
import com.example.hotelbooking.dto.accommodation.AccommodationDetailDTO.AccommodationDetailDTOBuilder;
import com.example.hotelbooking.dto.accommodation.AccommodationRequestDTO;
import com.example.hotelbooking.dto.accommodation.AccommodationSummaryDTO;
import com.example.hotelbooking.dto.roomtype.RoomTypeSummaryDTO;
import com.example.hotelbooking.enums.AccommodationTypeEnum;
import com.example.hotelbooking.exception.NotFoundException;
import com.example.hotelbooking.model.Accommodation;
import com.example.hotelbooking.model.RoomType;
import com.example.hotelbooking.model.User;
import com.example.hotelbooking.model.UserAuthProvider;
import com.example.hotelbooking.repository.AccommodationRepository;
import com.example.hotelbooking.repository.LocationRepository;
import com.example.hotelbooking.repository.UserAuthProviderRepository;
import com.example.hotelbooking.repository.UserRepository;
import com.github.davidmoten.geo.GeoHash;

import jakarta.transaction.Transactional;

@Service
public class AccommodationService {

        private final AccommodationRepository accommodationRepository;
        private final UserRepository userRepository;
        private final UserAuthProviderRepository userAuthProviderRepository;
        private final LocationRepository locationRepository;
        private final FileUploadService fileUploadService;

        public AccommodationService(AccommodationRepository accommodationRepository, UserRepository userRepository,
                        LocationRepository locationRepository, UserAuthProviderRepository userAuthProviderRepository,
                        FileUploadService fileUploadService) {
                this.accommodationRepository = accommodationRepository;
                this.userRepository = userRepository;
                this.locationRepository = locationRepository;
                this.userAuthProviderRepository = userAuthProviderRepository;
                this.fileUploadService = fileUploadService;
        }

        public List<AccommodationSummaryDTO> getAllAccommodation(Pageable pageable, AccommodationTypeEnum type,
                        Long locationId, Boolean sortBy) {

                List<Accommodation> accommodations = (sortBy != null && sortBy)
                                ? accommodationRepository
                                                .findByIsDeletedFalseAndLocationId(
                                                                pageable,
                                                                locationId,
                                                                type)
                                                .getContent()
                                : accommodationRepository
                                                .findByLocationIdAndTypeSortedByStar(
                                                                locationId,
                                                                type, pageable)
                                                .getContent();

                return accommodations.stream().map(accommodation -> {

                        Double averageRating = 0.0;
                        Double minPricePerNight = Double.MAX_VALUE;
                        Double discountMinPricePerNight = Double.MAX_VALUE;
                        Double finalMinPrice = Double.MAX_VALUE;

                        for (RoomType room : accommodation.getRooms()) {
                                if (room.getIsDeleted()) {
                                        continue;
                                }
                                averageRating += room.getStar();

                                Double roomPrice = room.getPrice();
                                Double discount = room.getDiscount() != null ? room.getDiscount() : 0.0;

                                Double finalPrice = roomPrice - (roomPrice * discount / 100);

                                if (finalPrice < finalMinPrice) {
                                        minPricePerNight = roomPrice;
                                        discountMinPricePerNight = discount;
                                        finalMinPrice = finalPrice;
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
                                        .discountMinPricePerNight(discountMinPricePerNight)
                                        .averageRating(averageRating)
                                        .minPricePerNight(minPricePerNight == Double.MAX_VALUE ? 0.0 : minPricePerNight)
                                        .build();
                }).toList();
        }

        public List<AccommodationSummaryDTO> getAllByFavorite(Pageable pageable, String providerId) {

                UserAuthProvider authProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("UserAuthProvider not found"));

                User user = authProvider.getUser();
                Long userId = user.getId();

                List<Accommodation> accommodations = accommodationRepository
                                .findByIsDeletedFalseAndFavoritedByUsers_id(pageable, userId).toList();

                return accommodations.stream().map(accommodation -> {

                        Double averageRating = 0.0;
                        Double minPricePerNight = Double.MAX_VALUE;

                        for (RoomType room : accommodation.getRooms()) {
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
                                        .averageRating(averageRating)
                                        .minPricePerNight(minPricePerNight == Double.MAX_VALUE ? 0.0 : minPricePerNight)
                                        .build();
                }).toList();
        }

        public AccommodationDetailDTO getAccommodationById(String providerId, Long accommodationId) {

                UserAuthProvider authProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("UserAuthProvider not found"));

                User user = authProvider.getUser();

                Accommodation accommodation = accommodationRepository.findById(accommodationId)
                                .orElseThrow(() -> new NotFoundException("Accommodation not found"));

                return (user != null)
                                ? convertToDetailDTO(accommodation, user)
                                : convertToDetailDTO(accommodation);
        }

        public AccommodationDetailDTO createAccommodation(AccommodationRequestDTO accommodationRequestDTO) {
                Accommodation accommodation = new Accommodation();

                accommodation.setAccommodationName(accommodationRequestDTO.getAccommodationName());
                accommodation.setDescription(accommodationRequestDTO.getDescription());
                accommodation.setAddress(accommodationRequestDTO.getAddress());
                accommodation.setCity(accommodationRequestDTO.getCity());
                accommodation.setLatitude(accommodationRequestDTO.getLatitude());
                accommodation.setLongitude(accommodationRequestDTO.getLongitude());

                if (accommodationRequestDTO.getImage() != null) {
                        accommodation.setImage(accommodationRequestDTO.getImage());
                        fileUploadService.deleteFile(accommodationRequestDTO.getImage());
                }
                accommodation.setType(accommodationRequestDTO.getType());

                accommodation.setLocation(locationRepository.findById(accommodationRequestDTO.getLocationId())
                                .orElseThrow(() -> new NotFoundException("Location not found")));

                String geoHash = GeoHash.encodeHash(accommodationRequestDTO.getLatitude(),
                                accommodationRequestDTO.getLongitude(), 12);

                accommodation.setGeohash(geoHash);

                accommodationRepository.save(accommodation);

                return convertToDetailDTO(accommodation);
        }

        public AccommodationDetailDTO deleteAccommodation(Long accommodationId) {
                Accommodation accommodation = accommodationRepository.findById(accommodationId)
                                .orElseThrow(() -> new NotFoundException("Accommodation not found"));

                accommodation.setIsDeleted(true);
                accommodationRepository.save(accommodation);

                return convertToDetailDTO(accommodation);
        }

        public AccommodationDetailDTO updateAccommodation(Long accommodationId,
                        AccommodationRequestDTO accommodationRequestDTO) {
                Accommodation accommodation = accommodationRepository.findById(accommodationId)
                                .orElseThrow(() -> new NotFoundException("Accommodation not found"));

                accommodation.setAccommodationName(accommodationRequestDTO.getAccommodationName());
                accommodation.setDescription(accommodationRequestDTO.getDescription());
                accommodation.setAddress(accommodationRequestDTO.getAddress());
                accommodation.setCity(accommodationRequestDTO.getCity());
                accommodation.setLatitude(accommodationRequestDTO.getLatitude());
                accommodation.setLongitude(accommodationRequestDTO.getLongitude());

                if (accommodationRequestDTO.getImage() != null) {
                        accommodation.setImage(accommodationRequestDTO.getImage());
                        fileUploadService.deleteFile(accommodationRequestDTO.getImage());
                }
                accommodation.setType(accommodationRequestDTO.getType());
                accommodation.setLocation(locationRepository.findById(accommodationRequestDTO.getLocationId())
                                .orElseThrow(() -> new NotFoundException("Location not found")));

                String geoHash = GeoHash.encodeHash(accommodationRequestDTO.getLatitude(),
                                accommodationRequestDTO.getLongitude(), 12);

                accommodation.setGeohash(geoHash);

                accommodationRepository.save(accommodation);

                return convertToDetailDTO(accommodation, null);
        }

        private AccommodationDetailDTO convertToDetailDTO(Accommodation accommodation) {
                return convertToDetailDTO(accommodation, null);
        }

        private AccommodationDetailDTO convertToDetailDTO(Accommodation accommodation, User user) {

                Boolean isFavorite = false;

                if (user != null) {
                        List<User> favoritedByUsers = accommodation.getFavoritedByUsers();
                        isFavorite = favoritedByUsers != null && favoritedByUsers.contains(user);
                }

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
                                .type(accommodation.getType().getDescription())
                                .isFavorite(isFavorite)
                                .locationId(accommodation.getLocation().getLocationId());

                Double starRating = 0.0;
                List<RoomTypeSummaryDTO> roomTypeSummaries = new ArrayList<>();
                List<RoomType> rooms = accommodation.getRooms();

                if (rooms != null && !rooms.isEmpty()) {
                        double totalStars = 0.0;
                        for (RoomType room : rooms) {
                                if (room.getIsDeleted()) {
                                        continue;
                                }
                                roomTypeSummaries.add(RoomTypeSummaryDTO.builder()
                                                .roomtypeId(room.getRoomtypeId())
                                                .name(room.getName())
                                                .star(room.getStar())
                                                .price(room.getPrice())
                                                .image(room.getImage())
                                                .discount(room.getDiscount())
                                                .address(accommodation.getAddress())
                                                .build());

                                totalStars += room.getStar();
                        }
                        starRating = totalStars / rooms.size();
                }

                builder
                                .roomTypes(roomTypeSummaries)
                                .starRating(starRating)
                                .isFavorite(isFavorite);

                return builder.build();
        }

        @Transactional
        public AccommodationDetailDTO updateFavoriteAccommodation(String providerId, Long accommodationId,
                        Boolean isFavorite) {
                Accommodation accommodation = accommodationRepository.findById(accommodationId)
                                .orElseThrow(() -> new NotFoundException("Accommodation not found"));

                UserAuthProvider authProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("UserAuthProvider not found"));

                User user = authProvider.getUser();

                List<User> favoritedByUsers = accommodation.getFavoritedByUsers();
                if (favoritedByUsers == null) {
                        favoritedByUsers = new ArrayList<>();
                }

                if (isFavorite) {
                        if (!favoritedByUsers.contains(user)) {
                                favoritedByUsers.add(user);
                        }
                } else {
                        favoritedByUsers.remove(user);
                }

                accommodation.setFavoritedByUsers(favoritedByUsers);
                accommodationRepository.save(accommodation);

                return convertToDetailDTO(accommodation, user);
        }

        public List<AccommodationSummaryDTO> findNearbyAccommodations(double latitude, double longitude,
                        Integer precision, String type) {

                String prefix = GeoHash.encodeHash(latitude, longitude, precision);
                List<Accommodation> nearbyAccommodations;

                if (type != null && !type.isEmpty()) {
                        try {
                                AccommodationTypeEnum typeEnum = AccommodationTypeEnum.valueOf(type.toUpperCase());
                                nearbyAccommodations = accommodationRepository.findNearbyWithType(prefix, typeEnum);
                        } catch (IllegalArgumentException e) {
                                nearbyAccommodations = accommodationRepository.findNearby(prefix);
                        }
                } else {
                        nearbyAccommodations = accommodationRepository.findNearby(prefix);
                }

                return nearbyAccommodations.stream()
                                .map(this::convertToSummaryDTO)
                                .toList();
        }

        private AccommodationSummaryDTO convertToSummaryDTO(Accommodation accommodation) {

                Double averageRating = 0.0;
                Double minPricePerNight = Double.MAX_VALUE;
                Double discountMinPricePerNight = Double.MAX_VALUE;
                Double finalMinPrice = Double.MAX_VALUE;

                if (accommodation.getRooms() != null) {
                        for (RoomType room : accommodation.getRooms()) {
                                if (room.getIsDeleted()) {
                                        continue;
                                }
                                averageRating += room.getStar();

                                Double roomPrice = room.getPrice();
                                Double discount = room.getDiscount() != null ? room.getDiscount() : 0.0;

                                Double finalPrice = roomPrice - (roomPrice * discount / 100);

                                if (finalPrice < finalMinPrice) {
                                        minPricePerNight = roomPrice;
                                        discountMinPricePerNight = discount;
                                        finalMinPrice = finalPrice;
                                }
                        }

                        averageRating = accommodation.getRooms().isEmpty() ? 0.0
                                        : averageRating / accommodation.getRooms().size();
                } else {
                        minPricePerNight = 0.0;
                        discountMinPricePerNight = 0.0;
                }

                return AccommodationSummaryDTO.builder()
                                .accommodationId(accommodation.getAccommodationId())
                                .accommodationName(accommodation.getAccommodationName())
                                .address(accommodation.getAddress())
                                .type(accommodation.getType().getDescription())
                                .image(accommodation.getImage())
                                .averageRating(averageRating)
                                .minPricePerNight(minPricePerNight == Double.MAX_VALUE ? 0.0 : minPricePerNight)
                                .discountMinPricePerNight(discountMinPricePerNight == Double.MAX_VALUE ? 0.0
                                                : discountMinPricePerNight)
                                .lat(accommodation.getLatitude())
                                .lng(accommodation.getLongitude())
                                .build();
        }

        public List<AccommodationSummaryDTO> searchAccommodations(String keyword, Pageable pageable) {
                List<Accommodation> accommodations = accommodationRepository
                                .searchByKeyword(keyword, pageable).toList();

                return accommodations.stream()
                                .map(this::convertToSummaryDTO)
                                .toList();
        }
}

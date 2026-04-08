package com.example.hotelbooking.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.catalina.User;
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
import com.example.hotelbooking.model.UserAuthProvider;
import com.example.hotelbooking.model.Users;
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
                // return null;

                List<Accommodations> accommodations = null;

                // if (type != null) {
                // accommodations = accommodationRepository
                // .findByIsDeletedFalseAndType(pageable, type)
                // .getContent();
                // } else {
                // accommodations = accommodationRepository
                // .findByIsDeletedFalse(pageable)
                // .getContent();
                // }

                accommodations = (sortBy != null && sortBy)
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

                return accommodations.stream().map((accommodation) -> {

                        Double averageRating = 0.0;
                        Double minPricePerNight = Double.MAX_VALUE;
                        Double discountMinPricePerNight = Double.MAX_VALUE;
                        Double finalMinPrice = Double.MAX_VALUE;

                        for (RoomTypes room : accommodation.getRooms()) {
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
                                        // .averageRating(accommodation.get)
                                        .discountMinPricePerNight(discountMinPricePerNight)
                                        .averageRating(averageRating)
                                        .minPricePerNight(minPricePerNight == Double.MAX_VALUE ? 0.0 : minPricePerNight)
                                        .build();
                }).toList();
        }

        public List<AccommodationSummaryDTO> getAllByFavorite(Pageable pageable, String providerId) {
                // return null;

                UserAuthProvider authProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("UserAuthProvider not found"));

                Users user = authProvider.getUser();
                Long userId = user.getId();

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

        public AccommodationDetailDTO getAccommodationById(String providerId, Long accommodationId) {

                UserAuthProvider authProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("UserAuthProvider not found"));

                Users user = authProvider.getUser();

                Accommodations accommodation = accommodationRepository.findById(accommodationId)
                                .orElseThrow(() -> new RuntimeException("Accommodation not found"));

                return (user != null)
                                ? convertToDetailDTO(accommodation, user)
                                : convertToDetailDTO(accommodation);
        }

        public AccommodationDetailDTO createAccommodation(AccommodationRequestDTO accommodationRequestDTO) {
                Accommodations accommodation = new Accommodations();

                accommodation.setAccommodationName(accommodationRequestDTO.getAccommodationName());
                accommodation.setDescription(accommodationRequestDTO.getDescription());
                accommodation.setAddress(accommodationRequestDTO.getAddress());
                accommodation.setCity(accommodationRequestDTO.getCity());
                accommodation.setLatitude(accommodationRequestDTO.getLatitude());
                accommodation.setLongitude(accommodationRequestDTO.getLongitude());
                // accommodation.setImage(accommodationRequestDTO.getImage());
                if (accommodationRequestDTO.getImage() != null) {
                        accommodation.setImage(accommodationRequestDTO.getImage());
                        fileUploadService.deleteFile(accommodationRequestDTO.getImage());
                }
                accommodation.setType(accommodationRequestDTO.getType());

                accommodation.setLocation(locationRepository.findById(accommodationRequestDTO.getLocationId())
                                .orElseThrow(() -> new NotFoundException("Location not found")));

                // Tính toán và lưu mã hash vị trí địa lý
                String geoHash = GeoHash.encodeHash(accommodationRequestDTO.getLatitude(),
                                accommodationRequestDTO.getLongitude(), 12);

                accommodation.setGeohash(geoHash);
                // --------------------------------------------

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
                // accommodation.setImage(accommodationRequestDTO.getImage());
                if (accommodationRequestDTO.getImage() != null) {
                        accommodation.setImage(accommodationRequestDTO.getImage());
                        fileUploadService.deleteFile(accommodationRequestDTO.getImage());
                }
                accommodation.setType(accommodationRequestDTO.getType());
                accommodation.setLocation(locationRepository.findById(accommodationRequestDTO.getLocationId())
                                .orElseThrow(() -> new NotFoundException("Location not found")));
                // Tính toán và lưu mã hash vị trí địa lý
                String geoHash = GeoHash.encodeHash(accommodationRequestDTO.getLatitude(),
                                accommodationRequestDTO.getLongitude(), 12);

                accommodation.setGeohash(geoHash);
                // --------------------------------------------

                accommodationRepository.save(accommodation);

                return convertToDetailDTO(accommodation, null);
        }

        private AccommodationDetailDTO convertToDetailDTO(Accommodations accommodation) {
                return convertToDetailDTO(accommodation, null);
        }

        private AccommodationDetailDTO convertToDetailDTO(Accommodations accommodation, Users user) {

                Boolean isFavorite = false;

                if (user != null) {
                        List<Users> favoritedByUsers = accommodation.getFavoritedByUsers();
                        isFavorite = favoritedByUsers.contains(user) ? true : false;
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
                                                .discount(room.getDiscount())
                                                .address(accommodation.getAddress())
                                                .build());

                                totalStars += room.getStar();
                        }
                        starRating = totalStars / rooms.size();
                }

                // List<Users> favoritedByUsers = accommodation.getFavoritedByUsers();

                // Users user = userRepository.findById(Long.valueOf(4)).orElse(null);

                // boolean isFavorite = (user != null && favoritedByUsers.contains(user)) ? true
                // : false;

                builder
                                .roomTypes(roomTypeSummaries)
                                .starRating(starRating)
                                .isFavorite(isFavorite);

                return builder.build();
        }

        @Transactional
        public AccommodationDetailDTO updateFavoriteAccommodation(String providerId, Long accommodationId,
                        Boolean isFavorite) {
                Accommodations accommodation = accommodationRepository.findById(accommodationId)
                                .orElseThrow(() -> new NotFoundException("Accommodation not found"));

                // Users user = userRepository.findById(Long.valueOf(4))
                // .orElseThrow(() -> new NotFoundException("User not found"));

                UserAuthProvider authProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("UserAuthProvider not found"));

                Users user = authProvider.getUser();

                List<Users> favoritedByUsers = accommodation.getFavoritedByUsers();

                // boolean isCurrentlyFavorite = favoritedByUsers.contains(user);

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
                List<Accommodations> nearbyAccommodations;

                if (type != null && !type.isEmpty()) {
                        nearbyAccommodations = accommodationRepository.findNearbyWithType(prefix, type);
                } else {
                        nearbyAccommodations = accommodationRepository.findNearby(prefix);
                }

                return nearbyAccommodations.stream()
                                .map(this::convertToSummaryDTO)
                                .toList();
        }

        private AccommodationSummaryDTO convertToSummaryDTO(Accommodations accommodation) {

                Double averageRating = 0.0;
                Double minPricePerNight = Double.MAX_VALUE;
                Double discountMinPricePerNight = Double.MAX_VALUE;
                Double finalMinPrice = Double.MAX_VALUE;

                for (RoomTypes room : accommodation.getRooms()) {
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
                // Double averageRating = 0.0;
                // Double minPricePerNight = Double.MAX_VALUE;

                // for (RoomTypes room : accommodation.getRooms()) {
                // if (room.getIsDeleted()) {
                // continue;
                // }
                // averageRating += room.getStar();
                // if (room.getPrice() < minPricePerNight) {
                // minPricePerNight = room.getPrice();
                // }
                // }

                // averageRating = accommodation.getRooms().isEmpty() ? 0.0
                // : averageRating / accommodation.getRooms().size();

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
                List<Accommodations> accommodations = accommodationRepository
                                .searchByKeyword(keyword, pageable).toList();

                return accommodations.stream()
                                .map(this::convertToSummaryDTO)
                                .toList();
        }

}

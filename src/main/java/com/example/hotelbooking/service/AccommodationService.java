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
import com.example.hotelbooking.model.Location;
import com.example.hotelbooking.model.AccommodationStaff;
import org.springframework.security.access.AccessDeniedException;

import jakarta.transaction.Transactional;

@Service
@Transactional
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
                                                .findByLocationIdAndTypeSortedByStar(
                                                                locationId,
                                                                type, pageable)
                                                .getContent()
                                : accommodationRepository
                                                .findByIsDeletedFalseAndLocationId(
                                                                pageable,
                                                                locationId,
                                                                type)
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
                                        .type(accommodation.getType() != null ? accommodation.getType().getDescription() : null)
                                        .image(accommodation.getImage())
                                        .discountMinPricePerNight(discountMinPricePerNight)
                                        .averageRating(averageRating)
                                        .minPricePerNight(minPricePerNight == Double.MAX_VALUE ? 0.0 : minPricePerNight)
                                        .lat(accommodation.getLatitude())
                                        .lng(accommodation.getLongitude())
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
                                        .type(accommodation.getType() != null ? accommodation.getType().getDescription() : null)
                                        .image(accommodation.getImage())
                                        .averageRating(averageRating)
                                        .minPricePerNight(minPricePerNight == Double.MAX_VALUE ? 0.0 : minPricePerNight)
                                        .build();
                }).toList();
        }

        public AccommodationDetailDTO getAccommodationById(String providerId, Long accommodationId) {
                User user = null;
                if (providerId != null && !providerId.isBlank()) {
                        UserAuthProvider authProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                        .orElse(null);
                        if (authProvider != null) {
                                user = authProvider.getUser();
                        }
                }

                Accommodation accommodation = accommodationRepository.findById(accommodationId)
                                .orElseThrow(() -> new NotFoundException("Accommodation not found"));

                return convertToDetailDTO(accommodation, user);
        }

        public AccommodationDetailDTO createAccommodation(AccommodationRequestDTO accommodationRequestDTO) {
                Accommodation accommodation = new Accommodation();

                accommodation.setAccommodationName(accommodationRequestDTO.getAccommodationName());
                accommodation.setDescription(accommodationRequestDTO.getDescription());
                accommodation.setAddress(accommodationRequestDTO.getAddress());
                accommodation.setImage(accommodationRequestDTO.getImage());
                accommodation.setType(accommodationRequestDTO.getType());

                Location location = locationRepository.findById(accommodationRequestDTO.getLocationId())
                                .orElseThrow(() -> new NotFoundException("Location not found"));
                accommodation.setLocation(location);

                // Gán district từ DTO hoặc fallback sang Location
                if (accommodationRequestDTO.getDistrict() != null && !accommodationRequestDTO.getDistrict().isBlank()) {
                        accommodation.setDistrict(accommodationRequestDTO.getDistrict());
                } else {
                        accommodation.setDistrict(location.getDistrictName());
                }

                // Gán city từ DTO hoặc fallback sang Location
                if (accommodationRequestDTO.getCity() != null && !accommodationRequestDTO.getCity().isBlank()) {
                        accommodation.setCity(accommodationRequestDTO.getCity());
                } else {
                        accommodation.setCity(location.getProvinceName());
                }

                // Xử lý tọa độ với fallback từ Location
                Double lat = accommodationRequestDTO.getLatitude() != null ? accommodationRequestDTO.getLatitude()
                                : location.getLatitude();
                Double lng = accommodationRequestDTO.getLongitude() != null ? accommodationRequestDTO.getLongitude()
                                : location.getLongitude();
                accommodation.setLatitude(lat);
                accommodation.setLongitude(lng);

                if (lat != null && lng != null) {
                        String geoHash = GeoHash.encodeHash(lat, lng, 12);
                        accommodation.setGeohash(geoHash);
                } else if (location.getGeoHash() != null) {
                        accommodation.setGeohash(location.getGeoHash());
                }

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
                return updateAccommodation(null, accommodationId, accommodationRequestDTO);
        }

        public AccommodationDetailDTO updateAccommodation(String providerId, Long accommodationId,
                        AccommodationRequestDTO accommodationRequestDTO) {
                Accommodation accommodation = accommodationRepository.findById(accommodationId)
                                .orElseThrow(() -> new NotFoundException("Accommodation not found"));

                // Nếu request từ Host, kiểm tra quyền sở hữu khách sạn
                if (providerId != null && !providerId.isBlank()) {
                        UserAuthProvider authProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                        .orElseThrow(() -> new NotFoundException("UserAuthProvider not found"));
                        ensureHostHasAccommodation(authProvider, accommodationId);
                }

                accommodation.setAccommodationName(accommodationRequestDTO.getAccommodationName());
                accommodation.setDescription(accommodationRequestDTO.getDescription());
                accommodation.setAddress(accommodationRequestDTO.getAddress());

                Location location = locationRepository.findById(accommodationRequestDTO.getLocationId())
                                .orElseThrow(() -> new NotFoundException("Location not found"));
                accommodation.setLocation(location);

                // Gán district từ DTO hoặc fallback sang Location
                if (accommodationRequestDTO.getDistrict() != null && !accommodationRequestDTO.getDistrict().isBlank()) {
                        accommodation.setDistrict(accommodationRequestDTO.getDistrict());
                } else {
                        accommodation.setDistrict(location.getDistrictName());
                }

                // Gán city từ DTO hoặc fallback sang Location
                if (accommodationRequestDTO.getCity() != null && !accommodationRequestDTO.getCity().isBlank()) {
                        accommodation.setCity(accommodationRequestDTO.getCity());
                } else {
                        accommodation.setCity(location.getProvinceName());
                }

                // Xử lý tọa độ với fallback từ Location
                Double lat = accommodationRequestDTO.getLatitude() != null ? accommodationRequestDTO.getLatitude()
                                : location.getLatitude();
                Double lng = accommodationRequestDTO.getLongitude() != null ? accommodationRequestDTO.getLongitude()
                                : location.getLongitude();
                accommodation.setLatitude(lat);
                accommodation.setLongitude(lng);

                if (lat != null && lng != null) {
                        String geoHash = GeoHash.encodeHash(lat, lng, 12);
                        accommodation.setGeohash(geoHash);
                } else if (location.getGeoHash() != null) {
                        accommodation.setGeohash(location.getGeoHash());
                }

                if (accommodationRequestDTO.getImage() != null && !accommodationRequestDTO.getImage().isBlank()) {
                        String oldImage = accommodation.getImage();
                        if (oldImage != null && !oldImage.equals(accommodationRequestDTO.getImage())) {
                                try {
                                        fileUploadService.deleteFile(oldImage);
                                } catch (Exception ignored) {
                                }
                        }
                        accommodation.setImage(accommodationRequestDTO.getImage());
                }

                accommodation.setType(accommodationRequestDTO.getType());

                accommodationRepository.save(accommodation);

                return convertToDetailDTO(accommodation, null);
        }

        private void ensureHostHasAccommodation(UserAuthProvider userAuthProvider, Long accommodationId) {
                if (userAuthProvider.getUser() == null) {
                        throw new AccessDeniedException("User account not found.");
                }

                List<AccommodationStaff> staffAssignments = userAuthProvider.getUser().getAccommodationStaffs();

                boolean hasAccess = staffAssignments != null && staffAssignments.stream()
                                .map(staff -> staff.getAccommodation().getAccommodationId())
                                .anyMatch(id -> id.equals(accommodationId));

                if (!hasAccess) {
                        throw new AccessDeniedException("Bạn không có quyền quản trị hoặc chỉnh sửa cơ sở lưu trú này.");
                }
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
                                .type(accommodation.getType() != null ? accommodation.getType().getDescription() : null)
                                .isFavorite(isFavorite)
                                .locationId(accommodation.getLocation() != null ? accommodation.getLocation().getLocationId() : null);

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

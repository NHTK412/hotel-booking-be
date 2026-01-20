package com.example.hotelbooking.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
// import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Service;

import com.example.hotelbooking.dto.room.RoomRequestDTO;
import com.example.hotelbooking.dto.room.RoomSummaryDTO;
import com.example.hotelbooking.dto.roomtype.RoomTypeDetailDTO;
import com.example.hotelbooking.dto.roomtype.RoomTypeRequestDTO;
import com.example.hotelbooking.dto.roomtype.RoomTypeSummaryDTO;
import com.example.hotelbooking.enums.StatusEnum;
import com.example.hotelbooking.exception.customer.AccessDeniedException;
import com.example.hotelbooking.exception.customer.NotFoundException;
import com.example.hotelbooking.model.AccommodationStaff;
import com.example.hotelbooking.model.Accommodations;
import com.example.hotelbooking.model.RoomTypes;
import com.example.hotelbooking.model.Rooms;
import com.example.hotelbooking.model.UserAuthProvider;
import com.example.hotelbooking.repository.RoomTypeRepository;
import com.example.hotelbooking.repository.UserAuthProviderRepository;
import com.example.hotelbooking.repository.AccommodationRepository;

import jakarta.transaction.Transactional;

@Service
public class RoomTypeService {

    private final RoomTypeRepository roomTypeRepository;

    private final UserAuthProviderRepository userAuthProviderRepository;

    private final AccommodationRepository accommodationRepository;

    public RoomTypeService(RoomTypeRepository roomTypeRepository,
            UserAuthProviderRepository userAuthProviderRepository,
            AccommodationRepository accommodationRepository) {
        this.roomTypeRepository = roomTypeRepository;
        this.userAuthProviderRepository = userAuthProviderRepository;
        this.accommodationRepository = accommodationRepository;
    }

    public RoomTypeDetailDTO getRoomTypeById(Long roomTypeId) {
        RoomTypes roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new NotFoundException("Room type not found with id: " + roomTypeId));

        return mapToRoomTypeDetailDTO(roomType);
    }

    public List<RoomTypeSummaryDTO> getAllRoomTypes(
            String district,
            String city,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            Integer capacity,
            Integer bedroom,
            Pageable pageable) {

        LocalDateTime checkInAt = checkInDate != null ? checkInDate.atTime(14, 0, 0) : null;
        LocalDateTime checkOutAt = checkOutDate != null ? checkOutDate.atTime(12, 0, 0) : null;

        List<RoomTypes> roomTypes = roomTypeRepository.findAvailableRoomTypes(
                normalizeFilter(district),
                normalizeFilter(city),
                normalizePositiveInteger(capacity),
                normalizePositiveInteger(bedroom),
                checkInAt,
                checkOutAt,
                pageable).toList();

        return roomTypes.stream().map(roomType -> RoomTypeSummaryDTO.builder()
                .roomtypeId(roomType.getRoomtypeId())
                .name(roomType.getName())
                .star(roomType.getStar())
                .price(roomType.getPrice())
                .image(roomType.getImage())
                .address(roomType.getAccommodation().getAddress()) // Thêm địa chỉ vào DTO
                .build()).toList();
    }

    private String normalizeFilter(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Integer normalizePositiveInteger(Integer value) {
        return value != null && value > 0 ? value : null;
    }

    @Transactional
    public RoomTypeDetailDTO createRoomType(String providerId, RoomTypeRequestDTO roomTypeRequestDTO) {
        if (roomTypeRequestDTO.getAccommodationId() == null) {
            throw new IllegalArgumentException("Accommodation id is required to create a room type.");
        }

        Long accommodationId = roomTypeRequestDTO.getAccommodationId();

        UserAuthProvider userAuthProvider = getUserAuthProvider(providerId);
        ensureHostHasAccommodation(userAuthProvider, accommodationId);

        Accommodations accommodation = getAccommodation(accommodationId);

        RoomTypes roomType = new RoomTypes();
        roomType.setName(roomTypeRequestDTO.getName());
        roomType.setStar(0);
        roomType.setPrice(roomTypeRequestDTO.getPrice());
        roomType.setDiscount(roomTypeRequestDTO.getDiscount());
        roomType.setImage(roomTypeRequestDTO.getImage());
        roomType.setImagesPreview(roomTypeRequestDTO.getImagesPreview());
        // Set other fields as necessary

        roomType.setAmenities(roomTypeRequestDTO.getAmenities());
        roomType.setAccommodation(accommodation);

        RoomTypes savedRoomType = roomTypeRepository.save(roomType);
        return mapToRoomTypeDetailDTO(savedRoomType);
    }

    @Transactional
    public RoomTypeDetailDTO deleteRoomType(String providerId, Long roomTypeId) {
        UserAuthProvider userAuthProvider = getUserAuthProvider(providerId);
        RoomTypes roomType = getRoomType(roomTypeId);

        ensureHostHasAccommodation(userAuthProvider, roomType.getAccommodation().getAccommodationId());

        // roomTypeRepository.delete(roomType);
        roomType.setIsDeleted(true);
        roomTypeRepository.save(roomType);

        return mapToRoomTypeDetailDTO(roomType);
    }

    @Transactional
    public RoomTypeDetailDTO updateRoomType(String providerId, Long roomTypeId,
            RoomTypeRequestDTO roomTypeRequestDTO) {
        UserAuthProvider userAuthProvider = getUserAuthProvider(providerId);
        RoomTypes roomType = getRoomType(roomTypeId);

        ensureHostHasAccommodation(userAuthProvider, roomType.getAccommodation().getAccommodationId());

        roomType.setName(roomTypeRequestDTO.getName());
        roomType.setPrice(roomTypeRequestDTO.getPrice());
        roomType.setDiscount(roomTypeRequestDTO.getDiscount());
        roomType.setImage(roomTypeRequestDTO.getImage());
        roomType.setImagesPreview(roomTypeRequestDTO.getImagesPreview());
        // Update other fields as necessary

        roomType.setAmenities(roomTypeRequestDTO.getAmenities());

        RoomTypes updatedRoomType = roomTypeRepository.save(roomType);
        return mapToRoomTypeDetailDTO(updatedRoomType);
    }

    @Transactional
    public RoomTypeDetailDTO patchRoomType(String providerId, Long roomTypeId, Double price, Double discount) {
        UserAuthProvider userAuthProvider = getUserAuthProvider(providerId);
        RoomTypes roomType = getRoomType(roomTypeId);

        ensureHostHasAccommodation(userAuthProvider, roomType.getAccommodation().getAccommodationId());

        roomType.setPrice(price != null ? price : roomType.getPrice());
        roomType.setDiscount(discount != null ? discount : roomType.getDiscount());

        RoomTypes updatedRoomType = roomTypeRepository.save(roomType);
        return mapToRoomTypeDetailDTO(updatedRoomType);
    }

    @Transactional
    public List<RoomSummaryDTO> addRoomsToRoomType(String providerId, Long roomTypeId,
            RoomRequestDTO roomRequestDTO) {
        UserAuthProvider userAuthProvider = getUserAuthProvider(providerId);
        RoomTypes roomType = getRoomType(roomTypeId);

        ensureHostHasAccommodation(userAuthProvider, roomType.getAccommodation().getAccommodationId());

        List<Rooms> existingRooms = roomRequestDTO.getRoomNumbers().stream().map((r) -> {
            Rooms room = new Rooms();
            room.setName(r);
            room.setRoomType(roomType);
            room.setStatus(StatusEnum.ACTIVE);
            return room;
        }).toList();
        roomType.getRooms().addAll(existingRooms);

        // roomType.addRoomsByNumbers(roomNumbers);

        roomTypeRepository.save(roomType);
        // return getRoomsByRoomType(roomTypeId);
        return roomType.getRooms().stream().map(room -> {
            // RoomSummaryDTO dto = new RoomSummaryDTO();
            // dto.setRoomId(room.getRoomId());
            // dto.setRoomNumber(room.getName());
            // dto.setIsDeleted(room.getIsDeleted());
            // return dto;
            return RoomSummaryDTO.builder()
                    .roomId(room.getRoomId())
                    .roomNumber(room.getName())
                    .isDeleted(room.getIsDeleted())
                    .build();
        }).toList();
    }

    @Transactional
    public List<RoomSummaryDTO> deleteRoomsFromRoomType(String providerId, Long roomTypeId, List<Long> roomIds) {
        UserAuthProvider userAuthProvider = getUserAuthProvider(providerId);
        RoomTypes roomType = getRoomType(roomTypeId);

        ensureHostHasAccommodation(userAuthProvider, roomType.getAccommodation().getAccommodationId());

        // List<Rooms> roomsToRemove = roomType.getRooms().stream()
        // .filter(room -> roomIds.contains(room.getRoomId()))
        // .toList();

        // roomType.getRooms().removeAll(roomsToRemove);

        roomType.getRooms().forEach((e) -> {
            if (roomIds.contains(e.getRoomId())) {
                e.setIsDeleted(true);
                // e.setStatus(StatusEnum.INACTIVE);
            }
        });

        roomTypeRepository.save(roomType);
        // return getRoomsByRoomType(roomTypeId);
        return roomType.getRooms().stream().map(room -> {
            // RoomSummaryDTO dto = new RoomSummaryDTO();
            // dto.setRoomId(room.getRoomId());
            // dto.setRoomNumber(room.getName());
            // dto.setIsDeleted(room.getIsDeleted());
            // return dto;
            return RoomSummaryDTO.builder()
                    .roomId(room.getRoomId())
                    .roomNumber(room.getName())
                    .isDeleted(room.getIsDeleted())
                    .build();
        }).toList();
    }

    public List<RoomSummaryDTO> getRoomsByRoomType(Long roomTypeId) {
        RoomTypes roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new NotFoundException("Room type not found with id: " + roomTypeId));

        return roomType.getRooms().stream().map(room -> {
            // RoomSummaryDTO dto = new RoomSummaryDTO();
            // dto.setRoomId(room.getRoomId());
            // dto.setRoomNumber(room.getName());
            // dto.setIsDeleted(room.getIsDeleted());
            // return dto;
            return RoomSummaryDTO.builder()
                    .roomId(room.getRoomId())
                    .roomNumber(room.getName())
                    .isDeleted(room.getIsDeleted())
                    .build();
        }).toList();
    }

    private RoomTypeDetailDTO mapToRoomTypeDetailDTO(RoomTypes roomType) {

        // List<AmentiesResponseDTO> amenitiesDTOs =
        // roomType.getAmenities().stream().map((element) -> {
        // return AmentiesResponseDTO.builder()
        // .amentiesId(element.getAmentiesId())
        // .amentiesName(element.getAmentiesName())
        // .build();
        // }).toList();

        return RoomTypeDetailDTO.builder()
                .roomtypeId(roomType.getRoomtypeId())
                .name(roomType.getName())
                .star(roomType.getStar())
                .image(roomType.getImage())
                .imagesPreview(roomType.getImagesPreview())
                .price(roomType.getPrice())
                .amenities(roomType.getAmenities())
                .localtion(roomType.getAccommodation().getAddress())
                .build();
    }

    public List<RoomTypeSummaryDTO> getRoomTypesByAccommodation(
            Long accommodationId,
            Pageable pageable) {

        List<RoomTypes> roomTypes = roomTypeRepository
                .findByAccommodation_AccommodationIdAndIsDeletedFalse(accommodationId, pageable)
                .toList();

        return roomTypes.stream().map(roomType -> RoomTypeSummaryDTO.builder()
                .roomtypeId(roomType.getRoomtypeId())
                .name(roomType.getName())
                .star(roomType.getStar())
                .price(roomType.getPrice())
                .image(roomType.getImage())
                .address(roomType.getAccommodation().getAddress()) // Thêm địa chỉ vào DTO
                .build()).toList();
    }

    private UserAuthProvider getUserAuthProvider(String providerId) {
        return userAuthProviderRepository.findByProviderUserId(providerId)
                .orElseThrow(() -> new NotFoundException(
                        "User auth provider not found with providerId: " + providerId));
    }

    private Accommodations getAccommodation(Long accommodationId) {
        return accommodationRepository.findById(accommodationId)
                .orElseThrow(() -> new NotFoundException("Accommodation not found with id: " + accommodationId));
    }

    private RoomTypes getRoomType(Long roomTypeId) {
        return roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new NotFoundException("Room type not found with id: " + roomTypeId));
    }

    private void ensureHostHasAccommodation(UserAuthProvider userAuthProvider, Long accommodationId) {
        if (userAuthProvider.getUser() == null) {
            throw new AccessDeniedException("Accommodation not found with id: " + accommodationId + " for the user.");
        }

        List<AccommodationStaff> staffAssignments = userAuthProvider.getUser()
                .getAccommodationStaffs();

        boolean hasAccess = staffAssignments != null && staffAssignments.stream()
                .map(staff -> staff.getAccommodation().getAccommodationId())
                .anyMatch(id -> id.equals(accommodationId));

        if (!hasAccess) {
            throw new AccessDeniedException("Accommodation not found with id: " + accommodationId + " for the user.");
        }
    }

}

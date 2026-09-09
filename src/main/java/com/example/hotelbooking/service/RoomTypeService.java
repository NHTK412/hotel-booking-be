package com.example.hotelbooking.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.hotelbooking.dto.room.RoomRequestDTO;
import com.example.hotelbooking.dto.room.RoomSummaryDTO;
import com.example.hotelbooking.dto.roomtype.RoomTypeDetailDTO;
import com.example.hotelbooking.dto.roomtype.RoomTypeRequestDTO;
import com.example.hotelbooking.dto.roomtype.RoomTypeSummaryDTO;
import com.example.hotelbooking.enums.StatusEnum;
import com.example.hotelbooking.exception.AccessDeniedException;
import com.example.hotelbooking.exception.NotFoundException;
import com.example.hotelbooking.model.Accommodation;
import com.example.hotelbooking.model.AccommodationStaff;
import com.example.hotelbooking.model.Room;
import com.example.hotelbooking.model.RoomType;
import com.example.hotelbooking.model.UserAuthProvider;
import com.example.hotelbooking.repository.AccommodationRepository;
import com.example.hotelbooking.repository.RoomTypeRepository;
import com.example.hotelbooking.repository.UserAuthProviderRepository;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class RoomTypeService {

    private final RoomTypeRepository roomTypeRepository;
    private final UserAuthProviderRepository userAuthProviderRepository;
    private final AccommodationRepository accommodationRepository;
    private final FileUploadService fileUploadService;

    public RoomTypeService(RoomTypeRepository roomTypeRepository,
            UserAuthProviderRepository userAuthProviderRepository,
            AccommodationRepository accommodationRepository,
            FileUploadService fileUploadService) {
        this.roomTypeRepository = roomTypeRepository;
        this.userAuthProviderRepository = userAuthProviderRepository;
        this.accommodationRepository = accommodationRepository;
        this.fileUploadService = fileUploadService;
    }

    public RoomTypeDetailDTO getRoomTypeById(Long roomTypeId) {
        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new NotFoundException("Room type not found with id: " + roomTypeId));

        return mapToRoomTypeDetailDTO(roomType);
    }

    public List<RoomTypeSummaryDTO> getAllRoomTypes(
            Long locationId,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            Integer capacity,
            Integer bedroom,
            Pageable pageable) {

        LocalDateTime checkInAt = checkInDate != null ? checkInDate.atTime(14, 0, 0) : null;
        LocalDateTime checkOutAt = checkOutDate != null ? checkOutDate.atTime(12, 0, 0) : null;

        List<RoomType> roomTypes = roomTypeRepository.findAvailableRoomTypes(
                locationId,
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
                .address(roomType.getAccommodation().getAccommodationName())
                .discount(roomType.getDiscount())
                .build()).toList();
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

        Accommodation accommodation = getAccommodation(accommodationId);

        RoomType roomType = new RoomType();
        roomType.setName(roomTypeRequestDTO.getName());
        roomType.setStar(0);
        roomType.setPrice(roomTypeRequestDTO.getPrice());
        roomType.setDiscount(roomTypeRequestDTO.getDiscount());

        if (roomTypeRequestDTO.getImage() != null) {
            roomType.setImage(roomTypeRequestDTO.getImage());
            fileUploadService.deleteFile(roomTypeRequestDTO.getImage());
        }

        if (roomTypeRequestDTO.getImagesPreview() != null) {
            roomType.setImagesPreview(roomTypeRequestDTO.getImagesPreview());
            roomTypeRequestDTO.getImagesPreview().forEach(fileUploadService::deleteFile);
        }
        roomType.setDescription(roomTypeRequestDTO.getDescription());
        roomType.setCapacity(roomTypeRequestDTO.getCapacity());
        roomType.setBedroom(roomTypeRequestDTO.getBedroom());
        roomType.setAmenities(roomTypeRequestDTO.getAmenities());
        roomType.setAccommodation(accommodation);

        RoomType savedRoomType = roomTypeRepository.save(roomType);
        return mapToRoomTypeDetailDTO(savedRoomType);
    }

    @Transactional
    public RoomTypeDetailDTO deleteRoomType(String providerId, Long roomTypeId) {
        UserAuthProvider userAuthProvider = getUserAuthProvider(providerId);
        RoomType roomType = getRoomType(roomTypeId);

        ensureHostHasAccommodation(userAuthProvider, roomType.getAccommodation().getAccommodationId());

        roomType.setIsDeleted(true);
        roomTypeRepository.save(roomType);

        return mapToRoomTypeDetailDTO(roomType);
    }

    @Transactional
    public RoomTypeDetailDTO updateRoomType(String providerId, Long roomTypeId,
            RoomTypeRequestDTO roomTypeRequestDTO) {
        UserAuthProvider userAuthProvider = getUserAuthProvider(providerId);
        RoomType roomType = getRoomType(roomTypeId);

        ensureHostHasAccommodation(userAuthProvider, roomType.getAccommodation().getAccommodationId());

        roomType.setName(roomTypeRequestDTO.getName());
        roomType.setPrice(roomTypeRequestDTO.getPrice());
        roomType.setDiscount(roomTypeRequestDTO.getDiscount());

        if (roomType.getImage() != null) {
            fileUploadService.deleteFile(roomType.getImage());
        }
        if (roomType.getImagesPreview() != null) {
            roomType.getImagesPreview().forEach(fileUploadService::deleteFile);
        }

        if (roomTypeRequestDTO.getImage() != null) {
            roomType.setImage(roomTypeRequestDTO.getImage());
            fileUploadService.deleteFileByPublicId(roomTypeRequestDTO.getImage());
        }

        if (roomTypeRequestDTO.getImagesPreview() != null) {
            roomType.setImagesPreview(roomTypeRequestDTO.getImagesPreview());
            roomTypeRequestDTO.getImagesPreview().forEach(fileUploadService::deleteFileByPublicId);
        }

        roomType.setAmenities(roomTypeRequestDTO.getAmenities());

        RoomType updatedRoomType = roomTypeRepository.save(roomType);
        return mapToRoomTypeDetailDTO(updatedRoomType);
    }

    @Transactional
    public RoomTypeDetailDTO patchRoomType(String providerId, Long roomTypeId, Double price, Double discount) {
        UserAuthProvider userAuthProvider = getUserAuthProvider(providerId);
        RoomType roomType = getRoomType(roomTypeId);

        ensureHostHasAccommodation(userAuthProvider, roomType.getAccommodation().getAccommodationId());

        roomType.setPrice(price != null ? price : roomType.getPrice());
        roomType.setDiscount(discount != null ? discount : roomType.getDiscount());

        RoomType updatedRoomType = roomTypeRepository.save(roomType);
        return mapToRoomTypeDetailDTO(updatedRoomType);
    }

    @Transactional
    public List<RoomSummaryDTO> addRoomsToRoomType(String providerId, Long roomTypeId,
            RoomRequestDTO roomRequestDTO) {
        UserAuthProvider userAuthProvider = getUserAuthProvider(providerId);
        RoomType roomType = getRoomType(roomTypeId);

        ensureHostHasAccommodation(userAuthProvider, roomType.getAccommodation().getAccommodationId());

        List<Room> existingRooms = roomRequestDTO.getRoomNumbers().stream().map(r -> {
            Room room = new Room();
            room.setName(r);
            room.setRoomType(roomType);
            room.setStatus(StatusEnum.ACTIVE);
            return room;
        }).toList();
        roomType.getRooms().addAll(existingRooms);

        roomTypeRepository.save(roomType);

        return roomType.getRooms().stream().map(room -> RoomSummaryDTO.builder()
                .roomId(room.getRoomId())
                .roomNumber(room.getName())
                .isDeleted(room.getIsDeleted())
                .build()).toList();
    }

    @Transactional
    public List<RoomSummaryDTO> deleteRoomsFromRoomType(String providerId, Long roomTypeId, List<Long> roomIds) {
        UserAuthProvider userAuthProvider = getUserAuthProvider(providerId);
        RoomType roomType = getRoomType(roomTypeId);

        ensureHostHasAccommodation(userAuthProvider, roomType.getAccommodation().getAccommodationId());

        roomType.getRooms().forEach(e -> {
            if (roomIds.contains(e.getRoomId())) {
                e.setIsDeleted(true);
            }
        });

        roomTypeRepository.save(roomType);

        return roomType.getRooms().stream().map(room -> RoomSummaryDTO.builder()
                .roomId(room.getRoomId())
                .roomNumber(room.getName())
                .isDeleted(room.getIsDeleted())
                .build()).toList();
    }

    public List<RoomSummaryDTO> getRoomsByRoomType(Long roomTypeId) {
        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new NotFoundException("Room type not found with id: " + roomTypeId));

        return roomType.getRooms().stream().map(room -> RoomSummaryDTO.builder()
                .roomId(room.getRoomId())
                .roomNumber(room.getName())
                .isDeleted(room.getIsDeleted())
                .build()).toList();
    }

    private RoomTypeDetailDTO mapToRoomTypeDetailDTO(RoomType roomType) {
        return RoomTypeDetailDTO.builder()
                .roomtypeId(roomType.getRoomtypeId())
                .name(roomType.getName())
                .star(roomType.getStar())
                .image(roomType.getImage())
                .imagesPreview(roomType.getImagesPreview())
                .price(roomType.getPrice())
                .amenities(roomType.getAmenities())
                .localtion(roomType.getAccommodation().getAddress())
                .bedroom(roomType.getBedroom())
                .description(roomType.getDescription())
                .capacity(roomType.getCapacity())
                .discount(roomType.getDiscount())
                .build();
    }

    public List<RoomTypeSummaryDTO> getRoomTypesByAccommodation(Long accommodationId, Pageable pageable) {
        List<RoomType> roomTypes = roomTypeRepository
                .findByAccommodation_AccommodationIdAndIsDeletedFalse(accommodationId, pageable)
                .toList();

        return roomTypes.stream().map(this::mapToRoomTypeSummaryDTO).toList();
    }

    public List<RoomTypeSummaryDTO> getHostRoomTypes(String providerId, Long accommodationId, Pageable pageable) {
        UserAuthProvider userAuthProvider = getUserAuthProvider(providerId);
        List<AccommodationStaff> staffs = userAuthProvider.getUser().getAccommodationStaffs();
        if (staffs == null || staffs.isEmpty()) {
            return List.of();
        }

        List<Long> managedAccommodationIds = staffs.stream()
                .map(s -> s.getAccommodation().getAccommodationId())
                .distinct()
                .toList();

        if (accommodationId != null) {
            if (!managedAccommodationIds.contains(accommodationId)) {
                throw new AccessDeniedException("Accommodation not found with id: " + accommodationId + " for the user.");
            }
            List<RoomType> roomTypes = roomTypeRepository
                    .findByAccommodation_AccommodationIdAndIsDeletedFalse(accommodationId, pageable)
                    .toList();
            return roomTypes.stream().map(this::mapToRoomTypeSummaryDTO).toList();
        } else {
            List<RoomType> roomTypes = roomTypeRepository
                    .findByAccommodation_AccommodationIdInAndIsDeletedFalse(managedAccommodationIds, pageable)
                    .toList();
            return roomTypes.stream().map(this::mapToRoomTypeSummaryDTO).toList();
        }
    }

    private RoomTypeSummaryDTO mapToRoomTypeSummaryDTO(RoomType roomType) {
        Accommodation acc = roomType.getAccommodation();
        return RoomTypeSummaryDTO.builder()
                .roomtypeId(roomType.getRoomtypeId())
                .name(roomType.getName())
                .star(roomType.getStar())
                .price(roomType.getPrice())
                .image(roomType.getImage())
                .discount(roomType.getDiscount())
                .address(acc != null ? acc.getAddress() : null)
                .accommodationId(acc != null ? acc.getAccommodationId() : null)
                .accommodationName(acc != null ? acc.getAccommodationName() : null)
                .build();
    }

    private UserAuthProvider getUserAuthProvider(String providerId) {
        return userAuthProviderRepository.findByProviderUserId(providerId)
                .orElseThrow(() -> new NotFoundException(
                        "User auth provider not found with providerId: " + providerId));
    }

    private Accommodation getAccommodation(Long accommodationId) {
        return accommodationRepository.findById(accommodationId)
                .orElseThrow(() -> new NotFoundException("Accommodation not found with id: " + accommodationId));
    }

    private RoomType getRoomType(Long roomTypeId) {
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

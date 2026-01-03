package com.example.hotelbooking.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.hotelbooking.dto.amenties.AmentiesResponseDTO;
import com.example.hotelbooking.dto.roomtype.RoomTypeDetailDTO;
import com.example.hotelbooking.exception.customer.NotFoundException;
import com.example.hotelbooking.model.RoomTypes;
import com.example.hotelbooking.repository.RoomTypeRepository;

@Service
public class RoomTypeService {

    private final RoomTypeRepository roomTypeRepository;

    public RoomTypeService(RoomTypeRepository roomTypeRepository) {
        this.roomTypeRepository = roomTypeRepository;
    }

    public RoomTypeDetailDTO getRoomTypeById(Long roomTypeId) {
        RoomTypes roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new NotFoundException("Room type not found with id: " + roomTypeId));

        return mapToRoomTypeDetailDTO(roomType);
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
}

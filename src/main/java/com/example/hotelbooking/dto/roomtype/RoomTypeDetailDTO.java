package com.example.hotelbooking.dto.roomtype;

import java.util.List;

import com.example.hotelbooking.dto.amenties.AmentiesResponseDTO;
import com.example.hotelbooking.enums.AmenityEnum;
// import com.example.hotelbooking.model.Amenties;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class RoomTypeDetailDTO {
    private Long roomtypeId;
    private String name;
    private Integer star;
    private Double price;
    private List<String> imagesPreview;
    private String image;
    // private List<AmentiesResponseDTO> amenities;
    private List<AmenityEnum> amenities;
    private String localtion;
}

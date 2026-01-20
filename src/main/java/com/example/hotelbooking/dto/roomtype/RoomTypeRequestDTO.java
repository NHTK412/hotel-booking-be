package com.example.hotelbooking.dto.roomtype;

import java.util.List;

import com.example.hotelbooking.enums.AmenityEnum;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoomTypeRequestDTO {

    private String name;
    private Double price;
    private Double discount;
    private List<String> imagesPreview;
    private String image;
    private List<AmenityEnum> amenities;
    private Long accommodationId;
    private Integer capacity;
    private Integer bedroom;

    // private String localtion; // Nghĩa là ở đâu đó
}

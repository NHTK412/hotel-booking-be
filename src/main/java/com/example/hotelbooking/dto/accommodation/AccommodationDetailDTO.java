package com.example.hotelbooking.dto.accommodation;

import java.util.List;

import com.example.hotelbooking.dto.roomtype.RoomTypeSummaryDTO;
import com.example.hotelbooking.enums.AccommodationTypeEnum;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AccommodationDetailDTO {

    private Long accommodationId;
    private String accommodationName;
    private String description;
    private String address;
    private String city;
    private Double latitude;
    private Double longitude;
    private AccommodationTypeEnum type;
    private List<RoomTypeSummaryDTO> roomTypes;
    private Double starRating;

}

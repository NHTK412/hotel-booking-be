package com.example.hotelbooking.dto.accommodation;

import com.example.hotelbooking.enums.AccommodationTypeEnum;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccommodationRequestDTO {

    private String accommodationName;
    private String description;
    private String address;
    private String city;
    private Double latitude;
    private Double longitude;
    private String image;
    private AccommodationTypeEnum type;

}

package com.example.hotelbooking.dto.accommodation;

import com.example.hotelbooking.enums.AccommodationStaffRoleEnum;
import com.example.hotelbooking.enums.AccommodationTypeEnum;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AccommodationInfoHostDTO {

    private Long accommodationId;
    private String accommodationName;
    private String address;
    private String image;
    // private Double averageRating;
    // private Double minPricePerNight;
    // private Double discountMinPricePerNight; // Giá đã giảm
    private AccommodationTypeEnum type;
    private AccommodationStaffRoleEnum staffRole;

    private Double lat;
    private Double lng;
    private Boolean isDeleted;
}

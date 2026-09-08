package com.example.hotelbooking.dto.accommodation;

import com.example.hotelbooking.enums.AccommodationTypeEnum;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AccommodationSummaryDTO {

    private Long accommodationId;
    private String accommodationName;
    private String address;
    private String image;
    private Double averageRating;
    private Double minPricePerNight;
    private Double discountMinPricePerNight; // Giá đã giảm
    private AccommodationTypeEnum type;

    private Double lat;
    private Double lng;
    private Boolean isDeleted;


    // private Double starRating;
    // private Double minPrice;
    // private Double distanceFromCityCenter;

}

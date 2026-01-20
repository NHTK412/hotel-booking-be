package com.example.hotelbooking.dto.location;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class LocationResponseDTO {
    private Long locationId;
    private String provinceName;
    private String districtName;
    private Double latitude;
    private Double longitude;
    private String searchVector; // Cột tổng hợp để search nhanh: "Quận 1, Hồ Chí Minh"

}

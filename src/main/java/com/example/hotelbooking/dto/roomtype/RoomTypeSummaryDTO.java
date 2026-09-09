package com.example.hotelbooking.dto.roomtype;

import com.example.hotelbooking.enums.StatusEnum;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class RoomTypeSummaryDTO {

    private Long roomtypeId;
    private String name;
    private Integer star;
    private Double price;
    private Double discount;
    private String image;

    // Thêm địa chỉ phòng
    private String address;

    private Long accommodationId;
    private String accommodationName;
    private StatusEnum status;
    private Boolean isDeleted;
}

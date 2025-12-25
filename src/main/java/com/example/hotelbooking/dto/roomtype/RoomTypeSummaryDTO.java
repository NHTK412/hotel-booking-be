package com.example.hotelbooking.dto.roomtype;

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
    private String image;
}

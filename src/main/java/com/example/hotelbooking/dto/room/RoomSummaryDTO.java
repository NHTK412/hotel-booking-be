package com.example.hotelbooking.dto.room;

import com.example.hotelbooking.enums.StatusEnum;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class RoomSummaryDTO {

    private Long roomId;

    private String roomNumber;

    private Boolean isDeleted;

    private StatusEnum status;
}

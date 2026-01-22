package com.example.hotelbooking.dto.room;

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
}

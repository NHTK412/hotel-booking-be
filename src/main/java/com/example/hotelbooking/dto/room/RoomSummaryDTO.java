package com.example.hotelbooking.dto.room;

import com.example.hotelbooking.enums.StatusEnum;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoomSummaryDTO {

    private Long roomId;

    private String roomNumber;

    private StatusEnum status;

    private Boolean isDeleted;
}

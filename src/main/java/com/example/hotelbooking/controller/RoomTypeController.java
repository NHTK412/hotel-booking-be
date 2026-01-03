package com.example.hotelbooking.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.hotelbooking.dto.roomtype.RoomTypeDetailDTO;
import com.example.hotelbooking.service.RoomTypeService;
import com.example.hotelbooking.util.ApiResponse;

@RestController
@RequestMapping("/roomtypes")
public class RoomTypeController {

    final RoomTypeService roomTypeService;

    public RoomTypeController(RoomTypeService roomTypeService) {
        this.roomTypeService = roomTypeService;
    }

    @GetMapping("/{roomTypeId}")
    public ResponseEntity<ApiResponse<RoomTypeDetailDTO>> getRoomTypeById(@PathVariable Long roomTypeId) {
        RoomTypeDetailDTO updatedRoomType = roomTypeService
                .getRoomTypeById(roomTypeId);

        ApiResponse<RoomTypeDetailDTO> response = new ApiResponse<>(true,
                "Room type retrieved successfully",
                updatedRoomType);

        return ResponseEntity.ok(response);
    }

}

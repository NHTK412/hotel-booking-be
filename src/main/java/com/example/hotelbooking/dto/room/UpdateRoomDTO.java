package com.example.hotelbooking.dto.room;

import com.example.hotelbooking.enums.StatusEnum;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request cập nhật thông tin phòng vật lý")
public class UpdateRoomDTO {

    @NotBlank(message = "Số phòng không được để trống")
    @Schema(description = "Tên hoặc số phòng vật lý", example = "101A")
    private String roomNumber;

    @Schema(description = "Trạng thái hoạt động của phòng (ACTIVE, INACTIVE)", example = "ACTIVE")
    private StatusEnum status;
}

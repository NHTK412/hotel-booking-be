package com.example.hotelbooking.dto.room;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
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
@Schema(description = "Request tạo danh sách phòng vật lý")
public class RoomRequestDTO {

    @NotEmpty(message = "Danh sách số phòng không được để trống")
    @Schema(description = "Danh sách tên/số phòng", example = "[\"101\", \"102\", \"103\"]")
    private List<String> roomNumbers;
}

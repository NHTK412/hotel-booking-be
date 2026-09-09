package com.example.hotelbooking.dto.roomtype;

import java.util.List;

import com.example.hotelbooking.enums.AmenityEnum;
import com.example.hotelbooking.enums.StatusEnum;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
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
@Schema(description = "Request tạo hoặc cập nhật loại phòng")
public class RoomTypeRequestDTO {

    @NotBlank(message = "Tên loại phòng không được để trống")
    @Schema(description = "Tên hạng phòng", example = "Deluxe Ocean View")
    private String name;

    @Schema(description = "Trạng thái hoạt động của loại phòng (ACTIVE: hoạt động, INACTIVE: tạm ngưng nhận khách)", example = "ACTIVE")
    private StatusEnum status;

    @NotNull(message = "Giá phòng không được để trống")
    @Positive(message = "Giá phòng phải lớn hơn 0")
    @Schema(description = "Giá gốc 1 đêm (VNĐ)", example = "1500000.0")
    private Double price;

    @PositiveOrZero(message = "Giảm giá phải lớn hơn hoặc bằng 0")
    @Schema(description = "Số tiền giảm giá (VNĐ)", example = "200000.0")
    private Double discount;

    @Schema(description = "Danh sách link ảnh preview phòng")
    private List<String> imagesPreview;

    @Schema(description = "Ảnh đại diện chính của loại phòng")
    private String image;

    @Schema(description = "Danh sách tiện ích đi kèm (WIFI, POOL, TV, ...)")
    private List<AmenityEnum> amenities;

    @NotNull(message = "Mã khách sạn không được để trống")
    @Positive(message = "Mã khách sạn phải lớn hơn 0")
    @Schema(description = "ID khách sạn sở hữu loại phòng này", example = "1")
    private Long accommodationId;

    @NotNull(message = "Sức chứa tối đa không được để trống")
    @Min(value = 1, message = "Sức chứa tối thiểu 1 người")
    @Schema(description = "Số lượng khách tối đa", example = "2")
    private Integer capacity;

    @NotNull(message = "Số phòng ngủ không được để trống")
    @Min(value = 1, message = "Số phòng ngủ tối thiểu 1 phòng")
    @Schema(description = "Số lượng phòng ngủ", example = "1")
    private Integer bedroom;

    @Schema(description = "Mô tả chi tiết phòng", example = "Phòng view biển ngắm hoàng hôn...")
    private String description;
}

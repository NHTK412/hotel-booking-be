package com.example.hotelbooking.dto.accommodation;

import com.example.hotelbooking.enums.AccommodationTypeEnum;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
@Schema(description = "Request tạo hoặc cập nhật thông tin khách sạn")
public class AccommodationRequestDTO {

    @NotBlank(message = "Tên khách sạn không được để trống")
    @Schema(description = "Tên khách sạn / resort", example = "Grand Luxury Hotel")
    private String accommodationName;

    @Schema(description = "Mô tả tổng quan về khách sạn", example = "Khách sạn 5 sao cao cấp ven biển...")
    private String description;

    @NotBlank(message = "Địa chỉ chi tiết không được để trống")
    @Schema(description = "Địa chỉ số nhà, đường", example = "123 Đường Lê Lợi, Bến Nghé")
    private String address;

    @Schema(description = "Tên thành phố", example = "Hồ Chí Minh")
    private String city;

    @Schema(description = "Tọa độ vĩ độ (Latitude)", example = "10.7769")
    private Double latitude;

    @Schema(description = "Tọa độ kinh độ (Longitude)", example = "106.7009")
    private Double longitude;

    @Schema(description = "Ảnh đại diện chính của khách sạn")
    private String image;

    @NotNull(message = "Loại hình lưu trú không được để trống")
    @Schema(description = "Loại hình (HOTEL, RESORT, HOMESTAY, VILLA, ...)", example = "HOTEL")
    private AccommodationTypeEnum type;

    @NotNull(message = "Mã vị trí / địa điểm không được để trống")
    @Positive(message = "Mã vị trí phải lớn hơn 0")
    @Schema(description = "ID địa điểm hành chính (Location ID)", example = "1")
    private Long locationId;
}

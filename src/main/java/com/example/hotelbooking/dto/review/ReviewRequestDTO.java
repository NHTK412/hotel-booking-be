package com.example.hotelbooking.dto.review;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
@Schema(description = "Request gửi đánh giá đơn đặt phòng")
public class ReviewRequestDTO {

    @NotNull(message = "Mã đơn đặt phòng không được để trống")
    @Positive(message = "Mã đơn đặt phòng phải lớn hơn 0")
    @Schema(description = "ID đơn đặt phòng", example = "101")
    private Long bookingId;

    @NotNull(message = "Điểm đánh giá không được để trống")
    @Min(value = 1, message = "Điểm đánh giá tối thiểu 1 sao")
    @Max(value = 5, message = "Điểm đánh giá tối đa 5 sao")
    @Schema(description = "Số sao đánh giá (1 - 5)", example = "5")
    private Integer rating;

    @Size(max = 2000, message = "Nội dung nhận xét tối đa 2000 ký tự")
    @Schema(description = "Nội dung phản hồi nhận xét", example = "Phòng rất đẹp, dịch vụ tuyệt vời!")
    private String comment;
}

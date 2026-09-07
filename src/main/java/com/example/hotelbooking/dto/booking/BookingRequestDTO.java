package com.example.hotelbooking.dto.booking;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request tạo đơn đặt phòng mới")
public class BookingRequestDTO {

    @NotBlank(message = "Tên khách hàng không được để trống")
    @Schema(description = "Tên người nhận phòng", example = "Nguyễn Văn A")
    private String customerName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0|\\+84)[3|5|7|8|9][0-9]{8}$", message = "Số điện thoại không đúng định dạng Việt Nam")
    @Schema(description = "Số điện thoại liên hệ", example = "0901234567")
    private String customerPhone;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Schema(description = "Email nhận thông tin đặt phòng", example = "customer@gmail.com")
    private String customerEmail;

    @NotNull(message = "Ngày nhận phòng không được để trống")
    @FutureOrPresent(message = "Ngày nhận phòng không thể trong quá khứ")
    @Schema(description = "Ngày Check-in (YYYY-MM-DD)", example = "2026-10-01")
    private LocalDate checkInDate;

    @NotNull(message = "Ngày trả phòng không được để trống")
    @Future(message = "Ngày trả phòng phải ở tương lai")
    @Schema(description = "Ngày Check-out (YYYY-MM-DD)", example = "2026-10-03")
    private LocalDate checkOutDate;

    @NotNull(message = "Mã loại phòng không được để trống")
    @Positive(message = "Mã loại phòng phải lớn hơn 0")
    @Schema(description = "ID loại phòng cần đặt", example = "1")
    private Long roomTypeId;
}

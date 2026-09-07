package com.example.hotelbooking.dto.user;

import java.time.LocalDateTime;

import com.example.hotelbooking.enums.AccommodationStaffRoleEnum;
import com.example.hotelbooking.enums.GenderEnum;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Schema(description = "Request đăng ký quản lý / nhân viên khách sạn (Host/Staff)")
public class CreateHostDTO {

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0|\\+84)[3|5|7|8|9][0-9]{8}$", message = "Số điện thoại không đúng định dạng Việt Nam")
    @Schema(description = "Số điện thoại liên hệ", example = "0901234567")
    private String phone;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Schema(description = "Địa chỉ email", example = "host@hotel.com")
    private String email;

    @NotBlank(message = "Họ tên không được để trống")
    @Schema(description = "Họ và tên nhân viên", example = "Trần Thị B")
    private String name;

    @Schema(description = "Ngày sinh")
    private LocalDateTime birthday;

    @Schema(description = "Giới tính (MALE, FEMALE, OTHER)")
    private GenderEnum gender;

    @Schema(description = "Địa chỉ cư trú")
    private String address;

    @Schema(description = "Link ảnh đại diện")
    private String avatarUrl;

    @NotNull(message = "Mã khách sạn không được để trống")
    @Positive(message = "Mã khách sạn phải lớn hơn 0")
    @Schema(description = "ID khách sạn làm việc", example = "1")
    private Long accommodationId;

    @NotNull(message = "Vai trò nhân viên không được để trống")
    @Schema(description = "Vai trò (HOST, RECEPTIONIST, STAFF)", example = "HOST")
    private AccommodationStaffRoleEnum hostRole;
}

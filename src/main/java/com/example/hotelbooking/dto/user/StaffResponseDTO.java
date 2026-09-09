package com.example.hotelbooking.dto.user;

import java.time.LocalDateTime;

import com.example.hotelbooking.enums.AccommodationStaffRoleEnum;
import com.example.hotelbooking.enums.AccommodationTypeEnum;
import com.example.hotelbooking.enums.UserRoleEnum;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Thông tin chi tiết nhân sự / quản lý cơ sở lưu trú")
public class StaffResponseDTO {

    @Schema(description = "ID người dùng", example = "10")
    private Long id;

    @Schema(description = "ID người dùng (alias)", example = "10")
    private Long userId;

    @Schema(description = "Họ và tên", example = "Nguyễn Văn Quản Lý")
    private String name;

    @Schema(description = "Email đăng nhập", example = "host@hotel.com")
    private String email;

    @Schema(description = "Số điện thoại liên hệ", example = "0901234567")
    private String phone;

    @Schema(description = "Ngày sinh")
    private LocalDateTime birthday;

    @Schema(description = "Giới tính", example = "Nam")
    private String gender;

    @Schema(description = "Địa chỉ liên hệ", example = "123 Lê Lợi, Q1, TP.HCM")
    private String address;

    @Schema(description = "Link ảnh đại diện")
    private String avatarUrl;

    @Schema(description = "Vai trò người dùng trong hệ thống (ROLE_HOST / ROLE_ADMIN)", example = "ROLE_HOST")
    private UserRoleEnum systemRole;

    @Schema(description = "Vai trò tại cơ sở lưu trú (ROLE_MANAGER / ROLE_RECEPTIONIST)", example = "ROLE_MANAGER")
    private AccommodationStaffRoleEnum role;

    @Schema(description = "Vai trò tại cơ sở lưu trú (alias)", example = "ROLE_MANAGER")
    private AccommodationStaffRoleEnum staffRole;

    @Schema(description = "Trạng thái hoạt động tài khoản", example = "true")
    private Boolean isActive;

    @Schema(description = "Trạng thái tài khoản người dùng", example = "ACTIVE")
    private com.example.hotelbooking.enums.StatusEnum status;

    @Schema(description = "ID liên kết nhân sự tại cơ sở lưu trú (AccommodationStaff ID)", example = "1")
    private Long accommodationStaffId;

    @Schema(description = "Trạng thái nhân sự đã nghỉ việc hay chưa (true: đã nghỉ, false: đang làm việc)", example = "false")
    private Boolean isDeleted;

    @Schema(description = "ID cơ sở lưu trú phụ trách", example = "5")
    private Long accommodationId;

    @Schema(description = "Tên cơ sở lưu trú phụ trách", example = "Vinpearl Luxury Nha Trang")
    private String accommodationName;

    @Schema(description = "Loại hình cơ sở lưu trú", example = "RESORT")
    private AccommodationTypeEnum hotelType;

    @Schema(description = "Ngày tạo tài khoản")
    private LocalDateTime createdAt;
}

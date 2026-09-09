package com.example.hotelbooking.dto.user;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.example.hotelbooking.enums.StatusEnum;
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
@Schema(description = "Thông tin Host/Nhân sự kèm danh sách các đơn vị lưu trú trực thuộc")
public class HostGroupResponseDTO {

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

    @Schema(description = "Vai trò trong hệ thống (ROLE_HOST / ROLE_ADMIN)", example = "ROLE_HOST")
    private UserRoleEnum systemRole;

    @Schema(description = "Trạng thái tài khoản người dùng (ACTIVE: hoạt động, INACTIVE: bị khóa)", example = "ACTIVE")
    private StatusEnum status;

    @Schema(description = "Trạng thái hoạt động tài khoản", example = "true")
    private Boolean isActive;

    @Schema(description = "Tài khoản người dùng đã bị xóa mềm toàn bộ", example = "false")
    private Boolean isDeleted;

    @Schema(description = "Ngày tạo tài khoản")
    private LocalDateTime createdAt;

    @Schema(description = "Danh sách các đơn vị lưu trú trực thuộc")
    @Builder.Default
    private List<StaffAccommodationAssignmentDTO> accommodations = new ArrayList<>();
}

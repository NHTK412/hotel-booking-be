package com.example.hotelbooking.dto.user;

import java.time.LocalDateTime;

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
@Schema(description = "Thông tin phản hồi người dùng")
public class UserResponseDTO {

    @Schema(description = "ID người dùng", example = "1")
    private Long id;

    @Schema(description = "Họ và tên", example = "Nguyễn Văn A")
    private String name;

    @Schema(description = "Email", example = "user@example.com")
    private String email;

    @Schema(description = "Số điện thoại", example = "0901234567")
    private String phone;

    @Schema(description = "Ngày sinh")
    private LocalDateTime birthday;

    @Schema(description = "Giới tính", example = "Nam")
    private String gender;

    @Schema(description = "Địa chỉ", example = "123 Đường Lê Lợi, TP. Hồ Chí Minh")
    private String address;

    @Schema(description = "Link ảnh đại diện")
    private String avatarUrl;

    @Schema(description = "Vai trò hệ thống", example = "ROLE_CUSTOMER")
    private UserRoleEnum role;

    @Schema(description = "Trạng thái tài khoản (ACTIVE: hoạt động, INACTIVE: bị khóa)", example = "ACTIVE")
    private StatusEnum status;

    @Schema(description = "Tài khoản đang kích hoạt", example = "true")
    private Boolean isActive;

    @Schema(description = "Tài khoản đã bị xóa mềm", example = "false")
    private Boolean isDeleted;
}

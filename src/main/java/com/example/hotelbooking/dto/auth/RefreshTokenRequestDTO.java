package com.example.hotelbooking.dto.auth;

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
@Schema(description = "Request body chứa Refresh Token để cấp lại Access Token mới")
public class RefreshTokenRequestDTO {

    @Schema(description = "Mã Refresh Token dài hạn đã được cấp phát khi đăng nhập", example = "a1b2c3d4e5f6...")
    private String refreshToken;
}

package com.example.hotelbooking.dto.device;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Schema(description = "Request đăng ký thiết bị nhận thông báo đẩy FCM")
public class DeviceRegistrationRequest {

    @NotBlank(message = "FCM token không được để trống")
    @Schema(description = "Token thiết bị từ Firebase Cloud Messaging", example = "fcm-token-xyz...")
    private String fcmToken;

    @Schema(description = "Loại thiết bị", example = "Android")
    private String deviceType;

    @Schema(description = "Nền tảng", example = "Mobile")
    private String platform;

    @Schema(description = "Mã người dùng sở hữu thiết bị", example = "1")
    private Long userId;
}

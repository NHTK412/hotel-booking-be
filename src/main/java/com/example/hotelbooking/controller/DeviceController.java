package com.example.hotelbooking.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.hotelbooking.dto.device.DeviceRegistrationRequest;
import com.example.hotelbooking.security.CustomUserDetails;
import com.example.hotelbooking.service.DeviceService;
import com.example.hotelbooking.util.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "8. Thiết Bị & Thông Báo Đẩy (Devices & Push Notifications)", description = "Các API đăng ký mã thiết bị Firebase Cloud Messaging (FCM) và làm mới mã FCM Token để nhận thông báo đặt phòng")
@RestController
@RequestMapping("/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @Operation(summary = "Đăng ký thiết bị nhận thông báo FCM (Đã đăng nhập)", description = "Lưu mã FCM Token của thiết bị di động/trình duyệt vào hệ thống để nhận thông báo nhắc nhở Check-in")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Boolean>> registerDevice(
            @AuthenticationPrincipal CustomUserDetails customerUserDetails,
            @RequestBody DeviceRegistrationRequest request) {

        Boolean result = deviceService.registerDevice(customerUserDetails.getProviderId(), request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Đăng ký thiết bị thành công", result));
    }

    @Operation(summary = "Làm mới mã FCM Token (Đã đăng nhập)", description = "Cập nhật mã FCM Token mới khi thiết bị tự động làm mới mã")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Boolean>> refreshDevice(
            @AuthenticationPrincipal CustomUserDetails customerUserDetails,
            @RequestBody DeviceRegistrationRequest request) {

        Boolean result = deviceService.refreshDevice(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Làm mới mã thiết bị thành công", result));
    }
}

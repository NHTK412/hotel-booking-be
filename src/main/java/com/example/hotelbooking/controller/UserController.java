package com.example.hotelbooking.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.hotelbooking.dto.user.CreateHostDTO;
import com.example.hotelbooking.dto.user.UserRequestDTO;
import com.example.hotelbooking.dto.user.UserResponseDTO;
import com.example.hotelbooking.security.CustomUserDetails;
import com.example.hotelbooking.service.UserService;
import com.example.hotelbooking.util.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "7. Người Dùng & Hồ Sơ (Users & Profiles)", description = "Các API xem thông tin cá nhân, cập nhật hồ sơ người dùng và Admin tạo tài khoản Host")
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Xem thông tin người dùng theo ID (Admin / Công khai)", description = "Lấy thông tin hồ sơ cơ bản của người dùng bằng userId")
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getUserById(@PathVariable Long userId) {
        UserResponseDTO userResponseDTO = userService.getUserById(userId);
        ApiResponse<UserResponseDTO> response = new ApiResponse<>(true, "Lấy thông tin người dùng thành công", userResponseDTO);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Xem thông tin hồ sơ cá nhân hiện tại (Đã đăng nhập)", description = "Lấy toàn bộ thông tin tài khoản của người dùng đang đăng nhập qua JWT Token")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getCurrentUser(
            @AuthenticationPrincipal CustomUserDetails customerUserDetails) {
        String providerId = customerUserDetails.getUsername();
        UserResponseDTO userResponseDTO = userService.getUserByProviderId(providerId);
        ApiResponse<UserResponseDTO> response = new ApiResponse<>(true, "Lấy thông tin hồ sơ thành công", userResponseDTO);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Cập nhật thông tin hồ sơ cá nhân (Đã đăng nhập)", description = "Chỉnh sửa tên, số điện thoại, ngày sinh, giới tính, địa chỉ của người dùng hiện tại")
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateCurrentUser(
            @AuthenticationPrincipal CustomUserDetails customerUserDetails,
            @Valid @RequestBody UserRequestDTO userRequestDTO) {
        String providerId = customerUserDetails.getUsername();
        UserResponseDTO userResponseDTO = userService.updateUserByProviderId(providerId, userRequestDTO);
        ApiResponse<UserResponseDTO> response = new ApiResponse<>(true, "Cập nhật hồ sơ thành công", userResponseDTO);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Đăng ký tài khoản Chủ khách sạn / Lễ tân (Admin & Host)", description = "Admin có quyền cấp tài khoản Chủ khách sạn (ROLE_MANAGER) và Lễ tân (ROLE_RECEPTIONIST) cho mọi khách sạn. Chủ khách sạn có quyền cấp tài khoản Lễ tân cho khách sạn mình quản lý.")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOST')")
    @PostMapping("/host")
    public ResponseEntity<ApiResponse<UserResponseDTO>> registerHost(
            @AuthenticationPrincipal CustomUserDetails customerUserDetails,
            @Valid @RequestBody CreateHostDTO createHostDTO) {
        String providerId = customerUserDetails.getUsername();
        UserResponseDTO userResponseDTO = userService.registerHost(providerId, createHostDTO);
        ApiResponse<UserResponseDTO> response = new ApiResponse<>(true, "Đăng ký tài khoản thành công", userResponseDTO);
        return ResponseEntity.ok(response);
    }
}

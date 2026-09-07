package com.example.hotelbooking.controller;

import java.util.Map;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.hotelbooking.dto.auth.AuthLoginDTO;
import com.example.hotelbooking.dto.auth.AuthRegisterDTO;
import com.example.hotelbooking.dto.auth.AuthResponseDTO;
import com.example.hotelbooking.dto.auth.OauthLoginDTO;
import com.example.hotelbooking.dto.auth.RefreshTokenRequestDTO;
import com.example.hotelbooking.security.CustomUserDetails;
import com.example.hotelbooking.service.AuthService;
import com.example.hotelbooking.util.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "1. Xác Thực & Tài Khoản (Authentication)", description = "Các API đăng nhập mật khẩu cục bộ, đăng nhập OAuth Google/Facebook, đăng ký khách hàng, gửi mã OTP và đổi mật khẩu")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Đăng nhập bằng Email & Mật khẩu", description = "Dành cho Admin, Host và Customer xác thực qua email và mật khẩu cục bộ")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> login(@Valid @RequestBody AuthLoginDTO loginDTO) {
        AuthResponseDTO authResponse = authService.login(loginDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "Đăng nhập thành công", authResponse));
    }

    @Operation(summary = "Đăng nhập bằng OAuth (Google / Facebook)", description = "Dành riêng cho Khách hàng (Customer). Hệ thống xác thực chữ ký số Firebase ID Token và tự động liên kết tài khoản theo email")
    @PostMapping("/oauth")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> oauthLogin(@RequestBody OauthLoginDTO oauthLoginDTO) {
        AuthResponseDTO authResponse = authService.oauthLogin(oauthLoginDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "Đăng nhập OAuth thành công", authResponse));
    }

    @Operation(summary = "Đăng ký tài khoản Khách hàng mới", description = "Tạo tài khoản khách hàng thông thường bằng họ tên, email, số điện thoại và mật khẩu")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> register(@Valid @RequestBody AuthRegisterDTO registerDTO) {
        AuthResponseDTO authResponse = authService.register(registerDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "Đăng ký tài khoản thành công", authResponse));
    }

    @Operation(summary = "Gửi mã OTP xác thực qua Email", description = "Tạo mã OTP 4 số ngẫu nhiên có hiệu lực 5 phút lưu vào Redis và gửi qua email người dùng")
    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<Boolean>> sendOtp(@RequestParam String email) {
        Boolean otp = authService.sendOtp(email);
        return ResponseEntity.ok(new ApiResponse<>(true, "Mã OTP đã được gửi thành công", otp));
    }

    @Operation(summary = "Xác thực mã OTP", description = "Kiểm tra tính hợp lệ của mã OTP và cấp phát JWT token nếu OTP chính xác")
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyOtp(@RequestParam String email,
            @RequestParam String otp) {
        Map<String, Object> responseData = authService.verifyOtp(email, otp);
        return ResponseEntity.ok(new ApiResponse<>(true, "Kết quả xác thực OTP", responseData));
    }

    @Operation(summary = "Đặt lại mật khẩu mới", description = "Cho phép người dùng cập nhật mật khẩu mới sau khi đã xác thực danh tính")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Boolean>> resetPassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String newPassword) {
        String email = userDetails.getUsername();
        Boolean result = authService.resetPassword(email, newPassword);
        return ResponseEntity.ok(new ApiResponse<>(true, "Đặt lại mật khẩu thành công", result));
    }

    @Operation(summary = "Làm mới Access Token (Token Rotation)", description = "Nhận Refresh Token còn hiệu lực, kiểm tra đối chiếu Redis, tạo Access Token mới và cấp Refresh Token mới (thu hồi Refresh Token cũ)")
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> refreshToken(@Valid @RequestBody RefreshTokenRequestDTO requestDTO) {
        AuthResponseDTO authResponse = authService.refreshToken(requestDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "Làm mới token thành công", authResponse));
    }

    @Operation(summary = "Đăng xuất tài khoản", description = "Thu hồi Refresh Token và xóa phiên đăng nhập khỏi Redis")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Boolean>> logout(@RequestBody(required = false) RefreshTokenRequestDTO requestDTO) {
        Boolean result = authService.logout(requestDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "Đăng xuất thành công", result));
    }
}
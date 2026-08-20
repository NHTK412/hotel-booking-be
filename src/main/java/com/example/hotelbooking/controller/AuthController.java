package com.example.hotelbooking.controller;

import java.util.Map;

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
import com.example.hotelbooking.security.CustomUserDetails;
import com.example.hotelbooking.service.AuthService;
import com.example.hotelbooking.util.ApiResponse;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> login(@RequestBody AuthLoginDTO loginDTO) {
        AuthResponseDTO authResponse = authService.login(loginDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "Login successful", authResponse));
    }

    @PostMapping("/oauth")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> oauthLogin(@RequestBody OauthLoginDTO oauthLoginDTO) {
        AuthResponseDTO authResponse = authService.oauthLogin(oauthLoginDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "OAuth login successful", authResponse));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> register(@RequestBody AuthRegisterDTO registerDTO) {
        AuthResponseDTO authResponse = authService.register(registerDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "Registration successful", authResponse));
    }

    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<Boolean>> sendOtp(@RequestParam String email) {
        Boolean otp = authService.sendOtp(email);
        return ResponseEntity.ok(new ApiResponse<>(true, "OTP sent successfully", otp));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyOtp(@RequestParam String email,
            @RequestParam String otp) {
        Map<String, Object> responseData = authService.verifyOtp(email, otp);
        return ResponseEntity.ok(new ApiResponse<>(true, "OTP verification result", responseData));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Boolean>> resetPassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String newPassword) {
        String email = userDetails.getUsername();
        Boolean result = authService.resetPassword(email, newPassword);
        return ResponseEntity.ok(new ApiResponse<>(true, "Password reset successful", result));
    }
}
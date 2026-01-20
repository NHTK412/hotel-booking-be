package com.example.hotelbooking.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.hotelbooking.dto.auth.AuthLoginDTO;
import com.example.hotelbooking.dto.auth.AuthReigsterDTO;
import com.example.hotelbooking.dto.auth.AuthResponseDTO;
import com.example.hotelbooking.dto.auth.OauthLoginDTO;
import com.example.hotelbooking.security.CustomerUserDetails;
import com.example.hotelbooking.service.AuthService;
import com.example.hotelbooking.util.ApiResponse;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

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
    public ResponseEntity<ApiResponse<AuthResponseDTO>> register(@RequestBody AuthReigsterDTO registerDTO) {
        AuthResponseDTO authResponse = authService.register(registerDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "Registration successful", authResponse));
    }

    // Tạo mã OTP và gửi đến email người dùng
    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<Map<String, String>>> sendOtp(@RequestParam String email) {
        String otp = authService.sendOtp(email);
        final Map<String, String> responseData = new HashMap<>();
        responseData.put("otp", otp); // Chỉ để minh họa, không nên
        return ResponseEntity.ok(new ApiResponse<>(true, "OTP sent successfully", responseData));
    }

    // xÁC THỰC MÃ OTP
    // @PostMapping("/verify-otp")
    // public ResponseEntity<ApiResponse<Map<String, Boolean>>>
    // verifyOtp(@RequestParam String email,
    // @RequestParam String otp) {
    // boolean isValid = authService.verifyOtp(email, otp);

    // final Map<String, Boolean> responseData = new HashMap<>();
    // responseData.put("isValid", isValid);
    // return ResponseEntity.ok(new ApiResponse<>(true, "OTP verification result",
    // responseData));
    // }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyOtp(@RequestParam String email,
            @RequestParam String otp) {
        // boolean isValid = authService.verifyOtp(email, otp);

        // final Map<String, Boolean> responseData = new HashMap<>();
        // responseData.put("isValid", isValid);
        Map<String, Object> responseData = authService.verifyOtp(email, otp);
        return ResponseEntity.ok(new ApiResponse<>(true, "OTP verification result", responseData));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Boolean>> resetPassword(@AuthenticationPrincipal CustomerUserDetails userDetails,
            @RequestParam String newPassword) {

        String email = userDetails.getUsername();
        Boolean result = authService.resetPassword(email, newPassword);
        return ResponseEntity.ok(new ApiResponse<>(true, "Password reset successful", result));
    }

}
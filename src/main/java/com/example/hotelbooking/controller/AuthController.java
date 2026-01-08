package com.example.hotelbooking.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.hotelbooking.dto.auth.AuthLoginDTO;
import com.example.hotelbooking.dto.auth.AuthReigsterDTO;
import com.example.hotelbooking.dto.auth.AuthResponseDTO;
import com.example.hotelbooking.dto.auth.OauthLoginDTO;
import com.example.hotelbooking.service.AuthService;
import com.example.hotelbooking.util.ApiResponse;

import org.springframework.http.ResponseEntity;
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

}

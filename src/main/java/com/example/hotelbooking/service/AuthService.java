package com.example.hotelbooking.service;

import java.security.SecureRandom;

import org.springframework.security.crypto.codec.Hex;
import org.springframework.stereotype.Service;

import com.example.hotelbooking.dto.auth.AuthLoginDTO;
import com.example.hotelbooking.dto.auth.AuthResponseDTO;
import com.example.hotelbooking.exception.customer.InvalidCredentialsException;
import com.example.hotelbooking.model.Users;
import com.example.hotelbooking.repository.UserRepository;
import com.example.hotelbooking.util.JwtUtil;

@Service
public class AuthService {

    private final UserRepository userRepository;

    private final JwtUtil jwtUtil;

    final private SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponseDTO login(AuthLoginDTO authLoginDTO) {
        Users user = userRepository.findByEmail(authLoginDTO.getEmail())
                // .orElseThrow(() -> new RuntimeException("User not found"));
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        // BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        // if (!passwordEncoder.matches(authLoginDTO.getPassword(), user.getPassword()))
        // {
        // throw new RuntimeException("Invalid credentials");
        // }

        if (!authLoginDTO.getPassword().equals(user.getPassword())) {
            // throw new RuntimeException("Invalid credentials");
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String accessToken = jwtUtil.generateToken(user.getEmail(), user.getRole());

        byte[] refreshTokenBytes = new byte[50];
        secureRandom.nextBytes(refreshTokenBytes);
        String refreshToken = new String(Hex.encode(refreshTokenBytes));

        return AuthResponseDTO.builder()
                .email(user.getEmail())
                .role(user.getRole())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtUtil.getExpirationMs())
                .build();
    }
}

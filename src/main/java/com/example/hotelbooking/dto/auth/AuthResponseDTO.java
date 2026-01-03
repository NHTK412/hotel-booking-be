package com.example.hotelbooking.dto.auth;

import com.example.hotelbooking.enums.UserRoleEnum;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AuthResponseDTO {
    private String email;
    private UserRoleEnum role;
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
}

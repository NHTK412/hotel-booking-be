package com.example.hotelbooking.dto.user;

import java.time.LocalDateTime;

import com.example.hotelbooking.enums.GenderEnum;
import com.example.hotelbooking.enums.UserRoleEnum;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserResponseDTO {

    private Long id;

    private String name;

    private String email;

    private String phone;

    private LocalDateTime birthday;

    private String gender;

    private String address;

    private String avatarUrl;

    private UserRoleEnum role;

    // private UserRoleEnum role;

    // private Boolean isActive;
}

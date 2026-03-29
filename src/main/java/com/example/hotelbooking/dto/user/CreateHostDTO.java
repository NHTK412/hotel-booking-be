package com.example.hotelbooking.dto.user;

import java.time.LocalDateTime;

import com.example.hotelbooking.enums.AccommodationStaffRoleEnum;
import com.example.hotelbooking.enums.GenderEnum;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CreateHostDTO {
    private String phone;
    private String email;
    private String name;
    private LocalDateTime birthday;
    private GenderEnum gender;
    private String address;
    private String avatarUrl;

    private Long accommodationId;
    private AccommodationStaffRoleEnum hostRole;
}

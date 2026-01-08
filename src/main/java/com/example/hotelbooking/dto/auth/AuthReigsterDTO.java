package com.example.hotelbooking.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AuthReigsterDTO {

    private String name; 

    private String email;

    private String phone;

    private String password;

    
}

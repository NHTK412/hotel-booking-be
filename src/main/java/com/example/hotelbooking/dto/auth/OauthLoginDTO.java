package com.example.hotelbooking.dto.auth;

import com.example.hotelbooking.enums.AuthProviderTypeEnum;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class OauthLoginDTO {

    private AuthProviderTypeEnum provider;
    private String accessToken;
    private String idToken;
    private String name;
}

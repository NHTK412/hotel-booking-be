package com.example.hotelbooking.model;

import com.example.hotelbooking.enums.AuthProviderTypeEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "UserAuthProvider")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UserAuthProvider extends Base {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private AuthProviderTypeEnum type;

    @Column(name = "providerUserId", nullable = false)
    private String providerUserId;

    @Column(name = "password")
    private String password;

    @OneToOne
    @JoinColumn(name = "userId")
    private Users user;

    // @Column(name = "accessToken")
    // private String accessToken;

}

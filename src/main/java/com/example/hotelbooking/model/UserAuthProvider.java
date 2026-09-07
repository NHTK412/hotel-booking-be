package com.example.hotelbooking.model;

import com.example.hotelbooking.enums.AuthProviderTypeEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "UserAuthProvider", indexes = {
    @Index(name = "idx_uap_provider_user_id", columnList = "providerUserId"),
    @Index(name = "idx_uap_type_provider_user", columnList = "type, providerUserId"),
    @Index(name = "idx_uap_user_id", columnList = "userId")
})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UserAuthProvider extends BaseEntity {

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private User user;
}

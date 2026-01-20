package com.example.hotelbooking.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.hotelbooking.enums.AuthProviderTypeEnum;
import com.example.hotelbooking.model.UserAuthProvider;

@Repository
public interface UserAuthProviderRepository extends JpaRepository<UserAuthProvider, Long> {

    Optional<UserAuthProvider> findByTypeAndProviderUserId(AuthProviderTypeEnum type, String providerUserId);

    Optional<UserAuthProvider> findByProviderUserId(String providerUserId);

    

}

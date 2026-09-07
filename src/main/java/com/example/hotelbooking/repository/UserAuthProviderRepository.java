package com.example.hotelbooking.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.hotelbooking.enums.AuthProviderTypeEnum;
import com.example.hotelbooking.model.UserAuthProvider;

@Repository
public interface UserAuthProviderRepository extends JpaRepository<UserAuthProvider, Long> {

    @EntityGraph(attributePaths = {"user"})
    Optional<UserAuthProvider> findByTypeAndProviderUserId(AuthProviderTypeEnum type, String providerUserId);

    @EntityGraph(attributePaths = {"user"})
    Optional<UserAuthProvider> findByProviderUserId(String providerUserId);

    @EntityGraph(attributePaths = {"user"})
    List<UserAuthProvider> findByUser_Id(Long userId);
}

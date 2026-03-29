package com.example.hotelbooking.init;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.hotelbooking.enums.AuthProviderTypeEnum;
import com.example.hotelbooking.model.UserAuthProvider;
import com.example.hotelbooking.model.Users;
import com.example.hotelbooking.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.security.AuthProvider;

import org.springframework.beans.factory.annotation.Value;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    @Value("${account.admin.username}")
    private String adminUsername;

    @Value("${account.admin.password}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) throws Exception {

        if (!userRepository.existsByEmail(adminUsername)) {

            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            String encodedPassword = passwordEncoder.encode(adminPassword);

            Users adminUser = new Users();

            adminUser.setEmail(adminUsername);
            // adminUser.set(encodedPassword);

            UserAuthProvider authProvider = new UserAuthProvider();
            authProvider.setType(AuthProviderTypeEnum.LOCAL);
            authProvider.setProviderUserId(adminUsername);
            authProvider.setPassword(encodedPassword);

            authProvider.setUser(adminUser);

            adminUser.setUserAuthProvider(authProvider);

            adminUser.setName("Admin");
            adminUser.setRole(com.example.hotelbooking.enums.UserRoleEnum.ROLE_ADMIN);
            adminUser.setIsActive(true);

            userRepository.save(adminUser);
        }

        // throw new UnsupportedOperationException("Unimplemented method 'run'");
    }

}

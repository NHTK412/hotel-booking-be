package com.example.hotelbooking.init;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.hotelbooking.enums.AuthProviderTypeEnum;
import com.example.hotelbooking.enums.UserRoleEnum;
import com.example.hotelbooking.model.User;
import com.example.hotelbooking.model.UserAuthProvider;
import com.example.hotelbooking.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

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

            User adminUser = new User();
            adminUser.setEmail(adminUsername);
            adminUser.setName("Admin");
            adminUser.setRole(UserRoleEnum.ROLE_ADMIN);
            adminUser.setIsActive(true);

            UserAuthProvider authProvider = new UserAuthProvider();
            authProvider.setType(AuthProviderTypeEnum.LOCAL);
            authProvider.setProviderUserId(adminUsername);
            authProvider.setPassword(encodedPassword);
            authProvider.setUser(adminUser);

            adminUser.getAuthProviders().add(authProvider);

            userRepository.save(adminUser);
        }
    }
}

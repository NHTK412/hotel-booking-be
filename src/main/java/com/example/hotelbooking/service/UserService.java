package com.example.hotelbooking.service;

import org.springframework.stereotype.Service;

import com.example.hotelbooking.dto.user.UserResponseDTO;
import com.example.hotelbooking.model.Users;
import com.example.hotelbooking.repository.UserRepository;

@Service
public class UserService {
    final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponseDTO getUserById(Long userId) {

        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return UserResponseDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .birthday(user.getBirthday())
                .gender(user.getGender().getDisplayName())
                .address(user.getAddress())
                .avatarUrl(user.getAvatarUrl())
                .build();

    }
}

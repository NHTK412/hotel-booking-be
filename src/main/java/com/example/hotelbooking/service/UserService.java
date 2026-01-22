package com.example.hotelbooking.service;

import org.springframework.stereotype.Service;

import com.example.hotelbooking.dto.user.UserResponseDTO;
import com.example.hotelbooking.exception.customer.NotFoundException;
import com.example.hotelbooking.model.UserAuthProvider;
import com.example.hotelbooking.model.Users;
import com.example.hotelbooking.repository.UserAuthProviderRepository;
import com.example.hotelbooking.repository.UserRepository;

@Service
public class UserService {
        final UserRepository userRepository;

        final UserAuthProviderRepository userAuthProviderRepository;;

        public UserService(UserRepository userRepository, UserAuthProviderRepository userAuthProviderRepository) {
                this.userRepository = userRepository;
                this.userAuthProviderRepository = userAuthProviderRepository;
        }

        public UserResponseDTO getUserById(Long userId) {

                Users user = userRepository.findById(userId)
                                .orElseThrow(() -> new NotFoundException("User not found"));

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

        public UserResponseDTO getUserByProviderId(String providerId) {

                UserAuthProvider userAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("User not found"));

                Users user = userAuthProvider.getUser();

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

        public UserResponseDTO updateUserByProviderId(String providerId,
                        com.example.hotelbooking.dto.user.UserReqquestDTO userReqquestDTO) {

                UserAuthProvider userAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("User not found"));

                Users user = userAuthProvider.getUser();

                user.setName(userReqquestDTO.getName());
                user.setEmail(userReqquestDTO.getEmail());
                user.setPhone(userReqquestDTO.getPhone());
                user.setBirthday(userReqquestDTO.getBirthday());
                user.setGender(userReqquestDTO.getGender());
                user.setAddress(userReqquestDTO.getAddress());
                user.setAvatarUrl(userReqquestDTO.getAvatarUrl());

                Users updatedUser = userRepository.save(user);

                return UserResponseDTO.builder()
                                .id(updatedUser.getId())
                                .name(updatedUser.getName())
                                .email(updatedUser.getEmail())
                                .phone(updatedUser.getPhone())
                                .birthday(updatedUser.getBirthday())
                                .gender(updatedUser.getGender().getDisplayName())
                                .address(updatedUser.getAddress())
                                .avatarUrl(updatedUser.getAvatarUrl())

                                .build();
        }
}

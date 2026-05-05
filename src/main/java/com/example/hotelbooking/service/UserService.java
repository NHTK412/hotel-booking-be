package com.example.hotelbooking.service;

import java.io.File;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.hotelbooking.dto.user.UserResponseDTO;
import com.example.hotelbooking.enums.UserRoleEnum;
import com.example.hotelbooking.exception.customer.NotFoundException;
import com.example.hotelbooking.model.AccommodationStaff;
import com.example.hotelbooking.model.Accommodations;
import com.example.hotelbooking.model.UserAuthProvider;
import com.example.hotelbooking.model.Users;
import com.example.hotelbooking.repository.AccommodationRepository;
import com.example.hotelbooking.repository.UserAuthProviderRepository;
import com.example.hotelbooking.repository.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class UserService {

        @Value("${account.default.password}")
        private String defaultPassword;

        private final UserRepository userRepository;

        private final UserAuthProviderRepository userAuthProviderRepository;

        private final AccommodationRepository accommodationRepository;

        private final FileUploadService fileUploadService;

        public UserService(UserRepository userRepository, UserAuthProviderRepository userAuthProviderRepository,
                        AccommodationRepository accommodationRepository, FileUploadService fileUploadService) {
                this.userRepository = userRepository;
                this.userAuthProviderRepository = userAuthProviderRepository;
                this.accommodationRepository = accommodationRepository;
                this.fileUploadService = fileUploadService;
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
                                .role(user.getRole())
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
                if (user.getAvatarUrl() != null) {
                        fileUploadService.deleteFileByPublicId(user.getAvatarUrl());
                }

                if (userReqquestDTO.getAvatarUrl() != null) {
                        user.setAvatarUrl(userReqquestDTO.getAvatarUrl());
                        fileUploadService.deleteFile(user.getAvatarUrl());
                }

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
                                .role(updatedUser.getRole())
                                .build();
        }

        @Transactional
        public UserResponseDTO registerHost(com.example.hotelbooking.dto.user.CreateHostDTO createHostDTO) {
                Users user = new Users();

                user.setName(createHostDTO.getName());
                user.setEmail(createHostDTO.getEmail());
                user.setPhone(createHostDTO.getPhone());
                user.setBirthday(createHostDTO.getBirthday());
                user.setGender(createHostDTO.getGender());
                user.setAddress(createHostDTO.getAddress());

                if (createHostDTO.getAvatarUrl() != null) {
                        user.setAvatarUrl(createHostDTO.getAvatarUrl());
                        fileUploadService.deleteFile(user.getAvatarUrl());
                }
                user.setIsActive(true);
                user.setRole(UserRoleEnum.ROLE_HOST);

                UserAuthProvider authProvider = new UserAuthProvider();
                authProvider.setType(com.example.hotelbooking.enums.AuthProviderTypeEnum.LOCAL);
                authProvider.setProviderUserId(createHostDTO.getEmail());

                BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
                String encodedPassword = passwordEncoder.encode(defaultPassword);

                authProvider.setPassword(encodedPassword);

                authProvider.setUser(user);

                Accommodations accommodation = accommodationRepository.findById(createHostDTO.getAccommodationId())
                                .orElseThrow(() -> new NotFoundException("Accommodation not found"));

                AccommodationStaff accommodationStaff = new AccommodationStaff();
                accommodationStaff.setUser(user);
                accommodationStaff.setAccommodation(accommodation);
                accommodationStaff.setRole(createHostDTO.getHostRole());

                user.getAccommodationStaffs().add(accommodationStaff);

                Users savedUser = userRepository.save(user);

                return UserResponseDTO.builder()
                                .id(savedUser.getId())
                                .name(savedUser.getName())
                                .email(savedUser.getEmail())
                                .phone(savedUser.getPhone())
                                .birthday(savedUser.getBirthday())
                                .gender(savedUser.getGender().getDisplayName())
                                .address(savedUser.getAddress())
                                .avatarUrl(savedUser.getAvatarUrl())
                                .build();

        }
}

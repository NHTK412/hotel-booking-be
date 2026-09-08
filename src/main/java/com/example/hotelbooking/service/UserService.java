package com.example.hotelbooking.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.hotelbooking.dto.user.CreateHostDTO;
import com.example.hotelbooking.dto.user.UserRequestDTO;
import com.example.hotelbooking.dto.user.UserResponseDTO;
import com.example.hotelbooking.enums.AccommodationStaffRoleEnum;
import com.example.hotelbooking.enums.AuthProviderTypeEnum;
import com.example.hotelbooking.enums.UserRoleEnum;
import com.example.hotelbooking.exception.AccessDeniedException;
import com.example.hotelbooking.exception.ConflictException;
import com.example.hotelbooking.exception.NotFoundException;
import com.example.hotelbooking.model.Accommodation;
import com.example.hotelbooking.model.AccommodationStaff;
import com.example.hotelbooking.model.User;
import com.example.hotelbooking.model.UserAuthProvider;
import com.example.hotelbooking.repository.AccommodationRepository;
import com.example.hotelbooking.repository.UserAuthProviderRepository;
import com.example.hotelbooking.repository.UserRepository;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class UserService {

        @Value("${account.default.password}")
        private String defaultPassword;

        private final UserRepository userRepository;
        private final UserAuthProviderRepository userAuthProviderRepository;
        private final AccommodationRepository accommodationRepository;
        private final FileUploadService fileUploadService;
        private final PasswordEncoder passwordEncoder;

        public UserService(UserRepository userRepository, UserAuthProviderRepository userAuthProviderRepository,
                        AccommodationRepository accommodationRepository, FileUploadService fileUploadService,
                        PasswordEncoder passwordEncoder) {
                this.userRepository = userRepository;
                this.userAuthProviderRepository = userAuthProviderRepository;
                this.accommodationRepository = accommodationRepository;
                this.fileUploadService = fileUploadService;
                this.passwordEncoder = passwordEncoder;
        }

        public UserResponseDTO getUserById(Long userId) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new NotFoundException("User not found"));

                return UserResponseDTO.builder()
                                .id(user.getId())
                                .name(user.getName())
                                .email(user.getEmail())
                                .phone(user.getPhone())
                                .birthday(user.getBirthday())
                                .gender(user.getGender() != null ? user.getGender().getDisplayName() : null)
                                .address(user.getAddress())
                                .avatarUrl(user.getAvatarUrl())
                                .build();
        }

        public UserResponseDTO getUserByProviderId(String providerId) {
                UserAuthProvider userAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("User not found"));

                User user = userAuthProvider.getUser();

                return UserResponseDTO.builder()
                                .id(user.getId())
                                .name(user.getName())
                                .email(user.getEmail())
                                .phone(user.getPhone())
                                .birthday(user.getBirthday())
                                .gender(user.getGender() != null ? user.getGender().getDisplayName() : null)
                                .address(user.getAddress())
                                .avatarUrl(user.getAvatarUrl())
                                .build();
        }

        public UserResponseDTO updateUserByProviderId(String providerId, UserRequestDTO userRequestDTO) {
                UserAuthProvider userAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("User not found"));

                User user = userAuthProvider.getUser();

                user.setName(userRequestDTO.getName());
                user.setEmail(userRequestDTO.getEmail());
                user.setPhone(userRequestDTO.getPhone());
                user.setBirthday(userRequestDTO.getBirthday());
                user.setGender(userRequestDTO.getGender());
                user.setAddress(userRequestDTO.getAddress());

                if (user.getAvatarUrl() != null) {
                        fileUploadService.deleteFileByPublicId(user.getAvatarUrl());
                }

                if (userRequestDTO.getAvatarUrl() != null) {
                        user.setAvatarUrl(userRequestDTO.getAvatarUrl());
                        fileUploadService.deleteFile(user.getAvatarUrl());
                }

                User updatedUser = userRepository.save(user);

                return UserResponseDTO.builder()
                                .id(updatedUser.getId())
                                .name(updatedUser.getName())
                                .email(updatedUser.getEmail())
                                .phone(updatedUser.getPhone())
                                .birthday(updatedUser.getBirthday())
                                .gender(updatedUser.getGender() != null ? updatedUser.getGender().getDisplayName() : null)
                                .address(updatedUser.getAddress())
                                .avatarUrl(updatedUser.getAvatarUrl())
                                .build();
        }

        @Transactional
        public UserResponseDTO registerHost(String currentProviderId, CreateHostDTO createHostDTO) {
                UserAuthProvider currentAuthProvider = userAuthProviderRepository.findByProviderUserId(currentProviderId)
                                .orElseThrow(() -> new NotFoundException("User not found"));
                User currentUser = currentAuthProvider.getUser();

                if (currentUser.getRole() == UserRoleEnum.ROLE_HOST) {
                        if (createHostDTO.getHostRole() != AccommodationStaffRoleEnum.ROLE_RECEPTIONIST) {
                                throw new AccessDeniedException("Chủ khách sạn chỉ có quyền cấp tài khoản Lễ tân (ROLE_RECEPTIONIST)");
                        }
                        boolean isManager = currentUser.getAccommodationStaffs() != null && currentUser.getAccommodationStaffs().stream()
                                        .anyMatch(staff -> staff.getAccommodation() != null
                                                        && staff.getAccommodation().getAccommodationId().equals(createHostDTO.getAccommodationId())
                                                        && staff.getRole() == AccommodationStaffRoleEnum.ROLE_MANAGER);
                        if (!isManager) {
                                throw new AccessDeniedException("Bạn không có quyền quản lý khách sạn này");
                        }
                } else if (currentUser.getRole() != UserRoleEnum.ROLE_ADMIN) {
                        throw new AccessDeniedException("Bạn không có quyền thực hiện thao tác này");
                }

                if (userRepository.existsByEmail(createHostDTO.getEmail())
                                || userAuthProviderRepository.findByProviderUserId(createHostDTO.getEmail()).isPresent()) {
                        throw new ConflictException("Email đã được sử dụng trong hệ thống.");
                }

                User user = new User();
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
                authProvider.setType(AuthProviderTypeEnum.LOCAL);
                authProvider.setProviderUserId(createHostDTO.getEmail());

                String encodedPassword = passwordEncoder.encode(defaultPassword);

                authProvider.setPassword(encodedPassword);
                authProvider.setUser(user);

                Accommodation accommodation = accommodationRepository.findById(createHostDTO.getAccommodationId())
                                .orElseThrow(() -> new NotFoundException("Accommodation not found"));

                AccommodationStaff accommodationStaff = new AccommodationStaff();
                accommodationStaff.setUser(user);
                accommodationStaff.setAccommodation(accommodation);
                accommodationStaff.setRole(createHostDTO.getHostRole());

                user.getAccommodationStaffs().add(accommodationStaff);
                user.getAuthProviders().add(authProvider);

                User savedUser = userRepository.save(user);

                return UserResponseDTO.builder()
                                .id(savedUser.getId())
                                .name(savedUser.getName())
                                .email(savedUser.getEmail())
                                .phone(savedUser.getPhone())
                                .birthday(savedUser.getBirthday())
                                .gender(savedUser.getGender() != null ? savedUser.getGender().getDisplayName() : null)
                                .address(savedUser.getAddress())
                                .avatarUrl(savedUser.getAvatarUrl())
                                .build();
        }
}

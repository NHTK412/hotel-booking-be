package com.example.hotelbooking.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.hotelbooking.dto.user.CreateHostDTO;
import com.example.hotelbooking.dto.user.StaffResponseDTO;
import com.example.hotelbooking.dto.user.UserRequestDTO;
import com.example.hotelbooking.dto.user.UserResponseDTO;
import com.example.hotelbooking.enums.AccommodationStaffRoleEnum;
import com.example.hotelbooking.enums.AuthProviderTypeEnum;
import com.example.hotelbooking.enums.StatusEnum;
import com.example.hotelbooking.enums.UserRoleEnum;
import com.example.hotelbooking.exception.AccessDeniedException;
import com.example.hotelbooking.exception.BadRequestException;
import com.example.hotelbooking.exception.ConflictException;
import com.example.hotelbooking.exception.NotFoundException;
import com.example.hotelbooking.model.Accommodation;
import com.example.hotelbooking.model.AccommodationStaff;
import com.example.hotelbooking.model.User;
import com.example.hotelbooking.model.UserAuthProvider;
import com.example.hotelbooking.repository.AccommodationRepository;
import com.example.hotelbooking.repository.AccommodationStaffRepository;
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
        private final AccommodationStaffRepository accommodationStaffRepository;
        private final FileUploadService fileUploadService;
        private final PasswordEncoder passwordEncoder;

        public UserService(UserRepository userRepository, UserAuthProviderRepository userAuthProviderRepository,
                        AccommodationRepository accommodationRepository,
                        AccommodationStaffRepository accommodationStaffRepository,
                        FileUploadService fileUploadService,
                        PasswordEncoder passwordEncoder) {
                this.userRepository = userRepository;
                this.userAuthProviderRepository = userAuthProviderRepository;
                this.accommodationRepository = accommodationRepository;
                this.accommodationStaffRepository = accommodationStaffRepository;
                this.fileUploadService = fileUploadService;
                this.passwordEncoder = passwordEncoder;
        }

        private UserResponseDTO convertToUserResponseDTO(User user) {
                return UserResponseDTO.builder()
                                .id(user.getId())
                                .name(user.getName())
                                .email(user.getEmail())
                                .phone(user.getPhone())
                                .birthday(user.getBirthday())
                                .gender(user.getGender() != null ? user.getGender().getDisplayName() : null)
                                .address(user.getAddress())
                                .avatarUrl(user.getAvatarUrl())
                                .role(user.getRole())
                                .status(user.getStatus())
                                .isActive(user.getIsActive())
                                .isDeleted(user.getIsDeleted())
                                .build();
        }

        public UserResponseDTO getUserById(Long userId) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new NotFoundException("User not found"));

                return convertToUserResponseDTO(user);
        }

        public UserResponseDTO getUserByProviderId(String providerId) {
                UserAuthProvider userAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("User not found"));

                User user = userAuthProvider.getUser();

                return convertToUserResponseDTO(user);
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

                return convertToUserResponseDTO(updatedUser);
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
                                        .anyMatch(staff -> !Boolean.TRUE.equals(staff.getIsDeleted())
                                                        && staff.getAccommodation() != null
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
                user.setStatus(StatusEnum.ACTIVE);
                user.setIsDeleted(false);
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
                accommodationStaff.setIsDeleted(false);

                user.getAccommodationStaffs().add(accommodationStaff);
                user.getAuthProviders().add(authProvider);

                User savedUser = userRepository.save(user);

                return convertToUserResponseDTO(savedUser);
        }

        public List<StaffResponseDTO> getStaffByAccommodation(String providerId, Long accommodationId) {
                return getStaffByAccommodation(providerId, accommodationId, false);
        }

        public List<StaffResponseDTO> getStaffByAccommodation(String providerId, Long accommodationId, Boolean isDeleted) {
                UserAuthProvider currentAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("User not found"));
                User currentUser = currentAuthProvider.getUser();

                if (currentUser.getRole() == UserRoleEnum.ROLE_HOST) {
                        boolean isManager = currentUser.getAccommodationStaffs() != null && currentUser.getAccommodationStaffs().stream()
                                        .anyMatch(staff -> !Boolean.TRUE.equals(staff.getIsDeleted())
                                                        && staff.getAccommodation() != null
                                                        && staff.getAccommodation().getAccommodationId().equals(accommodationId)
                                                        && staff.getRole() == AccommodationStaffRoleEnum.ROLE_MANAGER);
                        if (!isManager) {
                                throw new AccessDeniedException("Bạn không có quyền xem nhân sự của khách sạn này");
                        }
                } else if (currentUser.getRole() != UserRoleEnum.ROLE_ADMIN) {
                        throw new AccessDeniedException("Bạn không có quyền thực hiện thao tác này");
                }

                boolean filterDeleted = Boolean.TRUE.equals(isDeleted);
                List<AccommodationStaff> staffList = accommodationStaffRepository
                                .findByAccommodation_AccommodationIdAndIsDeleted(accommodationId, filterDeleted);
                return staffList.stream()
                                .map(this::convertToStaffResponseDTO)
                                .toList();
        }

        public List<StaffResponseDTO> getAllStaff(String providerId, Long accommodationId, AccommodationStaffRoleEnum role, String keyword) {
                return getAllStaff(providerId, accommodationId, role, keyword, false);
        }

        public List<StaffResponseDTO> getAllStaff(String providerId, Long accommodationId, AccommodationStaffRoleEnum role, String keyword, Boolean isDeleted) {
                UserAuthProvider currentAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("User not found"));
                User currentUser = currentAuthProvider.getUser();

                String cleanKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
                boolean filterDeleted = Boolean.TRUE.equals(isDeleted);

                if (currentUser.getRole() == UserRoleEnum.ROLE_ADMIN) {
                        List<AccommodationStaff> staffList = accommodationStaffRepository.searchStaff(accommodationId, role, cleanKeyword, filterDeleted);
                        return staffList.stream()
                                        .map(this::convertToStaffResponseDTO)
                                        .toList();
                } else if (currentUser.getRole() == UserRoleEnum.ROLE_HOST) {
                        List<Long> managedAccommodationIds = currentUser.getAccommodationStaffs() != null
                                        ? currentUser.getAccommodationStaffs().stream()
                                                        .filter(s -> !Boolean.TRUE.equals(s.getIsDeleted())
                                                                        && s.getAccommodation() != null
                                                                        && s.getRole() == AccommodationStaffRoleEnum.ROLE_MANAGER)
                                                        .map(s -> s.getAccommodation().getAccommodationId())
                                                        .toList()
                                        : List.of();

                        if (managedAccommodationIds.isEmpty()) {
                                return List.of();
                        }

                        if (accommodationId != null && !managedAccommodationIds.contains(accommodationId)) {
                                throw new AccessDeniedException("Bạn không có quyền xem nhân sự của khách sạn này");
                        }

                        List<AccommodationStaff> staffList = accommodationStaffRepository.searchStaffForAccommodations(
                                        managedAccommodationIds, accommodationId, role, cleanKeyword, filterDeleted);
                        return staffList.stream()
                                        .map(this::convertToStaffResponseDTO)
                                        .toList();
                } else {
                        throw new AccessDeniedException("Bạn không có quyền thực hiện thao tác này");
                }
        }

        @Transactional
        public UserResponseDTO updateUserStatus(String providerId, Long userId, StatusEnum status) {
                UserAuthProvider currentAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("User not found"));
                User currentUser = currentAuthProvider.getUser();

                if (currentUser.getRole() != UserRoleEnum.ROLE_ADMIN) {
                        throw new AccessDeniedException("Chỉ Admin mới có quyền khóa/mở khóa tài khoản người dùng");
                }

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng với ID: " + userId));

                user.setStatus(status);
                user.setIsActive(status == StatusEnum.ACTIVE);
                User savedUser = userRepository.save(user);

                return convertToUserResponseDTO(savedUser);
        }

        @Transactional
        public void deleteStaff(String providerId, Long accommodationStaffId) {
                UserAuthProvider currentAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("User not found"));
                User currentUser = currentAuthProvider.getUser();

                AccommodationStaff staff = accommodationStaffRepository.findById(accommodationStaffId)
                                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin nhân sự với ID: " + accommodationStaffId));

                if (currentUser.getRole() == UserRoleEnum.ROLE_HOST) {
                        Long accId = staff.getAccommodation() != null ? staff.getAccommodation().getAccommodationId() : null;
                        boolean isManager = currentUser.getAccommodationStaffs() != null && currentUser.getAccommodationStaffs().stream()
                                        .anyMatch(s -> !Boolean.TRUE.equals(s.getIsDeleted())
                                                        && s.getAccommodation() != null
                                                        && s.getAccommodation().getAccommodationId().equals(accId)
                                                        && s.getRole() == AccommodationStaffRoleEnum.ROLE_MANAGER);
                        if (!isManager) {
                                throw new AccessDeniedException("Bạn không có quyền quản lý nhân sự tại cơ sở lưu trú này");
                        }
                } else if (currentUser.getRole() != UserRoleEnum.ROLE_ADMIN) {
                        throw new AccessDeniedException("Bạn không có quyền thực hiện thao tác này");
                }

                if (Boolean.TRUE.equals(staff.getIsDeleted())) {
                        throw new BadRequestException("Nhân viên này đã được đánh dấu nghỉ làm trước đó.");
                }

                staff.setIsDeleted(true);
                accommodationStaffRepository.save(staff);
        }

        @Transactional
        public void restoreStaff(String providerId, Long accommodationStaffId) {
                UserAuthProvider currentAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("User not found"));
                User currentUser = currentAuthProvider.getUser();

                AccommodationStaff staff = accommodationStaffRepository.findById(accommodationStaffId)
                                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin nhân sự với ID: " + accommodationStaffId));

                if (currentUser.getRole() == UserRoleEnum.ROLE_HOST) {
                        Long accId = staff.getAccommodation() != null ? staff.getAccommodation().getAccommodationId() : null;
                        boolean isManager = currentUser.getAccommodationStaffs() != null && currentUser.getAccommodationStaffs().stream()
                                        .anyMatch(s -> !Boolean.TRUE.equals(s.getIsDeleted())
                                                        && s.getAccommodation() != null
                                                        && s.getAccommodation().getAccommodationId().equals(accId)
                                                        && s.getRole() == AccommodationStaffRoleEnum.ROLE_MANAGER);
                        if (!isManager) {
                                throw new AccessDeniedException("Bạn không có quyền quản lý nhân sự tại cơ sở lưu trú này");
                        }
                } else if (currentUser.getRole() != UserRoleEnum.ROLE_ADMIN) {
                        throw new AccessDeniedException("Bạn không có quyền thực hiện thao tác này");
                }

                if (!Boolean.TRUE.equals(staff.getIsDeleted())) {
                        throw new BadRequestException("Nhân viên này hiện đang hoạt động bình thường, không thể khôi phục.");
                }

                staff.setIsDeleted(false);
                accommodationStaffRepository.save(staff);
        }

        @Transactional
        public void deleteUser(String providerId, Long userId) {
                UserAuthProvider currentAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("User not found"));
                User currentUser = currentAuthProvider.getUser();

                if (currentUser.getRole() != UserRoleEnum.ROLE_ADMIN) {
                        throw new AccessDeniedException("Chỉ Admin mới có quyền xóa người dùng");
                }

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng với ID: " + userId));

                if (Boolean.TRUE.equals(user.getIsDeleted())) {
                        throw new BadRequestException("Tài khoản người dùng này đã bị xóa trước đó.");
                }

                user.setIsDeleted(true);
                userRepository.save(user);
        }

        @Transactional
        public void restoreUser(String providerId, Long userId) {
                UserAuthProvider currentAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("User not found"));
                User currentUser = currentAuthProvider.getUser();

                if (currentUser.getRole() != UserRoleEnum.ROLE_ADMIN) {
                        throw new AccessDeniedException("Chỉ Admin mới có quyền khôi phục người dùng");
                }

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng với ID: " + userId));

                if (!Boolean.TRUE.equals(user.getIsDeleted())) {
                        throw new BadRequestException("Tài khoản người dùng này chưa bị xóa, không thể khôi phục.");
                }

                user.setIsDeleted(false);
                userRepository.save(user);
        }

        private StaffResponseDTO convertToStaffResponseDTO(AccommodationStaff staff) {
                User u = staff.getUser();
                Accommodation acc = staff.getAccommodation();
                return StaffResponseDTO.builder()
                                .id(u.getId())
                                .userId(u.getId())
                                .accommodationStaffId(staff.getAccommodationStaffId())
                                .name(u.getName())
                                .email(u.getEmail())
                                .phone(u.getPhone())
                                .birthday(u.getBirthday())
                                .gender(u.getGender() != null ? u.getGender().getDisplayName() : null)
                                .address(u.getAddress())
                                .avatarUrl(u.getAvatarUrl())
                                .systemRole(u.getRole())
                                .role(staff.getRole())
                                .staffRole(staff.getRole())
                                .status(u.getStatus())
                                .isActive(u.getIsActive() != null ? u.getIsActive() : true)
                                .isDeleted(staff.getIsDeleted())
                                .accommodationId(acc != null ? acc.getAccommodationId() : null)
                                .accommodationName(acc != null ? acc.getAccommodationName() : null)
                                .hotelType(acc != null ? acc.getType() : null)
                                .createdAt(u.getCreateAt())
                                .build();
        }
}

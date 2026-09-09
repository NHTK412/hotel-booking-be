package com.example.hotelbooking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.hotelbooking.dto.user.CreateHostDTO;
import com.example.hotelbooking.dto.user.StaffResponseDTO;
import com.example.hotelbooking.dto.user.UserResponseDTO;
import com.example.hotelbooking.enums.AccommodationStaffRoleEnum;
import com.example.hotelbooking.enums.GenderEnum;
import com.example.hotelbooking.enums.StatusEnum;
import com.example.hotelbooking.enums.UserRoleEnum;
import com.example.hotelbooking.exception.AccessDeniedException;
import com.example.hotelbooking.exception.BadRequestException;
import com.example.hotelbooking.exception.ConflictException;
import com.example.hotelbooking.model.Accommodation;
import com.example.hotelbooking.model.AccommodationStaff;
import com.example.hotelbooking.model.User;
import com.example.hotelbooking.model.UserAuthProvider;
import com.example.hotelbooking.repository.AccommodationRepository;
import com.example.hotelbooking.repository.AccommodationStaffRepository;
import com.example.hotelbooking.repository.UserAuthProviderRepository;
import com.example.hotelbooking.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests for Staff & Host Registration RBAC (UserService)")
class UserServiceStaffRegistrationTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserAuthProviderRepository userAuthProviderRepository;

    @Mock
    private AccommodationRepository accommodationRepository;

    @Mock
    private AccommodationStaffRepository accommodationStaffRepository;

    @Mock
    private FileUploadService fileUploadService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User adminUser;
    private UserAuthProvider adminAuthProvider;

    private User hostManagerUser;
    private UserAuthProvider hostManagerAuthProvider;

    private Accommodation accommodation1;
    private Accommodation accommodation2;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "defaultPassword", "DefaultPassword123!");

        // 1. Admin
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setName("System Admin");
        adminUser.setRole(UserRoleEnum.ROLE_ADMIN);

        adminAuthProvider = new UserAuthProvider();
        adminAuthProvider.setProviderUserId("admin@hotel.com");
        adminAuthProvider.setUser(adminUser);

        // 2. Accommodations
        accommodation1 = new Accommodation();
        accommodation1.setAccommodationId(10L);
        accommodation1.setAccommodationName("Grand Hotel");

        accommodation2 = new Accommodation();
        accommodation2.setAccommodationId(20L);
        accommodation2.setAccommodationName("Seaside Resort");

        // 3. Host Manager of Accommodation 1
        hostManagerUser = new User();
        hostManagerUser.setId(2L);
        hostManagerUser.setName("Host Manager");
        hostManagerUser.setRole(UserRoleEnum.ROLE_HOST);

        AccommodationStaff staffManager = new AccommodationStaff();
        staffManager.setUser(hostManagerUser);
        staffManager.setAccommodation(accommodation1);
        staffManager.setRole(AccommodationStaffRoleEnum.ROLE_MANAGER);

        List<AccommodationStaff> staffList = new ArrayList<>();
        staffList.add(staffManager);
        hostManagerUser.setAccommodationStaffs(staffList);

        hostManagerAuthProvider = new UserAuthProvider();
        hostManagerAuthProvider.setProviderUserId("manager@hotel.com");
        hostManagerAuthProvider.setUser(hostManagerUser);
    }

    private CreateHostDTO createDTO(Long accId, AccommodationStaffRoleEnum role, String email) {
        return CreateHostDTO.builder()
                .name("New Staff")
                .email(email)
                .phone("0912345678")
                .birthday(LocalDateTime.now().minusYears(25))
                .gender(GenderEnum.MALE)
                .address("123 Street")
                .accommodationId(accId)
                .hostRole(role)
                .build();
    }

    @Nested
    @DisplayName("Admin Registration Permissions")
    class AdminRegistrationTests {

        @Test
        @DisplayName("Admin can create ROLE_MANAGER account successfully")
        void admin_createsManager_success() {
            CreateHostDTO dto = createDTO(10L, AccommodationStaffRoleEnum.ROLE_MANAGER, "newmanager@hotel.com");

            when(userAuthProviderRepository.findByProviderUserId("admin@hotel.com"))
                    .thenReturn(Optional.of(adminAuthProvider));
            when(userRepository.existsByEmail("newmanager@hotel.com")).thenReturn(false);
            when(userAuthProviderRepository.findByProviderUserId("newmanager@hotel.com")).thenReturn(Optional.empty());
            when(accommodationRepository.findById(10L)).thenReturn(Optional.of(accommodation1));
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User saved = invocation.getArgument(0);
                saved.setId(100L);
                return saved;
            });

            UserResponseDTO result = userService.registerHost("admin@hotel.com", dto);

            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo("newmanager@hotel.com");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Admin can create ROLE_RECEPTIONIST account successfully")
        void admin_createsReceptionist_success() {
            CreateHostDTO dto = createDTO(10L, AccommodationStaffRoleEnum.ROLE_RECEPTIONIST, "receptionist@hotel.com");

            when(userAuthProviderRepository.findByProviderUserId("admin@hotel.com"))
                    .thenReturn(Optional.of(adminAuthProvider));
            when(userRepository.existsByEmail("receptionist@hotel.com")).thenReturn(false);
            when(userAuthProviderRepository.findByProviderUserId("receptionist@hotel.com")).thenReturn(Optional.empty());
            when(accommodationRepository.findById(10L)).thenReturn(Optional.of(accommodation1));
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User saved = invocation.getArgument(0);
                saved.setId(101L);
                return saved;
            });

            UserResponseDTO result = userService.registerHost("admin@hotel.com", dto);

            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo("receptionist@hotel.com");
            verify(userRepository).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("Host Manager Registration Permissions")
    class HostManagerRegistrationTests {

        @Test
        @DisplayName("Host Manager can create ROLE_RECEPTIONIST for their own accommodation")
        void hostManager_createsReceptionistForOwnAccommodation_success() {
            CreateHostDTO dto = createDTO(10L, AccommodationStaffRoleEnum.ROLE_RECEPTIONIST, "rec1@hotel.com");

            when(userAuthProviderRepository.findByProviderUserId("manager@hotel.com"))
                    .thenReturn(Optional.of(hostManagerAuthProvider));
            when(userRepository.existsByEmail("rec1@hotel.com")).thenReturn(false);
            when(userAuthProviderRepository.findByProviderUserId("rec1@hotel.com")).thenReturn(Optional.empty());
            when(accommodationRepository.findById(10L)).thenReturn(Optional.of(accommodation1));
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User saved = invocation.getArgument(0);
                saved.setId(102L);
                return saved;
            });

            UserResponseDTO result = userService.registerHost("manager@hotel.com", dto);

            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo("rec1@hotel.com");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Host Manager CANNOT create ROLE_MANAGER account (throws AccessDeniedException)")
        void hostManager_createsManager_throwsAccessDenied() {
            CreateHostDTO dto = createDTO(10L, AccommodationStaffRoleEnum.ROLE_MANAGER, "anothermanager@hotel.com");

            when(userAuthProviderRepository.findByProviderUserId("manager@hotel.com"))
                    .thenReturn(Optional.of(hostManagerAuthProvider));

            assertThatThrownBy(() -> userService.registerHost("manager@hotel.com", dto))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Chủ khách sạn chỉ có quyền cấp tài khoản Lễ tân");
        }

        @Test
        @DisplayName("Host Manager CANNOT create receptionist for another accommodation (throws AccessDeniedException)")
        void hostManager_createsReceptionistForOtherAccommodation_throwsAccessDenied() {
            CreateHostDTO dto = createDTO(20L, AccommodationStaffRoleEnum.ROLE_RECEPTIONIST, "rec2@hotel.com");

            when(userAuthProviderRepository.findByProviderUserId("manager@hotel.com"))
                    .thenReturn(Optional.of(hostManagerAuthProvider));

            assertThatThrownBy(() -> userService.registerHost("manager@hotel.com", dto))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Bạn không có quyền quản lý khách sạn này");
        }
    }

    @Nested
    @DisplayName("Validation & Conflict Tests")
    class ValidationTests {

        @Test
        @DisplayName("Creating staff with duplicate email in userRepository throws ConflictException")
        void duplicateEmailInUserRepository_throwsConflictException() {
            CreateHostDTO dto = createDTO(10L, AccommodationStaffRoleEnum.ROLE_RECEPTIONIST, "existing@hotel.com");

            when(userAuthProviderRepository.findByProviderUserId("admin@hotel.com"))
                    .thenReturn(Optional.of(adminAuthProvider));
            when(userRepository.existsByEmail("existing@hotel.com")).thenReturn(true);

            assertThatThrownBy(() -> userService.registerHost("admin@hotel.com", dto))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Email đã được sử dụng");
        }

        @Test
        @DisplayName("Unauthorized role (e.g. ROLE_CUSTOMER) throws AccessDeniedException")
        void customerRole_throwsAccessDenied() {
            User customer = new User();
            customer.setId(3L);
            customer.setRole(UserRoleEnum.ROLE_CUSTOMER);

            UserAuthProvider customerAuthProvider = new UserAuthProvider();
            customerAuthProvider.setProviderUserId("customer@user.com");
            customerAuthProvider.setUser(customer);

            CreateHostDTO dto = createDTO(10L, AccommodationStaffRoleEnum.ROLE_RECEPTIONIST, "rec@hotel.com");

            when(userAuthProviderRepository.findByProviderUserId("customer@user.com"))
                    .thenReturn(Optional.of(customerAuthProvider));

            assertThatThrownBy(() -> userService.registerHost("customer@user.com", dto))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Bạn không có quyền");
        }
    }

    @Nested
    @DisplayName("Get Staff By Accommodation Tests")
    class GetStaffByAccommodationTests {

        @Test
        @DisplayName("Admin can get staff list of any accommodation")
        void admin_getStaffByAccommodation_success() {
            when(userAuthProviderRepository.findByProviderUserId("admin@hotel.com"))
                    .thenReturn(Optional.of(adminAuthProvider));

            AccommodationStaff staff = new AccommodationStaff();
            staff.setUser(hostManagerUser);
            staff.setAccommodation(accommodation1);
            staff.setRole(AccommodationStaffRoleEnum.ROLE_MANAGER);
            staff.setIsDeleted(false);

            when(accommodationStaffRepository.findByAccommodation_AccommodationIdAndIsDeleted(10L, false))
                    .thenReturn(List.of(staff));

            List<StaffResponseDTO> result = userService.getStaffByAccommodation("admin@hotel.com", 10L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Host Manager");
            assertThat(result.get(0).getAccommodationName()).isEqualTo("Grand Hotel");
        }

        @Test
        @DisplayName("Host Manager can get staff list of own accommodation")
        void hostManager_getStaffByAccommodation_success() {
            when(userAuthProviderRepository.findByProviderUserId("manager@hotel.com"))
                    .thenReturn(Optional.of(hostManagerAuthProvider));

            AccommodationStaff staff = new AccommodationStaff();
            staff.setUser(hostManagerUser);
            staff.setAccommodation(accommodation1);
            staff.setRole(AccommodationStaffRoleEnum.ROLE_MANAGER);
            staff.setIsDeleted(false);

            when(accommodationStaffRepository.findByAccommodation_AccommodationIdAndIsDeleted(10L, false))
                    .thenReturn(List.of(staff));

            List<StaffResponseDTO> result = userService.getStaffByAccommodation("manager@hotel.com", 10L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Host Manager");
        }

        @Test
        @DisplayName("Host Manager cannot get staff of accommodation they do not manage")
        void hostManager_getStaffOfOtherAccommodation_throwsAccessDenied() {
            when(userAuthProviderRepository.findByProviderUserId("manager@hotel.com"))
                    .thenReturn(Optional.of(hostManagerAuthProvider));

            assertThatThrownBy(() -> userService.getStaffByAccommodation("manager@hotel.com", 20L))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Bạn không có quyền xem nhân sự");
        }
    }

    @Nested
    @DisplayName("Get All Staff Tests")
    class GetAllStaffTests {

        @Test
        @DisplayName("Admin searches all staff successfully")
        void admin_getAllStaff_success() {
            when(userAuthProviderRepository.findByProviderUserId("admin@hotel.com"))
                    .thenReturn(Optional.of(adminAuthProvider));

            AccommodationStaff staff = new AccommodationStaff();
            staff.setUser(hostManagerUser);
            staff.setAccommodation(accommodation1);
            staff.setRole(AccommodationStaffRoleEnum.ROLE_MANAGER);
            staff.setIsDeleted(false);

            when(accommodationStaffRepository.searchStaff(null, null, "Manager", false))
                    .thenReturn(List.of(staff));

            List<StaffResponseDTO> result = userService.getAllStaff("admin@hotel.com", null, null, "Manager");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getEmail()).isEqualTo(hostManagerUser.getEmail());
        }

        @Test
        @DisplayName("Host Manager gets staff within their managed accommodations")
        void hostManager_getAllStaff_success() {
            when(userAuthProviderRepository.findByProviderUserId("manager@hotel.com"))
                    .thenReturn(Optional.of(hostManagerAuthProvider));

            AccommodationStaff staff = new AccommodationStaff();
            staff.setUser(hostManagerUser);
            staff.setAccommodation(accommodation1);
            staff.setRole(AccommodationStaffRoleEnum.ROLE_MANAGER);
            staff.setIsDeleted(false);

            when(accommodationStaffRepository.searchStaffForAccommodations(List.of(10L), null, null, null, false))
                    .thenReturn(List.of(staff));

            List<StaffResponseDTO> result = userService.getAllStaff("manager@hotel.com", null, null, null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Host Manager");
        }
    }

    @Nested
    @DisplayName("User Status & Staff Soft Delete Tests")
    class UserStatusAndStaffSoftDeleteTests {

        @Test
        @DisplayName("Admin locks user account by setting status to INACTIVE")
        void admin_lockUserAccount_success() {
            when(userAuthProviderRepository.findByProviderUserId("admin@hotel.com"))
                    .thenReturn(Optional.of(adminAuthProvider));
            when(userRepository.findById(2L)).thenReturn(Optional.of(hostManagerUser));
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            UserResponseDTO response = userService.updateUserStatus("admin@hotel.com", 2L, StatusEnum.INACTIVE);

            assertThat(response.getStatus()).isEqualTo(StatusEnum.INACTIVE);
            assertThat(response.getIsActive()).isFalse();
        }

        @Test
        @DisplayName("Host cannot update user status, throws AccessDeniedException")
        void host_updateUserStatus_throwsAccessDenied() {
            when(userAuthProviderRepository.findByProviderUserId("manager@hotel.com"))
                    .thenReturn(Optional.of(hostManagerAuthProvider));

            assertThatThrownBy(() -> userService.updateUserStatus("manager@hotel.com", 2L, StatusEnum.INACTIVE))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Chỉ Admin");
        }

        @Test
        @DisplayName("Host Manager soft deletes (terminates) staff from accommodation")
        void hostManager_deleteStaff_success() {
            when(userAuthProviderRepository.findByProviderUserId("manager@hotel.com"))
                    .thenReturn(Optional.of(hostManagerAuthProvider));

            User receptionistUser = new User();
            receptionistUser.setId(3L);
            receptionistUser.setName("Receptionist");

            AccommodationStaff staff = new AccommodationStaff();
            staff.setAccommodationStaffId(100L);
            staff.setUser(receptionistUser);
            staff.setAccommodation(accommodation1);
            staff.setRole(AccommodationStaffRoleEnum.ROLE_RECEPTIONIST);
            staff.setIsDeleted(false);

            when(accommodationStaffRepository.findById(100L)).thenReturn(Optional.of(staff));

            userService.deleteStaff("manager@hotel.com", 100L);

            assertThat(staff.getIsDeleted()).isTrue();
            verify(accommodationStaffRepository).save(staff);
        }

        @Test
        @DisplayName("Host Manager deleting already resigned staff throws BadRequestException")
        void hostManager_deleteStaff_alreadyResigned_throwsBadRequest() {
            when(userAuthProviderRepository.findByProviderUserId("manager@hotel.com"))
                    .thenReturn(Optional.of(hostManagerAuthProvider));

            User receptionistUser = new User();
            receptionistUser.setId(3L);
            receptionistUser.setName("Receptionist");

            AccommodationStaff staff = new AccommodationStaff();
            staff.setAccommodationStaffId(100L);
            staff.setUser(receptionistUser);
            staff.setAccommodation(accommodation1);
            staff.setIsDeleted(true);

            when(accommodationStaffRepository.findById(100L)).thenReturn(Optional.of(staff));

            assertThatThrownBy(() -> userService.deleteStaff("manager@hotel.com", 100L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("đã được đánh dấu nghỉ làm trước đó");
        }

        @Test
        @DisplayName("Host Manager restores resigned staff to active duty")
        void hostManager_restoreStaff_success() {
            when(userAuthProviderRepository.findByProviderUserId("manager@hotel.com"))
                    .thenReturn(Optional.of(hostManagerAuthProvider));

            User receptionistUser = new User();
            receptionistUser.setId(3L);
            receptionistUser.setName("Receptionist");

            AccommodationStaff staff = new AccommodationStaff();
            staff.setAccommodationStaffId(100L);
            staff.setUser(receptionistUser);
            staff.setAccommodation(accommodation1);
            staff.setIsDeleted(true);

            when(accommodationStaffRepository.findById(100L)).thenReturn(Optional.of(staff));

            userService.restoreStaff("manager@hotel.com", 100L);

            assertThat(staff.getIsDeleted()).isFalse();
            verify(accommodationStaffRepository).save(staff);
        }

        @Test
        @DisplayName("Resigned Host Manager cannot register new staff, throws AccessDeniedException")
        void resignedManager_cannotRegisterStaff_throwsAccessDenied() {
            when(userAuthProviderRepository.findByProviderUserId("manager@hotel.com"))
                    .thenReturn(Optional.of(hostManagerAuthProvider));

            // Mark manager as resigned (isDeleted = true)
            hostManagerUser.getAccommodationStaffs().get(0).setIsDeleted(true);

            CreateHostDTO dto = new CreateHostDTO();
            dto.setAccommodationId(10L);
            dto.setHostRole(AccommodationStaffRoleEnum.ROLE_RECEPTIONIST);
            dto.setEmail("newstaff@hotel.com");

            assertThatThrownBy(() -> userService.registerHost("manager@hotel.com", dto))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Bạn không có quyền quản lý khách sạn này");
        }

        @Test
        @DisplayName("Admin soft deletes user successfully")
        void admin_deleteUser_success() {
            when(userAuthProviderRepository.findByProviderUserId("admin@hotel.com"))
                    .thenReturn(Optional.of(adminAuthProvider));
            User target = new User();
            target.setId(5L);
            target.setIsDeleted(false);
            when(userRepository.findById(5L)).thenReturn(Optional.of(target));

            userService.deleteUser("admin@hotel.com", 5L);

            assertThat(target.getIsDeleted()).isTrue();
            verify(userRepository).save(target);
        }

        @Test
        @DisplayName("Admin restores soft deleted user successfully")
        void admin_restoreUser_success() {
            when(userAuthProviderRepository.findByProviderUserId("admin@hotel.com"))
                    .thenReturn(Optional.of(adminAuthProvider));
            User target = new User();
            target.setId(5L);
            target.setIsDeleted(true);
            when(userRepository.findById(5L)).thenReturn(Optional.of(target));

            userService.restoreUser("admin@hotel.com", 5L);

            assertThat(target.getIsDeleted()).isFalse();
            verify(userRepository).save(target);
        }
    }
}

package com.example.hotelbooking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.hotelbooking.dto.auth.AuthLoginDTO;
import com.example.hotelbooking.dto.auth.AuthRegisterDTO;
import com.example.hotelbooking.dto.auth.AuthResponseDTO;
import com.example.hotelbooking.dto.auth.OauthLoginDTO;
import com.example.hotelbooking.dto.auth.RefreshTokenRequestDTO;
import com.example.hotelbooking.enums.AuthProviderTypeEnum;
import com.example.hotelbooking.enums.UserRoleEnum;
import com.example.hotelbooking.exception.ConflictException;
import com.example.hotelbooking.exception.InvalidCredentialsException;
import com.example.hotelbooking.model.User;
import com.example.hotelbooking.model.UserAuthProvider;
import com.example.hotelbooking.repository.UserAuthProviderRepository;
import com.example.hotelbooking.repository.UserRepository;
import com.example.hotelbooking.security.jwt.JwtUtil;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests for AuthService")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserAuthProviderRepository userAuthProviderRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Spy
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private MailService mailService;

    @InjectMocks
    private AuthService authService;

    private User mockUser;
    private UserAuthProvider mockAuthProvider;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setName("Nguyen Van A");
        mockUser.setEmail("customer@gmail.com");
        mockUser.setPhone("0912345678");
        mockUser.setRole(UserRoleEnum.ROLE_CUSTOMER);
        mockUser.setIsActive(true);

        mockAuthProvider = new UserAuthProvider();
        mockAuthProvider.setId(10L);
        mockAuthProvider.setType(AuthProviderTypeEnum.LOCAL);
        mockAuthProvider.setProviderUserId("customer@gmail.com");
        mockAuthProvider.setPassword(passwordEncoder.encode("password123"));
        mockAuthProvider.setUser(mockUser);
    }

    @Nested
    @DisplayName("User Registration Tests")
    class RegisterTests {

        @Test
        @DisplayName("Đăng ký thành công: Tạo User, mã hóa mật khẩu, cấp phát tokens")
        void testRegister_Success() {
            AuthRegisterDTO registerDTO = new AuthRegisterDTO();
            registerDTO.setName("Nguyen Van B");
            registerDTO.setEmail("newuser@gmail.com");
            registerDTO.setPassword("Secret123!");
            registerDTO.setPhone("0987654321");

            when(userRepository.findByEmail("newuser@gmail.com")).thenReturn(Optional.empty());
            when(jwtUtil.generateToken(eq("newuser@gmail.com"), eq(UserRoleEnum.ROLE_CUSTOMER)))
                    .thenReturn("mock-access-token");
            when(jwtUtil.getExpirationMs()).thenReturn(900000L);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            AuthResponseDTO response = authService.register(registerDTO);

            assertNotNull(response);
            assertEquals("mock-access-token", response.getAccessToken());
            assertEquals("newuser@gmail.com", response.getEmail());
            verify(userRepository).save(any(User.class));
            verify(userAuthProviderRepository).save(any(UserAuthProvider.class));
        }

        @Test
        @DisplayName("Đăng ký thất bại khi email đã tồn tại trong hệ thống (ConflictException)")
        void testRegister_DuplicateEmail_ThrowsConflictException() {
            AuthRegisterDTO registerDTO = new AuthRegisterDTO();
            registerDTO.setEmail("customer@gmail.com");

            when(userRepository.findByEmail("customer@gmail.com")).thenReturn(Optional.of(mockUser));

            ConflictException ex = assertThrows(ConflictException.class, () -> {
                authService.register(registerDTO);
            });

            assertEquals("Email is already in use", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("User Login Tests")
    class LoginTests {

        @Test
        @DisplayName("Đăng nhập thành công: Cấp phát Access Token và lưu Refresh Token vào Redis với TTL 7 ngày")
        void testLogin_Success_StoresRefreshTokenInRedis() {
            AuthLoginDTO loginDTO = new AuthLoginDTO("customer@gmail.com", "password123");

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(userAuthProviderRepository.findByTypeAndProviderUserId(AuthProviderTypeEnum.LOCAL, "customer@gmail.com"))
                    .thenReturn(Optional.of(mockAuthProvider));
            when(jwtUtil.generateToken("customer@gmail.com", UserRoleEnum.ROLE_CUSTOMER))
                    .thenReturn("mocked-access-token");
            when(jwtUtil.getExpirationMs()).thenReturn(900000L);

            AuthResponseDTO response = authService.login(loginDTO);

            assertNotNull(response);
            assertEquals("mocked-access-token", response.getAccessToken());
            assertNotNull(response.getRefreshToken());
            assertEquals("customer@gmail.com", response.getEmail());
            assertEquals(1L, response.getUserId());

            verify(valueOperations).set(eq("RT::" + response.getRefreshToken()), eq("1"), eq(7L), eq(TimeUnit.DAYS));
            verify(valueOperations).set(eq("RT_USER::1"), eq(response.getRefreshToken()), eq(7L), eq(TimeUnit.DAYS));
        }

        @Test
        @DisplayName("Đăng nhập thất bại khi sai mật khẩu")
        void testLogin_WrongPassword_ThrowsException() {
            AuthLoginDTO loginDTO = new AuthLoginDTO("customer@gmail.com", "wrong-password");

            when(userAuthProviderRepository.findByTypeAndProviderUserId(AuthProviderTypeEnum.LOCAL, "customer@gmail.com"))
                    .thenReturn(Optional.of(mockAuthProvider));

            assertThrows(InvalidCredentialsException.class, () -> {
                authService.login(loginDTO);
            });
        }

        @Test
        @DisplayName("Đăng nhập thất bại khi email không tồn tại trong hệ thống")
        void testLogin_UserNotFound_ThrowsException() {
            AuthLoginDTO loginDTO = new AuthLoginDTO("notfound@gmail.com", "password123");

            when(userAuthProviderRepository.findByTypeAndProviderUserId(AuthProviderTypeEnum.LOCAL, "notfound@gmail.com"))
                    .thenReturn(Optional.empty());

            assertThrows(InvalidCredentialsException.class, () -> {
                authService.login(loginDTO);
            });
        }

        @Test
        @DisplayName("Đăng nhập thất bại khi tài khoản đã bị vô hiệu hóa (isActive = false)")
        void testLogin_InactiveAccount_ThrowsException() {
            AuthLoginDTO loginDTO = new AuthLoginDTO("customer@gmail.com", "password123");
            mockUser.setIsActive(false);

            when(userAuthProviderRepository.findByTypeAndProviderUserId(AuthProviderTypeEnum.LOCAL, "customer@gmail.com"))
                    .thenReturn(Optional.of(mockAuthProvider));

            InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class, () -> {
                authService.login(loginDTO);
            });
            assertTrue(ex.getMessage().contains("inactive"));
        }
    }

    @Nested
    @DisplayName("OAuth Login Tests")
    class OAuthTests {

        @Test
        @DisplayName("OAuth Login thành công cho khách hàng mới: Tự động đăng ký và đăng nhập")
        void testOauthLogin_NewUser_Success() {
            OauthLoginDTO dto = OauthLoginDTO.builder()
                    .provider(AuthProviderTypeEnum.GOOGLE)
                    .sub("google-uid-123456")
                    .email("googleuser@gmail.com")
                    .name("Google User")
                    .avatarUrl("https://photo.com/avt.jpg")
                    .build();

            when(userAuthProviderRepository.findByTypeAndProviderUserId(AuthProviderTypeEnum.GOOGLE, "google-uid-123456"))
                    .thenReturn(Optional.empty());
            when(userRepository.findByEmail("googleuser@gmail.com")).thenReturn(Optional.empty());
            when(jwtUtil.generateToken("google-uid-123456", UserRoleEnum.ROLE_CUSTOMER))
                    .thenReturn("oauth-access-token");
            when(jwtUtil.getExpirationMs()).thenReturn(900000L);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            AuthResponseDTO response = authService.oauthLogin(dto);

            assertNotNull(response);
            assertEquals("oauth-access-token", response.getAccessToken());
            verify(userRepository).save(any(User.class));
            verify(userAuthProviderRepository).save(any(UserAuthProvider.class));
        }
    }

    @Nested
    @DisplayName("Refresh Token & Logout Tests")
    class TokenRotationTests {

        @Test
        @DisplayName("Làm mới Token (Token Rotation) thành công: Thu hồi Refresh Token cũ và cấp phát cặp Token mới")
        void testRefreshToken_Success_RotatesTokens() {
            String oldRefreshToken = "old-refresh-token-12345";
            RefreshTokenRequestDTO requestDTO = new RefreshTokenRequestDTO(oldRefreshToken);

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("RT::" + oldRefreshToken)).thenReturn("1");
            when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
            when(userAuthProviderRepository.findByUser_Id(1L)).thenReturn(List.of(mockAuthProvider));
            when(jwtUtil.generateToken("customer@gmail.com", UserRoleEnum.ROLE_CUSTOMER))
                    .thenReturn("new-access-token");
            when(jwtUtil.getExpirationMs()).thenReturn(900000L);

            AuthResponseDTO response = authService.refreshToken(requestDTO);

            assertNotNull(response);
            assertEquals("new-access-token", response.getAccessToken());
            assertNotNull(response.getRefreshToken());

            verify(redisTemplate).delete("RT::" + oldRefreshToken);
            verify(valueOperations).set(eq("RT::" + response.getRefreshToken()), eq("1"), eq(7L), eq(TimeUnit.DAYS));
            verify(valueOperations).set(eq("RT_USER::1"), eq(response.getRefreshToken()), eq(7L), eq(TimeUnit.DAYS));
        }

        @Test
        @DisplayName("Làm mới Token thất bại khi gửi Refresh Token không tồn tại hoặc đã hết hạn trong Redis")
        void testRefreshToken_InvalidToken_ThrowsException() {
            String invalidToken = "invalid-or-expired-token";
            RefreshTokenRequestDTO requestDTO = new RefreshTokenRequestDTO(invalidToken);

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("RT::" + invalidToken)).thenReturn(null);

            InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class, () -> {
                authService.refreshToken(requestDTO);
            });

            assertTrue(ex.getMessage().contains("Invalid or expired refresh token"));
        }

        @Test
        @DisplayName("Làm mới Token thất bại khi tài khoản người dùng đã bị vô hiệu hóa (isActive = false)")
        void testRefreshToken_InactiveUser_ThrowsExceptionAndCleansUp() {
            String token = "active-token-inactive-user";
            RefreshTokenRequestDTO requestDTO = new RefreshTokenRequestDTO(token);

            mockUser.setIsActive(false);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("RT::" + token)).thenReturn("1");
            when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

            InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class, () -> {
                authService.refreshToken(requestDTO);
            });

            assertTrue(ex.getMessage().contains("inactive"));
            verify(redisTemplate).delete("RT::" + token);
            verify(redisTemplate).delete("RT_USER::1");
        }

        @Test
        @DisplayName("Đăng xuất thành công: Xóa Refresh Token và phiên đăng nhập của người dùng khỏi Redis")
        void testLogout_Success_RemovesTokensFromRedis() {
            String refreshToken = "logout-refresh-token";
            RefreshTokenRequestDTO requestDTO = new RefreshTokenRequestDTO(refreshToken);

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("RT::" + refreshToken)).thenReturn("1");

            Boolean result = authService.logout(requestDTO);

            assertTrue(result);
            verify(redisTemplate).delete("RT::" + refreshToken);
            verify(redisTemplate).delete("RT_USER::1");
        }
    }

    @Nested
    @DisplayName("OTP Sending Tests")
    class OtpTests {

        @Test
        @DisplayName("Gửi mã OTP thành công khi email tồn tại")
        void testSendOtp_Success() {
            when(userAuthProviderRepository.findByTypeAndProviderUserId(AuthProviderTypeEnum.LOCAL, "customer@gmail.com"))
                    .thenReturn(Optional.of(mockAuthProvider));
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            Boolean result = authService.sendOtp("customer@gmail.com");

            assertTrue(result);
            verify(mailService).sendEmail(eq("customer@gmail.com"), eq("Your OTP Code"), anyString());
        }

        @Test
        @DisplayName("Gửi mã OTP thất bại khi email chưa đăng ký tài khoản")
        void testSendOtp_EmailNotFound_ThrowsException() {
            when(userAuthProviderRepository.findByTypeAndProviderUserId(AuthProviderTypeEnum.LOCAL, "unknown@gmail.com"))
                    .thenReturn(Optional.empty());

            assertThrows(InvalidCredentialsException.class, () -> {
                authService.sendOtp("unknown@gmail.com");
            });
        }
    }
}

package com.example.hotelbooking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.example.hotelbooking.dto.auth.AuthLoginDTO;
import com.example.hotelbooking.dto.auth.AuthResponseDTO;
import com.example.hotelbooking.dto.auth.RefreshTokenRequestDTO;
import com.example.hotelbooking.enums.AuthProviderTypeEnum;
import com.example.hotelbooking.enums.UserRoleEnum;
import com.example.hotelbooking.exception.InvalidCredentialsException;
import com.example.hotelbooking.model.User;
import com.example.hotelbooking.model.UserAuthProvider;
import com.example.hotelbooking.repository.UserAuthProviderRepository;
import com.example.hotelbooking.repository.UserRepository;
import com.example.hotelbooking.security.jwt.JwtUtil;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserAuthProviderRepository userAuthProviderRepository;

    @Mock
    private JwtUtil jwtUtil;

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
        mockUser.setRole(UserRoleEnum.ROLE_CUSTOMER);
        mockUser.setIsActive(true);

        mockAuthProvider = new UserAuthProvider();
        mockAuthProvider.setId(10L);
        mockAuthProvider.setType(AuthProviderTypeEnum.LOCAL);
        mockAuthProvider.setProviderUserId("customer@gmail.com");
        mockAuthProvider.setPassword(new BCryptPasswordEncoder().encode("password123"));
        mockAuthProvider.setUser(mockUser);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("Đăng nhập thành công: Cấp phát Access Token và lưu Refresh Token vào Redis với TTL 7 ngày")
    void testLogin_Success_StoresRefreshTokenInRedis() {
        AuthLoginDTO loginDTO = new AuthLoginDTO("customer@gmail.com", "password123");

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

        // Kiểm tra Refresh Token được lưu vào Redis với TTL 7 ngày
        verify(valueOperations).set(eq("RT::" + response.getRefreshToken()), eq("1"), eq(7L), eq(TimeUnit.DAYS));
        verify(valueOperations).set(eq("RT_USER::1"), eq(response.getRefreshToken()), eq(7L), eq(TimeUnit.DAYS));
    }

    @Test
    @DisplayName("Làm mới Token (Token Rotation) thành công: Thu hồi Refresh Token cũ và cấp phát cặp Token mới")
    void testRefreshToken_Success_RotatesTokens() {
        String oldRefreshToken = "old-refresh-token-12345";
        RefreshTokenRequestDTO requestDTO = new RefreshTokenRequestDTO(oldRefreshToken);

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

        // Đảm bảo Refresh Token cũ đã bị xóa khỏi Redis (Token Rotation)
        verify(redisTemplate).delete("RT::" + oldRefreshToken);

        // Đảm bảo Refresh Token mới được lưu vào Redis
        verify(valueOperations).set(eq("RT::" + response.getRefreshToken()), eq("1"), eq(7L), eq(TimeUnit.DAYS));
        verify(valueOperations).set(eq("RT_USER::1"), eq(response.getRefreshToken()), eq(7L), eq(TimeUnit.DAYS));
    }

    @Test
    @DisplayName("Làm mới Token thất bại khi gửi Refresh Token không tồn tại hoặc đã hết hạn trong Redis")
    void testRefreshToken_InvalidToken_ThrowsException() {
        String invalidToken = "invalid-or-expired-token";
        RefreshTokenRequestDTO requestDTO = new RefreshTokenRequestDTO(invalidToken);

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

        when(valueOperations.get("RT::" + refreshToken)).thenReturn("1");

        Boolean result = authService.logout(requestDTO);

        assertTrue(result);
        verify(redisTemplate).delete("RT::" + refreshToken);
        verify(redisTemplate).delete("RT_USER::1");
    }
}

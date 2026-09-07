package com.example.hotelbooking.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.example.hotelbooking.dto.auth.AuthLoginDTO;
import com.example.hotelbooking.dto.auth.AuthRegisterDTO;
import com.example.hotelbooking.dto.auth.AuthResponseDTO;
import com.example.hotelbooking.dto.auth.RefreshTokenRequestDTO;
import com.example.hotelbooking.enums.UserRoleEnum;
import com.example.hotelbooking.exception.GlobalExceptionHandler;
import com.example.hotelbooking.exception.InvalidCredentialsException;
import com.example.hotelbooking.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("Integration Tests for AuthController (HTTP -> Controller -> ExceptionHandler)")
class AuthControllerIntegrationTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setValidator(validator)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("POST /auth/login Integration Tests")
    class LoginIntegrationTests {

        @Test
        @DisplayName("200 OK: Đăng nhập thành công khi thông tin hợp lệ")
        void testLogin_Success_Returns200() throws Exception {
            AuthLoginDTO loginDTO = new AuthLoginDTO("user@example.com", "password123");
            AuthResponseDTO authResponse = AuthResponseDTO.builder()
                    .userId(1L)
                    .email("user@example.com")
                    .role(UserRoleEnum.ROLE_CUSTOMER)
                    .accessToken("mock-access-token")
                    .refreshToken("mock-refresh-token")
                    .build();

            when(authService.login(any(AuthLoginDTO.class))).thenReturn(authResponse);

            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Đăng nhập thành công"))
                    .andExpect(jsonPath("$.data.accessToken").value("mock-access-token"))
                    .andExpect(jsonPath("$.data.refreshToken").value("mock-refresh-token"))
                    .andExpect(jsonPath("$.data.email").value("user@example.com"));
        }

        @Test
        @DisplayName("400 Bad Request: Validate thất bại khi email không đúng định dạng hoặc để trống")
        void testLogin_ValidationFailure_Returns400() throws Exception {
            AuthLoginDTO invalidDto = new AuthLoginDTO("", "");

            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.data.email").exists())
                    .andExpect(jsonPath("$.data.password").exists());
        }

        @Test
        @DisplayName("401 Unauthorized: Sai thông tin đăng nhập trả về lỗi qua GlobalExceptionHandler")
        void testLogin_InvalidCredentials_Returns401() throws Exception {
            AuthLoginDTO loginDTO = new AuthLoginDTO("user@example.com", "wrongpassword");

            when(authService.login(any(AuthLoginDTO.class)))
                    .thenThrow(new InvalidCredentialsException("Invalid email or password"));

            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginDTO)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Invalid email or password"));
        }
    }

    @Nested
    @DisplayName("POST /auth/register Integration Tests")
    class RegisterIntegrationTests {

        @Test
        @DisplayName("200 OK: Đăng ký tài khoản thành công")
        void testRegister_Success_Returns200() throws Exception {
            AuthRegisterDTO registerDTO = new AuthRegisterDTO();
            registerDTO.setName("Nguyen Van C");
            registerDTO.setEmail("newuser@gmail.com");
            registerDTO.setPassword("Secret123!");
            registerDTO.setPhone("0912345678");

            AuthResponseDTO authResponse = AuthResponseDTO.builder()
                    .userId(2L)
                    .email("newuser@gmail.com")
                    .role(UserRoleEnum.ROLE_CUSTOMER)
                    .accessToken("mock-access-token")
                    .refreshToken("mock-refresh-token")
                    .build();

            when(authService.register(any(AuthRegisterDTO.class))).thenReturn(authResponse);

            mockMvc.perform(post("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.email").value("newuser@gmail.com"));
        }

        @Test
        @DisplayName("400 Bad Request: Validate thất bại khi password ngắn hơn 6 ký tự hoặc sđt sai định dạng")
        void testRegister_ValidationFailure_Returns400() throws Exception {
            AuthRegisterDTO invalidDto = new AuthRegisterDTO();
            invalidDto.setName("Test");
            invalidDto.setEmail("test@gmail.com");
            invalidDto.setPassword("123");
            invalidDto.setPhone("123456");

            mockMvc.perform(post("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.data.password").exists())
                    .andExpect(jsonPath("$.data.phone").exists());
        }
    }

    @Nested
    @DisplayName("POST /auth/refresh-token Integration Tests")
    class RefreshTokenIntegrationTests {

        @Test
        @DisplayName("200 OK: Làm mới token thành công")
        void testRefreshToken_Success_Returns200() throws Exception {
            RefreshTokenRequestDTO requestDTO = new RefreshTokenRequestDTO("valid-refresh-token");
            AuthResponseDTO authResponse = AuthResponseDTO.builder()
                    .userId(1L)
                    .accessToken("new-access-token")
                    .refreshToken("new-refresh-token")
                    .build();

            when(authService.refreshToken(any(RefreshTokenRequestDTO.class))).thenReturn(authResponse);

            mockMvc.perform(post("/auth/refresh-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
                    .andExpect(jsonPath("$.data.refreshToken").value("new-refresh-token"));
        }

        @Test
        @DisplayName("400 Bad Request: RefreshToken bị trống trả về mã 400")
        void testRefreshToken_BlankToken_Returns400() throws Exception {
            RefreshTokenRequestDTO requestDTO = new RefreshTokenRequestDTO("");

            mockMvc.perform(post("/auth/refresh-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.data.refreshToken").exists());
        }
    }

    @Nested
    @DisplayName("POST /auth/logout Integration Tests")
    class LogoutIntegrationTests {

        @Test
        @DisplayName("200 OK: Đăng xuất thành công")
        void testLogout_Success_Returns200() throws Exception {
            RefreshTokenRequestDTO requestDTO = new RefreshTokenRequestDTO("logout-token");

            when(authService.logout(any())).thenReturn(true);

            mockMvc.perform(post("/auth/logout")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value(true));
        }
    }
}

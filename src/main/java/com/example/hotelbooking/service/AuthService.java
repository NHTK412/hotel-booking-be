package com.example.hotelbooking.service;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.security.autoconfigure.SecurityProperties.User;
import org.springframework.context.support.BeanDefinitionDsl.Role;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.stereotype.Service;

import com.example.hotelbooking.dto.auth.AuthLoginDTO;
import com.example.hotelbooking.dto.auth.AuthReigsterDTO;
import com.example.hotelbooking.dto.auth.AuthResponseDTO;
import com.example.hotelbooking.dto.auth.OauthLoginDTO;
import com.example.hotelbooking.enums.AuthProviderTypeEnum;
import com.example.hotelbooking.enums.GenderEnum;
import com.example.hotelbooking.enums.UserRoleEnum;
import com.example.hotelbooking.exception.customer.AccessDeniedHandlerException;
import com.example.hotelbooking.exception.customer.ConflictException;
import com.example.hotelbooking.exception.customer.InvalidCredentialsException;
import com.example.hotelbooking.exception.customer.InvalidRefreshTokenException;
import com.example.hotelbooking.model.UserAuthProvider;
import com.example.hotelbooking.model.Users;
import com.example.hotelbooking.repository.UserAuthProviderRepository;
import com.example.hotelbooking.repository.UserRepository;
import com.example.hotelbooking.util.JwtUtil;

import jakarta.transaction.Transactional;

@Service
public class AuthService {

    private final AccessDeniedHandlerException accessDeniedHandlerException;

    private final UserRepository userRepository;

    private final UserAuthProviderRepository userAuthProviderRepository;

    private final JwtUtil jwtUtil;

    final private SecureRandom secureRandom = new SecureRandom();

    final private RedisTemplate<String, String> redisTemplate;

    final private MailService mailService;

    // public AuthService(UserRepository userRepository, JwtUtil jwtUtil) {
    // this.userRepository = userRepository;
    public AuthService(
            UserRepository userRepository, UserAuthProviderRepository userAuthProviderRepository, JwtUtil jwtUtil,
            RedisTemplate<String, String> redisTemplate, MailService mailService,
            AccessDeniedHandlerException accessDeniedHandlerException) {

        this.userRepository = userRepository;
        this.userAuthProviderRepository = userAuthProviderRepository;
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
        this.mailService = mailService;
        this.accessDeniedHandlerException = accessDeniedHandlerException;
    }

    @Transactional
    public AuthResponseDTO login(AuthLoginDTO authLoginDTO) {
        // Users user = userRepository.findByEmail(authLoginDTO.getEmail())
        // // .orElseThrow(() -> new RuntimeException("User not found"));
        // .orElseThrow(() -> new InvalidCredentialsException("Invalid email or
        // password"));

        UserAuthProvider userAuthProvider = userAuthProviderRepository
                .findByTypeAndProviderUserId(AuthProviderTypeEnum.LOCAL,
                        authLoginDTO.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        // BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        // if (!passwordEncoder.matches(authLoginDTO.getPassword(), user.getPassword()))
        // {
        // throw new RuntimeException("Invalid credentials");
        // }

        // if (!authLoginDTO.getPassword().equals(user.getPassword())) {

        // if
        // (!authLoginDTO.getPassword().equals(user.getUserAuthProvider().getPassword()))
        // {

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        // String code = encoder.encode(authLoginDTO.getPassword());
        // System.out.println(code);

        if (!encoder.matches(authLoginDTO.getPassword(), userAuthProvider.getPassword())) {

            // throw new RuntimeException("Invalid credentials");
            throw new InvalidCredentialsException("Invalid email or password");
        }

        Users user = userAuthProvider.getUser();

        String accessToken = jwtUtil.generateToken(userAuthProvider.getProviderUserId(), user.getRole());

        byte[] refreshTokenBytes = new byte[50];
        secureRandom.nextBytes(refreshTokenBytes);
        String refreshToken = new String(Hex.encode(refreshTokenBytes));

        return AuthResponseDTO.builder()
                // .email(user.getEmail())
                .email(userAuthProvider.getProviderUserId())
                .role(user.getRole())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtUtil.getExpirationMs())
                .build();
    }

    public AuthResponseDTO oauthLogin(OauthLoginDTO oauthLoginDTO) {

        if (oauthLoginDTO.getProvider() == null || oauthLoginDTO.getProvider() == AuthProviderTypeEnum.LOCAL) {
            throw new InvalidCredentialsException("Invalid OAuth provider");
        }

        Optional<UserAuthProvider> userAuthProviderOptional = userAuthProviderRepository
                .findByTypeAndProviderUserId(
                        oauthLoginDTO.getProvider(), oauthLoginDTO.getIdToken());

        final Users user;
        final UserAuthProvider userAuthProvider;

        if (userAuthProviderOptional.isEmpty()) {

            // Users newUser = new Users();
            // newUser.setName(oauthLoginDTO.getName());
            // newUser.setRole(UserRoleEnum.ROLE_CUSTOMER);

            // userRepository.save(newUser);

            user = new Users();
            user.setName(oauthLoginDTO.getName());
            user.setRole(UserRoleEnum.ROLE_CUSTOMER);
            user.setEmail("");
            user.setPhone("");
            user.setIsActive(true);
            user.setGender(GenderEnum.OTHER);

            userRepository.save(user);

            userAuthProvider = new UserAuthProvider();
            userAuthProvider.setType(oauthLoginDTO.getProvider());
            userAuthProvider.setProviderUserId(oauthLoginDTO.getIdToken());
            userAuthProvider.setUser(user);

            userAuthProviderRepository.save(userAuthProvider);
        } else {

            userAuthProvider = userAuthProviderOptional.get();
            user = userAuthProvider.getUser();
        }

        // Users user = userAuthProvider.getUser();

        // String accessToken = jwtUtil.generateToken(user.getEmail(), user.getRole());
        String accessToken = jwtUtil.generateToken(userAuthProvider.getProviderUserId(), UserRoleEnum.ROLE_CUSTOMER);
        // user.getRole());

        byte[] refreshTokenBytes = new byte[50];
        secureRandom.nextBytes(refreshTokenBytes);
        String refreshToken = new String(Hex.encode(refreshTokenBytes));

        return AuthResponseDTO.builder()
                .email(user.getEmail())
                .role(user.getRole())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtUtil.getExpirationMs())
                .build();
    }

    @Transactional
    public AuthResponseDTO register(AuthReigsterDTO registerDTO) {
        // Check if user with the same email already exists
        if (userRepository.findByEmail(registerDTO.getEmail()).isPresent()) {
            throw new ConflictException("Email is already in use");
        }

        // Create new user
        Users newUser = new Users();
        newUser.setName(registerDTO.getName());
        newUser.setEmail(registerDTO.getEmail());
        newUser.setPhone(registerDTO.getPhone());
        newUser.setRole(UserRoleEnum.ROLE_CUSTOMER);
        newUser.setIsActive(true);
        newUser.setGender(GenderEnum.OTHER);

        userRepository.save(newUser);

        // Create UserAuthProvider for local authentication
        UserAuthProvider userAuthProvider = new UserAuthProvider();
        userAuthProvider.setType(AuthProviderTypeEnum.LOCAL);
        userAuthProvider.setProviderUserId(registerDTO.getEmail());
        // userAuthProvider.setPassword(registerDTO.getPassword());
        userAuthProvider.setUser(newUser);

        /// Hash pass
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // if (!employee.getPassword().equals(password)) {
        if (!encoder.matches(registerDTO.getPassword(), userAuthProvider.getPassword())) {
            throw new InvalidRefreshTokenException("Mật khẩu không hợp lệ");
        }

        userAuthProviderRepository.save(userAuthProvider);

        // Generate tokens
        String accessToken = jwtUtil.generateToken(newUser.getEmail(), newUser.getRole());

        byte[] refreshTokenBytes = new byte[50];
        secureRandom.nextBytes(refreshTokenBytes);
        String refreshToken = new String(Hex.encode(refreshTokenBytes));

        return AuthResponseDTO.builder()
                .email(newUser.getEmail())
                .role(newUser.getRole())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtUtil.getExpirationMs())
                .build();
    }

    @Transactional
    public Boolean sendOtp(String email) {

        UserAuthProvider userAuthProvider = userAuthProviderRepository
                .findByTypeAndProviderUserId(AuthProviderTypeEnum.LOCAL, email)
                .orElseThrow(() -> new InvalidCredentialsException("Email not registered"));
        // Tạo mã OTP gồm 4 chữ số
        String otp = String.format("%04d", secureRandom.nextInt(10000));

        // Lưu vào redis

        // redisTemplate.opsForValue().set("refreshToken::" + refreshToken,
        // employee.getUsername(), 7, TimeUnit.DAYS);

        // redisTemplate.opsForValue().set("OTP_" + email, otp, 5 * 60); // hết hạn sau
        // 5 phút
        redisTemplate.opsForValue().set("otp::" + userAuthProvider.getProviderUserId(), otp, 5, TimeUnit.MINUTES); // hết
                                                                                                                   // hạn
                                                                                                                   // sau
                                                                                                                   // 5
                                                                                                                   // phút

        // Gửi mã OTP đến email người dùng (sử dụng dịch vụ email)
        System.out.println("Sending OTP " + otp + " to email: " + email);

        mailService.sendEmail(email, "Your OTP Code", "Your OTP code is: " + otp);

        // return otp;
        return true;
    }

    // @Transactional
    // public boolean verifyOtp(String email, String otp) {

    // String cachedOtp = redisTemplate.opsForValue().get("otp::" + email);

    // if (cachedOtp != null && cachedOtp.equals(otp)) {
    // // Xóa mã OTP khỏi Redis sau khi xác thực thành công
    // redisTemplate.delete("otp::" + email);
    // return true;
    // }

    // return false;
    // }
    @Transactional
    public Map<String, Object> verifyOtp(String email, String otp) {

        String cachedOtp = redisTemplate.opsForValue().get("otp::" + email);

        if (cachedOtp != null && cachedOtp.equals(otp)) {
            // Xóa mã OTP khỏi Redis sau khi xác thực thành công
            redisTemplate.delete("otp::" + email);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("isValid", true);

            // Users user = userRepository.findByEmail(email)
            // .orElseThrow(() -> new InvalidCredentialsException("Email not registered"));
            // String accessToken = jwtUtil.generateToken(user.getEmail(), user.getRole());

            UserAuthProvider userAuthProvider = userAuthProviderRepository
                    .findByTypeAndProviderUserId(AuthProviderTypeEnum.LOCAL,
                            email)
                    .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

            String accessToken = jwtUtil.generateToken(userAuthProvider.getProviderUserId(),
                    userAuthProvider.getUser().getRole());

            byte[] refreshTokenBytes = new byte[50];
            secureRandom.nextBytes(refreshTokenBytes);
            String refreshToken = new String(Hex.encode(refreshTokenBytes));
            responseData.put("accessToken", accessToken);
            responseData.put("refreshToken", refreshToken);
            responseData.put("expiresIn", jwtUtil.getExpirationMs());
            return responseData;

        }

        return Map.of("isValid", false);
    }

    // resetPassword
    @Transactional
    public Boolean resetPassword(String email, String newPassword) {

        UserAuthProvider userAuthProvider = userAuthProviderRepository
                .findByTypeAndProviderUserId(AuthProviderTypeEnum.LOCAL, email)
                .orElseThrow(() -> new InvalidCredentialsException("Email not registered"));

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String code = encoder.encode(newPassword);

        userAuthProvider.setPassword(code);
        userAuthProviderRepository.save(userAuthProvider);
        return true;
    }

}

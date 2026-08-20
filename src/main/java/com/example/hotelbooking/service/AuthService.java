package com.example.hotelbooking.service;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.stereotype.Service;

import com.example.hotelbooking.dto.auth.AuthLoginDTO;
import com.example.hotelbooking.dto.auth.AuthRegisterDTO;
import com.example.hotelbooking.dto.auth.AuthResponseDTO;
import com.example.hotelbooking.dto.auth.OauthLoginDTO;
import com.example.hotelbooking.enums.AuthProviderTypeEnum;
import com.example.hotelbooking.enums.GenderEnum;
import com.example.hotelbooking.enums.UserRoleEnum;
import com.example.hotelbooking.exception.ConflictException;
import com.example.hotelbooking.exception.InvalidCredentialsException;
import com.example.hotelbooking.model.User;
import com.example.hotelbooking.model.UserAuthProvider;
import com.example.hotelbooking.repository.UserAuthProviderRepository;
import com.example.hotelbooking.repository.UserRepository;
import com.example.hotelbooking.security.jwt.JwtUtil;

import jakarta.transaction.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserAuthProviderRepository userAuthProviderRepository;
    private final JwtUtil jwtUtil;
    private final SecureRandom secureRandom = new SecureRandom();
    private final RedisTemplate<String, String> redisTemplate;
    private final MailService mailService;

    public AuthService(
            UserRepository userRepository,
            UserAuthProviderRepository userAuthProviderRepository,
            JwtUtil jwtUtil,
            RedisTemplate<String, String> redisTemplate,
            MailService mailService) {
        this.userRepository = userRepository;
        this.userAuthProviderRepository = userAuthProviderRepository;
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
        this.mailService = mailService;
    }

    @Transactional
    public AuthResponseDTO login(AuthLoginDTO authLoginDTO) {
        UserAuthProvider userAuthProvider = userAuthProviderRepository
                .findByTypeAndProviderUserId(AuthProviderTypeEnum.LOCAL, authLoginDTO.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        if (!encoder.matches(authLoginDTO.getPassword(), userAuthProvider.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        User user = userAuthProvider.getUser();
        String accessToken = jwtUtil.generateToken(userAuthProvider.getProviderUserId(), user.getRole());

        byte[] refreshTokenBytes = new byte[50];
        secureRandom.nextBytes(refreshTokenBytes);
        String refreshToken = new String(Hex.encode(refreshTokenBytes));

        return AuthResponseDTO.builder()
                .email(userAuthProvider.getProviderUserId())
                .role(user.getRole())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtUtil.getExpirationMs())
                .userId(user.getId())
                .build();
    }

    public AuthResponseDTO oauthLogin(OauthLoginDTO oauthLoginDTO) {
        if (oauthLoginDTO.getProvider() == null || oauthLoginDTO.getProvider() == AuthProviderTypeEnum.LOCAL) {
            throw new InvalidCredentialsException("Invalid OAuth provider");
        }

        Optional<UserAuthProvider> userAuthProviderOptional = userAuthProviderRepository
                .findByTypeAndProviderUserId(oauthLoginDTO.getProvider(), oauthLoginDTO.getIdToken());

        final User user;
        final UserAuthProvider userAuthProvider;

        if (userAuthProviderOptional.isEmpty()) {
            user = new User();
            user.setName(oauthLoginDTO.getName());
            user.setRole(UserRoleEnum.ROLE_CUSTOMER);
            user.setEmail("");
            user.setPhone("");
            user.setIsActive(true);
            user.setGender(GenderEnum.OTHER);
            user.setAvatarUrl("avt.png");

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

        String accessToken = jwtUtil.generateToken(userAuthProvider.getProviderUserId(), UserRoleEnum.ROLE_CUSTOMER);

        byte[] refreshTokenBytes = new byte[50];
        secureRandom.nextBytes(refreshTokenBytes);
        String refreshToken = new String(Hex.encode(refreshTokenBytes));

        return AuthResponseDTO.builder()
                .email(user.getEmail())
                .role(user.getRole())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtUtil.getExpirationMs())
                .userId(user.getId())
                .build();
    }

    @Transactional
    public AuthResponseDTO register(AuthRegisterDTO registerDTO) {
        if (userRepository.findByEmail(registerDTO.getEmail()).isPresent()) {
            throw new ConflictException("Email is already in use");
        }

        User newUser = new User();
        newUser.setName(registerDTO.getName());
        newUser.setEmail(registerDTO.getEmail());
        newUser.setPhone(registerDTO.getPhone());
        newUser.setRole(UserRoleEnum.ROLE_CUSTOMER);
        newUser.setIsActive(true);
        newUser.setGender(GenderEnum.OTHER);
        newUser.setAvatarUrl("avt.png");

        userRepository.save(newUser);

        UserAuthProvider userAuthProvider = new UserAuthProvider();
        userAuthProvider.setType(AuthProviderTypeEnum.LOCAL);
        userAuthProvider.setProviderUserId(registerDTO.getEmail());

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        userAuthProvider.setPassword(encoder.encode(registerDTO.getPassword()));
        userAuthProvider.setUser(newUser);

        userAuthProviderRepository.save(userAuthProvider);

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
                .userId(newUser.getId())
                .build();
    }

    @Transactional
    public Boolean sendOtp(String email) {
        UserAuthProvider userAuthProvider = userAuthProviderRepository
                .findByTypeAndProviderUserId(AuthProviderTypeEnum.LOCAL, email)
                .orElseThrow(() -> new InvalidCredentialsException("Email not registered"));

        String otp = String.format("%04d", secureRandom.nextInt(10000));
        redisTemplate.opsForValue().set("otp::" + userAuthProvider.getProviderUserId(), otp, 5, TimeUnit.MINUTES);

        mailService.sendEmail(email, "Your OTP Code", "Your OTP code is: " + otp);
        return true;
    }

    @Transactional
    public Map<String, Object> verifyOtp(String email, String otp) {
        String cachedOtp = redisTemplate.opsForValue().get("otp::" + email);

        if (cachedOtp != null && cachedOtp.equals(otp)) {
            redisTemplate.delete("otp::" + email);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("isValid", true);

            UserAuthProvider userAuthProvider = userAuthProviderRepository
                    .findByTypeAndProviderUserId(AuthProviderTypeEnum.LOCAL, email)
                    .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

            String accessToken = jwtUtil.generateToken(userAuthProvider.getProviderUserId(),
                    userAuthProvider.getUser().getRole());

            byte[] refreshTokenBytes = new byte[50];
            secureRandom.nextBytes(refreshTokenBytes);
            String refreshToken = new String(Hex.encode(refreshTokenBytes));
            responseData.put("userId", userAuthProvider.getUser().getId());
            responseData.put("accessToken", accessToken);
            responseData.put("refreshToken", refreshToken);
            responseData.put("expiresIn", jwtUtil.getExpirationMs());
            return responseData;
        }

        return Map.of("isValid", false);
    }

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

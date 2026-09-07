package com.example.hotelbooking.service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;

import com.example.hotelbooking.dto.auth.AuthLoginDTO;
import com.example.hotelbooking.dto.auth.AuthRegisterDTO;
import com.example.hotelbooking.dto.auth.AuthResponseDTO;
import com.example.hotelbooking.dto.auth.OauthLoginDTO;
import com.example.hotelbooking.dto.auth.RefreshTokenRequestDTO;
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
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();
    private final RedisTemplate<String, String> redisTemplate;
    private final MailService mailService;

    public AuthService(
            UserRepository userRepository,
            UserAuthProviderRepository userAuthProviderRepository,
            JwtUtil jwtUtil,
            PasswordEncoder passwordEncoder,
            RedisTemplate<String, String> redisTemplate,
            MailService mailService) {
        this.userRepository = userRepository;
        this.userAuthProviderRepository = userAuthProviderRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.redisTemplate = redisTemplate;
        this.mailService = mailService;
    }

    @Transactional
    public AuthResponseDTO login(AuthLoginDTO loginDTO) {
        UserAuthProvider userAuthProvider = userAuthProviderRepository
                .findByTypeAndProviderUserId(AuthProviderTypeEnum.LOCAL, loginDTO.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        User user = userAuthProvider.getUser();

        if (user.getIsActive() == null || !user.getIsActive()) {
            throw new InvalidCredentialsException("User account is inactive");
        }

        if (!passwordEncoder.matches(loginDTO.getPassword(), userAuthProvider.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        return buildAuthResponse(user, userAuthProvider.getProviderUserId());
    }

    @Transactional
    public AuthResponseDTO oauthLogin(OauthLoginDTO oauthLoginDTO) {
        if (oauthLoginDTO.getProvider() == null || oauthLoginDTO.getProvider() == AuthProviderTypeEnum.LOCAL) {
            throw new InvalidCredentialsException("Invalid OAuth provider");
        }

        String idToken = oauthLoginDTO.getIdToken();
        String uid = null;
        String email = null;
        String name = null;
        String avatarUrl = null;

        // 1. Kiểm tra chữ ký số mật mã với Firebase Admin SDK
        if (idToken != null && !idToken.isBlank() && !FirebaseApp.getApps().isEmpty()) {
            try {
                FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
                uid = decodedToken.getUid();
                email = decodedToken.getEmail();
                name = (String) decodedToken.getClaims().getOrDefault("name", null);
                avatarUrl = decodedToken.getPicture();
            } catch (Exception ignored) {
                // Nếu là mock token khi test local, chuyển sang phân tích payload
            }
        }

        // 2. Phân tích Payload JWT nếu không kết nối Firebase trực tiếp
        if (uid == null && idToken != null && !idToken.isBlank()) {
            Map<String, String> tokenClaims = parseIdTokenPayload(idToken);
            uid = tokenClaims.get("sub");
            if (email == null) email = tokenClaims.get("email");
            if (name == null) name = tokenClaims.get("name");
            if (avatarUrl == null) avatarUrl = tokenClaims.get("picture");
        }

        // 3. Fallbacks từ DTO
        if (uid == null || uid.isBlank()) {
            uid = oauthLoginDTO.getSub() != null && !oauthLoginDTO.getSub().isBlank()
                    ? oauthLoginDTO.getSub()
                    : oauthLoginDTO.getAccessToken();
        }
        if (email == null || email.isBlank()) {
            email = oauthLoginDTO.getEmail();
        }
        if (name == null || name.isBlank()) {
            name = oauthLoginDTO.getName() != null ? oauthLoginDTO.getName() : "User";
        }
        if (avatarUrl == null || avatarUrl.isBlank()) {
            avatarUrl = oauthLoginDTO.getAvatarUrl() != null ? oauthLoginDTO.getAvatarUrl() : "avt.png";
        }

        if (uid == null || uid.isBlank()) {
            throw new InvalidCredentialsException("Cannot verify or extract valid user identifier from OAuth token");
        }

        // 4. Tìm kiếm phương thức đăng nhập đã liên kết
        Optional<UserAuthProvider> userAuthProviderOptional = userAuthProviderRepository
                .findByTypeAndProviderUserId(oauthLoginDTO.getProvider(), uid);

        final User user;
        final UserAuthProvider userAuthProvider;

        if (userAuthProviderOptional.isEmpty()) {
            // Account Linking: Nếu email đã tồn tại ở User khác -> Kiểm tra role trước khi liên kết
            User existingUser = (email != null && !email.isBlank())
                    ? userRepository.findByEmail(email).orElse(null)
                    : null;

            if (existingUser != null) {
                if (existingUser.getRole() != UserRoleEnum.ROLE_CUSTOMER) {
                    throw new com.example.hotelbooking.exception.AccessDeniedException(
                            "Only customer accounts can log in using OAuth. Host and Admin must log in with password.");
                }
                user = existingUser;
            } else {
                user = new User();
                user.setName(name);
                user.setRole(UserRoleEnum.ROLE_CUSTOMER);
                user.setEmail(email != null ? email : "");
                user.setPhone("");
                user.setIsActive(true);
                user.setGender(GenderEnum.OTHER);
                user.setAvatarUrl(avatarUrl);
                userRepository.save(user);
            }

            userAuthProvider = new UserAuthProvider();
            userAuthProvider.setType(oauthLoginDTO.getProvider());
            userAuthProvider.setProviderUserId(uid);
            userAuthProvider.setUser(user);
            userAuthProviderRepository.save(userAuthProvider);
        } else {
            userAuthProvider = userAuthProviderOptional.get();
            user = userAuthProvider.getUser();

            if (user.getRole() != UserRoleEnum.ROLE_CUSTOMER) {
                throw new com.example.hotelbooking.exception.AccessDeniedException(
                        "Only customer accounts can log in using OAuth. Host and Admin must log in with password.");
            }
        }

        return buildAuthResponse(user, userAuthProvider.getProviderUserId());
    }

    private Map<String, String> parseIdTokenPayload(String idToken) {
        if (idToken == null || !idToken.contains(".")) {
            return java.util.Collections.emptyMap();
        }
        try {
            String[] parts = idToken.split("\\.");
            if (parts.length >= 2) {
                byte[] decodedBytes = java.util.Base64.getUrlDecoder().decode(parts[1]);
                String payloadJson = new String(decodedBytes, java.nio.charset.StandardCharsets.UTF_8);
                org.json.JSONObject jsonObject = new org.json.JSONObject(payloadJson);
                Map<String, String> map = new HashMap<>();
                if (jsonObject.has("sub")) map.put("sub", jsonObject.getString("sub"));
                if (jsonObject.has("user_id")) map.put("sub", jsonObject.getString("user_id"));
                if (jsonObject.has("email")) map.put("email", jsonObject.getString("email"));
                if (jsonObject.has("name")) map.put("name", jsonObject.getString("name"));
                if (jsonObject.has("picture")) map.put("picture", jsonObject.getString("picture"));
                return map;
            }
        } catch (Exception ignored) {
        }
        return java.util.Collections.emptyMap();
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

        userAuthProvider.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        userAuthProvider.setUser(newUser);

        userAuthProviderRepository.save(userAuthProvider);

        return buildAuthResponse(newUser, newUser.getEmail());
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

            UserAuthProvider userAuthProvider = userAuthProviderRepository
                    .findByTypeAndProviderUserId(AuthProviderTypeEnum.LOCAL, email)
                    .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

            AuthResponseDTO authResponse = buildAuthResponse(userAuthProvider.getUser(),
                    userAuthProvider.getProviderUserId());

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("isValid", true);
            responseData.put("userId", authResponse.getUserId());
            responseData.put("accessToken", authResponse.getAccessToken());
            responseData.put("refreshToken", authResponse.getRefreshToken());
            responseData.put("expiresIn", authResponse.getExpiresIn());
            return responseData;
        }

        return Map.of("isValid", false);
    }

    @Transactional
    public AuthResponseDTO refreshToken(RefreshTokenRequestDTO requestDTO) {
        if (requestDTO == null || requestDTO.getRefreshToken() == null || requestDTO.getRefreshToken().isBlank()) {
            throw new InvalidCredentialsException("Refresh token is required");
        }

        String incomingRefreshToken = requestDTO.getRefreshToken().trim();
        String userIdStr = redisTemplate.opsForValue().get("RT::" + incomingRefreshToken);

        if (userIdStr == null || userIdStr.isBlank()) {
            throw new InvalidCredentialsException("Invalid or expired refresh token");
        }

        Long userId;
        try {
            userId = Long.valueOf(userIdStr);
        } catch (NumberFormatException e) {
            redisTemplate.delete("RT::" + incomingRefreshToken);
            throw new InvalidCredentialsException("Invalid refresh token payload");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidCredentialsException("User account not found"));

        if (user.getIsActive() == null || !user.getIsActive()) {
            redisTemplate.delete("RT::" + incomingRefreshToken);
            redisTemplate.delete("RT_USER::" + userId);
            throw new InvalidCredentialsException("User account is inactive");
        }

        // Token Rotation: Thu hồi Refresh Token cũ đã sử dụng
        redisTemplate.delete("RT::" + incomingRefreshToken);

        // Lấy providerUserId của user (mặc định ưu tiên provider đầu tiên hoặc email)
        String providerUserId = userAuthProviderRepository.findByUser_Id(userId).stream()
                .findFirst()
                .map(UserAuthProvider::getProviderUserId)
                .orElse(user.getEmail());

        // Cấp cặp token mới và lưu vào Redis
        return buildAuthResponse(user, providerUserId);
    }

    @Transactional
    public Boolean logout(RefreshTokenRequestDTO requestDTO) {
        if (requestDTO != null && requestDTO.getRefreshToken() != null && !requestDTO.getRefreshToken().isBlank()) {
            String incomingRefreshToken = requestDTO.getRefreshToken().trim();
            String userIdStr = redisTemplate.opsForValue().get("RT::" + incomingRefreshToken);
            redisTemplate.delete("RT::" + incomingRefreshToken);
            if (userIdStr != null && !userIdStr.isBlank()) {
                redisTemplate.delete("RT_USER::" + userIdStr);
            }
        }
        return true;
    }

    private AuthResponseDTO buildAuthResponse(User user, String providerUserId) {
        String accessToken = jwtUtil.generateToken(providerUserId, user.getRole());

        byte[] refreshTokenBytes = new byte[50];
        secureRandom.nextBytes(refreshTokenBytes);
        String refreshToken = new String(Hex.encode(refreshTokenBytes));

        // Token Rotation: Xóa Refresh Token cũ của user trong Redis nếu có
        String oldRefreshToken = redisTemplate.opsForValue().get("RT_USER::" + user.getId());
        if (oldRefreshToken != null) {
            redisTemplate.delete("RT::" + oldRefreshToken);
        }

        // Lưu Refresh Token mới vào Redis với TTL 7 ngày
        redisTemplate.opsForValue().set("RT::" + refreshToken, String.valueOf(user.getId()), 7, TimeUnit.DAYS);
        redisTemplate.opsForValue().set("RT_USER::" + user.getId(), refreshToken, 7, TimeUnit.DAYS);

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
    public Boolean resetPassword(String email, String newPassword) {
        UserAuthProvider userAuthProvider = userAuthProviderRepository
                .findByTypeAndProviderUserId(AuthProviderTypeEnum.LOCAL, email)
                .orElseThrow(() -> new InvalidCredentialsException("Email not registered"));

        String code = passwordEncoder.encode(newPassword);

        userAuthProvider.setPassword(code);
        userAuthProviderRepository.save(userAuthProvider);
        return true;
    }
}

package com.example.hotelbooking.util;

import java.security.Key;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.hotelbooking.enums.UserRoleEnum;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

    @Value("${jwt.secret-key}")
    private String secretKey;

    @Value("${jwt.expiration-ms}")
    private Long expirationMs;

    public String generateToken(String email, UserRoleEnum role) {
        Key key = Keys.hmacShaKeyFor(secretKey.getBytes());

        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims extractClaims(String token) {
        Key key = Keys.hmacShaKeyFor(secretKey.getBytes());
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Boolean isTokenValid(String token) {
        try {
            Claims claims = extractClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public String getEmail(String token) {
        return extractClaims(token).getSubject();
    }

    public String getProviderId(String token) {
        return extractClaims(token).getSubject();
    }

    public UserRoleEnum getRole(String token) {
        String roleStr = (String) extractClaims(token).get("role");
        return UserRoleEnum.valueOf(roleStr);
    }

    public Long getExpirationMs() {
        return expirationMs;
    }
}

// import java.util.Date;

// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.stereotype.Component;

// import io.jsonwebtoken.Claims;
// import io.jsonwebtoken.Jwts;
// import io.jsonwebtoken.SignatureAlgorithm;
// import io.jsonwebtoken.security.Keys;
// import java.security.Key;

// @Component
// public class JwtUtil {

// @Value("${jwt.secret-key}")
// private String secretKey;

// public String generateToken(String userName, String role, Long expiration) {
// Key key = Keys.hmacShaKeyFor(secretKey.getBytes());
// return Jwts.builder()
// .setSubject(userName)
// .claim("role", role)
// .setIssuedAt(new Date())
// .setExpiration(new Date(System.currentTimeMillis() + expiration))
// .signWith(key, SignatureAlgorithm.HS256)
// .compact();
// }

// public Claims extractClaims(String token) {
// Key key = Keys.hmacShaKeyFor(secretKey.getBytes());
// return Jwts.parserBuilder()
// .setSigningKey(key)
// .build()
// .parseClaimsJws(token)
// .getBody();
// }

// public Boolean isTokenVaid(String token) {
// try {
// Claims claims = extractClaims(token);
// return !claims.getExpiration().before(new Date());
// } catch (Exception e) {
// return false;
// }
// }

// public String getUserName(String token) {
// return extractClaims(token).getSubject();
// }

// public RoleEnum getRole(String token) {
// String roleStr = (String) extractClaims(token).get("role");
// return RoleEnum.valueOf(roleStr);

// }

// public String getSecretKey() {
// return secretKey;
// }

// public void setSecretKey(String secretKey) {
// this.secretKey = secretKey;
// }

// }

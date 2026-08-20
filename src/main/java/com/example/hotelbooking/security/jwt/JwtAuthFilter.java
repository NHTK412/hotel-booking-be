package com.example.hotelbooking.security.jwt;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.hotelbooking.model.User;
import com.example.hotelbooking.model.UserAuthProvider;
import com.example.hotelbooking.repository.UserAuthProviderRepository;
import com.example.hotelbooking.security.CustomUserDetails;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserAuthProviderRepository userAuthProviderRepository;

    public JwtAuthFilter(JwtUtil jwtUtil, UserAuthProviderRepository userAuthProviderRepository) {
        this.jwtUtil = jwtUtil;
        this.userAuthProviderRepository = userAuthProviderRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authorizationHeader.substring(7);

        if (!jwtUtil.isTokenValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String providerId = jwtUtil.getProviderId(token);

        UserAuthProvider userAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                .orElse(null);

        if (userAuthProvider != null && userAuthProvider.getUser() != null) {
            User user = userAuthProvider.getUser();

            CustomUserDetails userDetails = CustomUserDetails.builder()
                    .providerId(userAuthProvider.getProviderUserId())
                    .authorities(List.of(new SimpleGrantedAuthority(user.getRole().name())))
                    .build();

            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            authenticationToken.setDetails(new WebAuthenticationDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        }

        filterChain.doFilter(request, response);
    }
}

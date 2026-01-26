package com.example.hotelbooking.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import com.example.hotelbooking.exception.customer.AccessDeniedHandlerException;
import com.example.hotelbooking.exception.customer.AuthenticationEntryPointException;
import com.example.hotelbooking.filter.JwtAuthFilter;

@Configuration
public class SecurityConfig {

        final private AuthenticationEntryPointException authenticationEntryPointException;

        final private AccessDeniedHandlerException accessDeniedHandlerException;

        final private JwtAuthFilter jwtAuthFilter;

        SecurityConfig(
                        AuthenticationEntryPointException authenticationEntryPointException,
                        AccessDeniedHandlerException accessDeniedHandlerException,
                        JwtAuthFilter jwtAuthFilter) {
                this.authenticationEntryPointException = authenticationEntryPointException;
                this.accessDeniedHandlerException = accessDeniedHandlerException;
                this.jwtAuthFilter = jwtAuthFilter;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

                // http.cors(cors -> cors.disable());

                // http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

                // http.csrf(csrf -> csrf.disable());

                // return http.build();

                http.cors(cors -> cors.configurationSource((request) -> {

                        CorsConfiguration corsConfiguration = new CorsConfiguration();

                        corsConfiguration.setAllowedOrigins(List.of("*"));

                        corsConfiguration.setAllowedMethods(List.of(
                                        "GET",
                                        "POST",
                                        "PUT",
                                        "DELETE", "PATCH",
                                        "OPTIONS"));

                        corsConfiguration.setAllowedHeaders(List.of(
                                        "Authorization",
                                        "Content-Type",
                                        "Accept",
                                        "Cache-Control",
                                        "X-Requested-With",
                                        "X-Client-Version",
                                        "X-Refresh-Token"));

                        corsConfiguration.setExposedHeaders(List.of("Authorization"));

                        corsConfiguration.setAllowCredentials(null);

                        return corsConfiguration;

                }));

                http.csrf((csrf) -> csrf.disable());

                http.sessionManagement(
                                (session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

                http.authorizeHttpRequests(auth -> auth

                                // .anyRequest().permitAll());
                                // Cho phép truy cập không cần xác thực đến các endpoint sau
                                // cho phép truy cập mà không kiểm tra authentication/roles
                                .requestMatchers(
                                                "/auth/**",
                                                "/swagger-ui/**",
                                                "/swagger-ui.html",
                                                "/v3/api-docs/**",
                                                "/images/**",
                                                "/locations/calculator",
                                                "/zalopay/callback"
                                // "/api/v3/api-docs",
                                // "/api/v3/api-docs/swagger-config",
                                // "/swagger-ui.html",
                                // "/swagger-ui/**"
                                )
                                .permitAll()
                                // Yêu cầu xác thực cho các request khác
                                .anyRequest().authenticated());

                // Xử lý ngoại lệ liên quan đến xác thực và phân quyền
                http.exceptionHandling(ex -> ex
                                .authenticationEntryPoint(authenticationEntryPointException) // 401
                                .accessDeniedHandler(accessDeniedHandlerException)); // 403

                // Dùng để lọc JWT trước khi đến UsernamePasswordAuthenticationFilter
                http.addFilterBefore(jwtAuthFilter,
                                UsernamePasswordAuthenticationFilter.class);

                return http.build();

        }
}

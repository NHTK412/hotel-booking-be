package com.example.hotelbooking.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import com.example.hotelbooking.security.handler.CustomAccessDeniedHandler;
import com.example.hotelbooking.security.handler.CustomAuthenticationEntryPoint;
import com.example.hotelbooking.security.jwt.JwtAuthFilter;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        private final CustomAuthenticationEntryPoint authenticationEntryPoint;
        private final CustomAccessDeniedHandler accessDeniedHandler;
        private final JwtAuthFilter jwtAuthFilter;

        public SecurityConfig(
                        CustomAuthenticationEntryPoint authenticationEntryPoint,
                        CustomAccessDeniedHandler accessDeniedHandler,
                        JwtAuthFilter jwtAuthFilter) {
                this.authenticationEntryPoint = authenticationEntryPoint;
                this.accessDeniedHandler = accessDeniedHandler;
                this.jwtAuthFilter = jwtAuthFilter;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

                http.cors(cors -> cors.configurationSource(request -> {
                        CorsConfiguration corsConfiguration = new CorsConfiguration();
                        corsConfiguration.setAllowedOrigins(List.of("*"));
                        corsConfiguration.setAllowedMethods(List.of(
                                        "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
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

                http.csrf(csrf -> csrf.disable());

                http.sessionManagement(
                                session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

                http.authorizeHttpRequests(auth -> auth
                                .requestMatchers(
                                                "/auth/**",
                                                "/swagger-ui/**",
                                                "/swagger-ui.html",
                                                "/v3/api-docs/**",
                                                "/images/**",
                                                "/locations/**",
                                                "/room-types/**",
                                                "/accommodations/search",
                                                "/accommodations/nearby",
                                                "/zalopay/callback")
                                .permitAll()
                                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                                "/accommodations",
                                                "/accommodations/{accommodationId}",
                                                "/reviews")
                                .permitAll()
                                .anyRequest().authenticated());

                http.exceptionHandling(ex -> ex
                                .authenticationEntryPoint(authenticationEntryPoint)
                                .accessDeniedHandler(accessDeniedHandler));

                http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}

package com.example.hotelbooking.filter;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.hotelbooking.exception.customer.NotFoundException;
import com.example.hotelbooking.model.UserAuthProvider;
import com.example.hotelbooking.model.Users;
import com.example.hotelbooking.repository.UserAuthProviderRepository;
import com.example.hotelbooking.security.CustomerUserDetails;
import com.example.hotelbooking.util.JwtUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

        @Autowired
        private JwtUtil jwtUtil;

        // @Autowired
        // private Auth accountService;

        // @Autowired
        // private CustomerRepository customerRepository;

        // @Autowired
        // private AdminRepository adminRepository;

        final UserAuthProviderRepository userAuthProviderRepository;

        public JwtAuthFilter(UserAuthProviderRepository userAuthProviderRepository) {
                this.userAuthProviderRepository = userAuthProviderRepository;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                        FilterChain filterChain)
                        throws ServletException, IOException {

                final String authorizationHeader = request.getHeader("Authorization");
                if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer")) {
                        filterChain.doFilter(request, response);
                        return;
                }

                final String token = authorizationHeader.substring(7);

                final String providerId = jwtUtil.getProviderId(token);

                UserAuthProvider userAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                                .orElseThrow(() -> new NotFoundException("UserAuthProvider Not Found"));

                Users user = userAuthProvider.getUser();

                CustomerUserDetails userDetails = CustomerUserDetails.builder()
                                .providerId(userAuthProvider.getProviderUserId())
                                .authorities(
                                                List.of(new SimpleGrantedAuthority(user.getRole().name())))
                                .build();

                // final String authorizationHeader = request.getHeader("Authorization");

                // if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer"))
                // {
                // filterChain.doFilter(request, response);
                // return;
                // }

                // final String token = authorizationHeader.substring(7);

                // final String username = jwtUtil.getUserName(token);

                // final RoleEnum role = jwtUtil.getRole(token);

                // if (username != null &&
                // SecurityContextHolder.getContext().getAuthentication() == null) {
                // if (jwtUtil.isTokenVaid(token)) {
                // // UserDetails customerUserDetails =
                // // accountService.getAccountByUsername(username);

                // // UserDetails userDetails = null;
                // CustomerUserDetails userDetails = null;

                // if (role == RoleEnum.ROLE_CUSTOMER) {
                // Customer customer = customerRepository.findByUserName(username)
                // .orElseThrow(() -> new NotFoundException("Username không tồn tại"));

                // // userDetails = User.builder()
                // // .username(customer.getUserName())
                // // .password(customer.getPassword())
                // // .authorities(List.of(new
                // // SimpleGrantedAuthority(RoleEnum.ROLE_CUSTOMER.name())))
                // // .build();
                // userDetails = CustomerUserDetails.builder()
                // .userName(customer.getUserName())
                // .password(customer.getPassword())
                // .userId(customer.getCustomerId())
                // .authorities(List.of(new
                // SimpleGrantedAuthority(RoleEnum.ROLE_CUSTOMER.name())))
                // .build();
                // } else if (role == RoleEnum.ROLE_ADMIN) {
                // Admin admin = adminRepository.findByUserName(username)
                // .orElseThrow(() -> new NotFoundException("Username không tồn tại"));

                // // userDetails = User.builder()
                // // .username(admin.getUserName())
                // // .password(admin.getPassword())
                // // .authorities(List.of(new
                // SimpleGrantedAuthority(RoleEnum.ROLE_ADMIN.name())))
                // // .build();

                // userDetails = CustomerUserDetails.builder()
                // .userName(admin.getUserName())
                // .password(admin.getPassword())
                // .userId(admin.getAdminId())
                // .authorities(List.of(new SimpleGrantedAuthority(RoleEnum.ROLE_ADMIN.name())))
                // .build();

                // } else {
                // throw new RuntimeException("Role không tồn tại");
                // }

                // UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new
                // UsernamePasswordAuthenticationToken(
                // userDetails, null, userDetails.getAuthorities());

                // usernamePasswordAuthenticationToken.setDetails(new
                // WebAuthenticationDetails(request));

                // SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);

                // }
                // }

                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                // tạo đối tượng xác thực với thông tin user và quyền hạn

                usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetails(request));
                // dùng để lưu thông tin về request

                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
                // lưu thông tin xác thực vào SecurityContext

                filterChain.doFilter(request, response);

        }

}

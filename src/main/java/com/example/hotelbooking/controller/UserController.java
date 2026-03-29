package com.example.hotelbooking.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.hotelbooking.dto.user.UserResponseDTO;
import com.example.hotelbooking.security.CustomerUserDetails;
import com.example.hotelbooking.service.UserService;
import com.example.hotelbooking.util.ApiResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;



@RestController
@RequestMapping("/users")
public class UserController {

    final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getUserById(@PathVariable Long userId) {
        // return new String();

        UserResponseDTO userResponseDTO = userService.getUserById(userId);

        ApiResponse<UserResponseDTO> response = new ApiResponse<>(true, "User fetched successfully", userResponseDTO);

        return ResponseEntity.ok(response);

    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getCurrentUser(
            @AuthenticationPrincipal CustomerUserDetails customerUserDetails) {
        String providerId = customerUserDetails.getUsername();
        // return new String();

        UserResponseDTO userResponseDTO = userService.getUserByProviderId(providerId);

        ApiResponse<UserResponseDTO> response = new ApiResponse<>(true, "User fetched successfully", userResponseDTO);

        return ResponseEntity.ok(response);

    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateCurrentUser(
            @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
            @RequestBody com.example.hotelbooking.dto.user.UserReqquestDTO userReqquestDTO) {
        String providerId = customerUserDetails.getUsername();
        // return new String();

        UserResponseDTO userResponseDTO = userService.updateUserByProviderId(providerId, userReqquestDTO);

        ApiResponse<UserResponseDTO> response = new ApiResponse<>(true, "User updated successfully", userResponseDTO);

        return ResponseEntity.ok(response);

    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/host")
    public ResponseEntity<ApiResponse<UserResponseDTO>> registerHost(
            @RequestBody com.example.hotelbooking.dto.user.CreateHostDTO createHostDTO) {

        UserResponseDTO userResponseDTO = userService.registerHost(createHostDTO);

        ApiResponse<UserResponseDTO> response = new ApiResponse<>(true, "Host registered successfully", userResponseDTO);

        return ResponseEntity.ok(response);

    }
    

}

package com.example.hotelbooking.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.hotelbooking.dto.user.UserResponseDTO;
import com.example.hotelbooking.service.UserService;
import com.example.hotelbooking.util.ApiResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

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

}

package com.example.hotelbooking.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.hotelbooking.dto.device.DeviceRegistrationRequest;
import com.example.hotelbooking.security.CustomerUserDetails;
import com.example.hotelbooking.service.DeviceService;
import com.example.hotelbooking.util.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/devices")
@RequiredArgsConstructor
public class DeviceController {

    final private DeviceService deviceService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Boolean>> registerDevice(
            @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
            @RequestBody DeviceRegistrationRequest request) {

        Boolean result = deviceService.registerDevice(customerUserDetails.getProviderId(), request);

        return ResponseEntity.ok(new ApiResponse<>(true, "Device registered successfully", result));

    }

    @PostMapping("/refresh ")
    public ResponseEntity<ApiResponse<Boolean>> refreshDevice(
            @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
            @RequestBody DeviceRegistrationRequest request) {

        Boolean result = deviceService.refreshDevice(request);

        return ResponseEntity.ok(new ApiResponse<>(true, "Device refreshed successfully", result));

    }

}

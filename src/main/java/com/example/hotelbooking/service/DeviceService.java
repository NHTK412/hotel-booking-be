package com.example.hotelbooking.service;

import org.springframework.stereotype.Service;

import com.example.hotelbooking.dto.device.DeviceRegistrationRequest;
import com.example.hotelbooking.exception.customer.NotFoundException;
import com.example.hotelbooking.model.Device;
import com.example.hotelbooking.model.UserAuthProvider;
import com.example.hotelbooking.model.Users;
import com.example.hotelbooking.repository.DeviceRepository;
import com.example.hotelbooking.repository.UserAuthProviderRepository;
import com.example.hotelbooking.repository.UserRepository;
import com.google.cloud.storage.Acl.User;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeviceService {

    final private DeviceRepository deviceRepository;
    final private UserRepository userRepository;
    final private UserAuthProviderRepository userAuthProviderRepository;

    @Transactional
    public Boolean registerDevice(String providerId, DeviceRegistrationRequest deviceRegistrationRequest) {

        UserAuthProvider userAuthProvider = userAuthProviderRepository.findByProviderUserId(providerId)
                .orElseThrow(() -> new NotFoundException("UserAuthProvider not found"));

        Users user = userAuthProvider.getUser();

        Device device = new Device();
        device.setFcmToken(deviceRegistrationRequest.getFcmToken());
        device.setDeviceType(deviceRegistrationRequest.getDeviceType());
        device.setPlatform(deviceRegistrationRequest.getPlatform());
        device.setUser(user);

        // Users user = userRepository.findById(deviceRegistrationRequest.getUserId())
        // .orElseThrow(() -> new NotFoundException("User not found"));

        deviceRepository.save(device);
        return true;
    }

    @Transactional
    public Boolean refreshDevice(DeviceRegistrationRequest deviceRegistrationRequest) {

        // UserAuthProvider userAuthProvider =
        // userAuthProviderRepository.findByProviderUserId(providerId)
        // .orElseThrow(() -> new NotFoundException("UserAuthProvider not found"));

        // Users user = userAuthProvider.getUser();

        // Device device = new Device();
        // device.setFcmToken(deviceRegistrationRequest.getFcmToken());
        // device.setDeviceType(deviceRegistrationRequest.getDeviceType());
        // device.setPlatform(deviceRegistrationRequest.getPlatform());
        // device.setUser(user);

        // Users user = userRepository.findById(deviceRegistrationRequest.getUserId())
        // .orElseThrow(() -> new NotFoundException("User not found"));

        Device device = deviceRepository.findByDeviceType(deviceRegistrationRequest.getDeviceType())
                .orElseThrow(() -> new NotFoundException("Device not found"));

        device.setFcmToken(deviceRegistrationRequest.getFcmToken());

        deviceRepository.save(device);
        return true;
    }

}

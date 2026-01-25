package com.example.hotelbooking.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.hotelbooking.model.Device;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {
    
    // Define repository methods here

    Optional<Device> findByDeviceType(String deviceType);
}
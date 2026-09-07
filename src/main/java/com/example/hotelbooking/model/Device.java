package com.example.hotelbooking.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "device", indexes = {
    @Index(name = "idx_device_user", columnList = "userId"),
    @Index(name = "idx_device_type", columnList = "deviceType")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long deviceId;

    private String fcmToken;

    private String deviceType; // e.g., "Android", "iOS", "Web"

    private String platform; // e.g., "Mobile", "Desktop"

    @ManyToOne
    @JoinColumn(name = "userId", nullable = false)
    private User user;
}

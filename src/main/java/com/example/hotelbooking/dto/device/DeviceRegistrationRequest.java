package com.example.hotelbooking.dto.device;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class DeviceRegistrationRequest {

    private String fcmToken;
    private String deviceType; // e.g., "Android", "iOS", "Web"
    private String platform; // e.g., "Mobile", "Desktop"

    private Long userId; // ID of the user to whom the device belongs
    
}

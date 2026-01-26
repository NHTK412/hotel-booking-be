package com.example.hotelbooking.config;

import java.io.FileInputStream;
import java.io.IOException;

import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import jakarta.annotation.PostConstruct;

@Configuration
public class FirebaseConfig {

    @PostConstruct // Nghĩa là phương thức này sẽ được gọi sau khi bean được khởi tạo
    public void init() throws IOException {
        // Đường dẫn tới file JSON chứa thông tin xác thực của Firebase
        FileInputStream serviceAccount = new FileInputStream(
                "D:/Programming_Language/Java/Project_LTTBDT_UTH/hotelbooking/booking-hotel-app-43981-firebase-adminsdk-fbsvc-c91a1a3701.json");

        FirebaseOptions options = new FirebaseOptions.Builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options);
        }
    }

}

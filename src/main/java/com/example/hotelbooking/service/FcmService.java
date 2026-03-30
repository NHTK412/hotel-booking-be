package com.example.hotelbooking.service;

import org.springframework.stereotype.Service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

@Service
public class FcmService {

        public String sendNotification(
                        String title,
                        String body,
                        String token) throws FirebaseMessagingException {

                Notification notification = Notification
                                .builder()
                                .setTitle(title)
                                .setBody(body)
                                .build();

                Message message = Message
                                .builder()
                                .setNotification(notification)
                                .setToken(token)
                                .build();

                return FirebaseMessaging.getInstance().send(message);

        }
}

package com.example.hotelbooking.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@DisplayName("Unit Tests for ZaloPay Configuration Properties")
class ZaloPayPropertiesTest {

    @Autowired
    private ZaloPayProperties zaloPayProperties;

    @Test
    @DisplayName("Should inject ZaloPayProperties with non-null values from application.properties")
    void testZaloPayPropertiesLoaded() {
        assertNotNull(zaloPayProperties);
        assertEquals("2553", zaloPayProperties.getAppId());
        assertNotNull(zaloPayProperties.getKey1());
        assertNotNull(zaloPayProperties.getKey2());
        assertNotNull(zaloPayProperties.getEndpoint());
        assertNotNull(zaloPayProperties.getCallbackUrl());
        assertNotNull(zaloPayProperties.getRedirectUrl());
    }
}

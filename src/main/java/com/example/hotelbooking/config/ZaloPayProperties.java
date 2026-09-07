package com.example.hotelbooking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cấu hình thông số kết nối ZaloPay Payment Gateway thông qua ConfigurationProperties.
 * Giá trị được inject tự động từ application.properties / application-prod.properties / Environment Variables.
 */
@Configuration
@ConfigurationProperties(prefix = "zalopay")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ZaloPayProperties {

    /**
     * Mã định danh ứng dụng (App ID) do ZaloPay cấp
     */
    private String appId;

    /**
     * Khóa bí mật thứ 1 (Key 1) dùng để tạo chữ ký HMAC SHA256 khi tạo đơn hàng
     */
    private String key1;

    /**
     * Khóa bí mật thứ 2 (Key 2) dùng để xác thực chữ ký HMAC callback IPN từ máy chủ ZaloPay
     */
    private String key2;

    /**
     * Endpoint API tạo đơn hàng (Sandbox: https://sb-openapi.zalopay.vn/v2/create)
     */
    private String endpoint;

    /**
     * URL Webhook callback nhận kết quả giao dịch IPN từ ZaloPay
     */
    private String callbackUrl;

    /**
     * URL redirect người dùng sau khi thanh toán trên cổng ZaloPay
     */
    private String redirectUrl;
}

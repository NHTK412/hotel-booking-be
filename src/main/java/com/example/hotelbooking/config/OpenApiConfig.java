package com.example.hotelbooking.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Hệ Thống Đặt Phòng Khách Sạn (Hotel Booking API)",
                version = "1.0.0",
                description = "Tài liệu API chính thức cho Hệ thống Đặt phòng Khách sạn & Homestay. "
                        + "Hỗ trợ xác thực đa phân quyền (Admin, Host, Customer), xác thực OAuth Google/Facebook với chữ ký số, "
                        + "quản lý đặt phòng, tìm kiếm theo vị trí địa lý GeoHash, đánh giá & nhận xét, và tích hợp thanh toán trực tuyến ZaloPay."
        ),
        security = {
                @SecurityRequirement(name = "BearerAuth")
        }
)
@SecurityScheme(
        name = "BearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {
}

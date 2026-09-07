package com.example.hotelbooking.dto.zalopay;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO (Data Transfer Object) chứa thông tin yêu cầu tạo đơn hàng ZaloPay
 * Class này được sử dụng để nhận dữ liệu từ client khi gọi API tạo đơn hàng
 */
@Data
public class CreateOrderRequest {

    @NotNull(message = "Mã đơn đặt phòng (bookingId) không được để trống")
    private Long bookingId;

    private String description;
}

package com.example.hotelbooking.dto.booking;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class BookingDetailDTO {
    private Long bookingId;

    // customer info
    private String customerName;
    private String customerPhone;
    private String customerEmail;

    // booking dates
    // private LocalDate checkInDate;
    // private LocalDate checkOutDate;
    private LocalDateTime checkInAt;
    private LocalDateTime checkOutAt;

    // prices
    private Double originalPrice;
    private Double discountedPrice;
    private Double finalPrice;

    // booking status
    private String status;

    // Thông tin phòng
    private String accommodationName;
    private String roomType;
    private String roomNumber;
    private Double lat;
    private Double lng;

    // Thông tin đánh giá (nếu có)
    private Long reviewId;

}

package com.example.hotelbooking.dto.booking;

import java.time.LocalDate;

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
    private LocalDate checkInDate;
    private LocalDate checkOutDate;

    // prices
    private Double originalPrice;
    private Double discountedPrice;
    private Double finalPrice;

    // booking status
    private String status;

}

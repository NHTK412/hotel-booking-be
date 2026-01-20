package com.example.hotelbooking.dto.booking;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class BookingSummaryDTO {

    private Long bookingId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String status;
    private Double finalPrice;
}

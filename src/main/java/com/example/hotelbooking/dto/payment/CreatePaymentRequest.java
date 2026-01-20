package com.example.hotelbooking.dto.payment;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePaymentRequest {
    private Long bookingId;
}

package com.example.hotelbooking.dto.booking;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequestDTO {

    // customer info
    private String customerName;
    private String customerPhone;
    private String customerEmail;

    // booking dates
    private LocalDate checkInDate;
    private LocalDate checkOutDate;

    // Id room
    private Long roomTypeId;
}

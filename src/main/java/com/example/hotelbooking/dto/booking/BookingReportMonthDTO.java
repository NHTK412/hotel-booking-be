package com.example.hotelbooking.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingReportMonthDTO {
    Integer month;
    Long totalBookings;

    // @Override
    // public String toString() {
    //     return "BookingReportMonthDTO [month=" + month + ", totalBookings=" + totalBookings + "]";
    // }
}

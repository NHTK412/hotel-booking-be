package com.example.hotelbooking.model;

import java.time.LocalDate;

import com.example.hotelbooking.enums.BookingStatusEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Bookings")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Bookings extends Base {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bookingId")
    private Long bookingId;

    @Column(name = "checkInDate", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "checkOutDate", nullable = false)
    private LocalDate checkOutDate;

    @Column(name = "originalPrice", nullable = false)
    private Double originalPrice;

    @Column(name = "discountedPrice")
    private Double discountedPrice;

    @Column(name = "finalPrice", nullable = false)
    private Double finalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BookingStatusEnum status;

    // tên người đặt phòng
    @Column(name = "customerName", nullable = false)
    private String customerName;

    // số điện thoại người đặt phòng
    @Column(name = "customerPhone", nullable = false)
    private String customerPhone;

    @Column(name = "customerEmail", nullable = false)
    private String customerEmail;

    @ManyToOne
    @JoinColumn(name = "userId", nullable = false)
    private Users user;

    @ManyToOne
    @JoinColumn(name = "roomId", nullable = false)
    private Rooms room;

    @OneToOne
    @JoinColumn(name = "reviewId", nullable = true)
    private Review review = null;

}

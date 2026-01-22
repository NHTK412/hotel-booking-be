package com.example.hotelbooking.model;

import com.example.hotelbooking.enums.PaymentStatusEnum;
import com.example.hotelbooking.enums.ProviderEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Payments")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Payment extends Base {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "paymentId")
    private Long paymentId;

    @ManyToOne
    @JoinColumn(name = "bookingId", nullable = false)
    private Bookings booking;

    @Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private ProviderEnum provider;

    @Column(name = "providerTransId", nullable = false)
    private String providerTransId;

    @Column(name = "amount", nullable = false)
    private Double amount;

    @Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatusEnum status; // PENDING / SUCCESS / FAILED

    @Column(name = "rawCallbackData", columnDefinition = "TEXT")
    private String rawCallbackData; // JSON

}

// Payments
// - paymentId
// - bookingId (FK)
// - provider (ZALOPAY / VNPAY / PAYPAL)
// - providerTransId (app_trans_id, vnp_TxnRef, paypal_order_id)
// - amount
// - status (PENDING / SUCCESS / FAILED)
// - rawCallbackData (JSON)

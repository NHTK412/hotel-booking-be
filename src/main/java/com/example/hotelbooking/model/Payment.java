package com.example.hotelbooking.model;

import com.example.hotelbooking.enums.PaymentStatusEnum;
import com.example.hotelbooking.enums.ProviderEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Payments", indexes = {
    @Index(name = "idx_payment_provider_trans", columnList = "provider, providerTransId"),
    @Index(name = "idx_payment_booking", columnList = "bookingId")
})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "paymentId")
    private Long paymentId;

    @ManyToOne
    @JoinColumn(name = "bookingId", nullable = false)
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private ProviderEnum provider;

    @Column(name = "providerTransId", nullable = false)
    private String providerTransId;

    @Column(name = "amount", nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatusEnum status; // PENDING / SUCCESS / FAILED

    @Column(name = "rawCallbackData", columnDefinition = "TEXT")
    private String rawCallbackData; // JSON
}

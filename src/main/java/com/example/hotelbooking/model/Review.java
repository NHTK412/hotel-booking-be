package com.example.hotelbooking.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "Reviews")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Review  extends Base {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reviewId")
    private Long reviewId;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "comment", length = 2000)
    private String comment;

    @ManyToOne
    @JoinColumn(name = "userId", nullable = false)
    private Users user;

    @OneToOne
    @JoinColumn(name = "bookingId", nullable = false)
    private Bookings booking;

    @ManyToOne
    @JoinColumn(name = "roomtypeId", nullable = false)
    private RoomTypes roomType;

}

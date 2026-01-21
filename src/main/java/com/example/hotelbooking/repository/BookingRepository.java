package com.example.hotelbooking.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.hotelbooking.enums.BookingStatusEnum;
import com.example.hotelbooking.model.Bookings;


@Repository
public interface BookingRepository extends JpaRepository<Bookings, Long> {

    @Query("""
            SELECT b
            FROM Bookings b
            WHERE b.bookingId = :bookingId
                """)
    Optional<Bookings> findByIdPayment(Long bookingId);

    Page<Bookings> findByRoom_RoomType_Accommodation_AccommodationId(Long accommodationId,
            org.springframework.data.domain.Pageable pageable);

    // private LocalDateTime checkInAt;

    @Query("""
                SELECT b
                FROM Bookings b
                WHERE (:start IS NULL OR b.checkInAt >= :start)
                    AND (:end IS NULL OR b.checkInAt < :end)
                    AND (:status IS NULL OR b.status = :status)
                    AND b.user.userAuthProvider.providerUserId = :providerId
            """)
    Page<Bookings> findBookingsByCustomer(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("status") BookingStatusEnum status,
            @Param("providerId") String providerId,
            Pageable pageable);

}
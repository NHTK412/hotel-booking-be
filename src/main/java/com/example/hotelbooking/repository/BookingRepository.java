package com.example.hotelbooking.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.hotelbooking.model.Bookings;

@Repository
public interface BookingRepository extends JpaRepository<Bookings, Long> {

    @Query("""
            SELECT b
            FROM Bookings b
            WHERE b.bookingId = :bookingId
                """)
    Optional<Bookings> findByIdPayment(Long bookingId);

    Page<Bookings> findByRoom_RoomType_Accommodation_AccommodationId(Long accommodationId, org.springframework.data.domain.Pageable pageable);

}
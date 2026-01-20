package com.example.hotelbooking.repository;

import java.time.LocalDateTime;

import org.antlr.v4.runtime.atn.SemanticContext.AND;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.hotelbooking.model.RoomTypes;

@Repository
public interface RoomTypeRepository extends JpaRepository<RoomTypes, Long> {

    // SELECT rt.*
    // FROM
    // accommodations a
    // JOIN room_types rt ON rt.accommodation_id = a.accommodation_id
    // JOIN rooms r ON r.roomtype_id = rt.roomtype_id
    // WHERE
    // a.city = 'Đà Nẵng'
    // AND rt.capacity >= 2
    // AND NOT EXISTS (
    // SELECT 1
    // FROM bookings b
    // WHERE
    // b.room_id = r.room_id
    // AND b.status != 'CANCELLED'
    // AND NOT(
    // b.check_out_date <= '2026-01-03'
    // OR b.check_in_date >= '2026-01-05'
    // )
    // );

    @Query("""
            SELECT DISTINCT rt
                FROM RoomTypes rt
                JOIN rt.accommodation a
                JOIN rt.rooms r
                WHERE
                    (:district IS NULL OR a.district = :district)
                    AND (:city IS NULL OR a.city = :city)
                    AND (:capacity IS NULL OR rt.capacity >= :capacity)
                    AND (:bedroom IS NULL OR rt.bedroom >= :bedroom)
                    AND (
                        :checkInAt IS NULL OR :checkOutAt IS NULL OR NOT EXISTS (
                            SELECT 1
                                FROM Bookings b
                                WHERE b.room = r
                                    AND b.status != 'CANCELLED'
                                    AND NOT (
                                        b.checkOutAt <= :checkInAt
                                        OR b.checkInAt >= :checkOutAt
                                    )
                        )
                    )
                """)
    Page<RoomTypes> findAvailableRoomTypes(
            String district,
            String city,
            Integer capacity,
            Integer bedroom,
            LocalDateTime checkInAt,
            LocalDateTime checkOutAt,
            Pageable pageable);

    Page<RoomTypes> findByAccommodation_AccommodationIdAndIsDeletedFalse(Long accommodationId, Pageable pageable);
}

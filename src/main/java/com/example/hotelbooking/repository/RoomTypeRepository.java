package com.example.hotelbooking.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.hotelbooking.model.RoomType;

@Repository
public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {

    @Query("""
            SELECT DISTINCT rt
            FROM RoomType rt
            JOIN rt.accommodation a
            JOIN rt.rooms r
            JOIN a.location l
            WHERE
                l.locationId = :locationId
                AND rt.isDeleted = false
                AND rt.status = com.example.hotelbooking.enums.StatusEnum.ACTIVE
                AND a.isDeleted = false
                AND r.isDeleted = false
                AND r.status = com.example.hotelbooking.enums.StatusEnum.ACTIVE
                AND (:capacity IS NULL OR rt.capacity >= :capacity)
                AND (:bedroom IS NULL OR rt.bedroom >= :bedroom)
                AND (
                    :checkInAt IS NULL OR :checkOutAt IS NULL OR NOT EXISTS (
                        SELECT 1
                        FROM Booking b
                        WHERE b.room = r
                            AND b.status != com.example.hotelbooking.enums.BookingStatusEnum.CANCELED
                            AND NOT (
                                b.checkOutAt <= :checkInAt
                                OR b.checkInAt >= :checkOutAt
                            )
                    )
                )
            """)
    Page<RoomType> findAvailableRoomTypes(
            Long locationId,
            Integer capacity,
            Integer bedroom,
            LocalDateTime checkInAt,
            LocalDateTime checkOutAt,
            Pageable pageable);

    Page<RoomType> findByAccommodation_AccommodationIdAndIsDeletedFalse(Long accommodationId, Pageable pageable);

    Page<RoomType> findByAccommodation_AccommodationIdInAndIsDeletedFalse(List<Long> accommodationIds, Pageable pageable);

    Page<RoomType> findByAccommodation_AccommodationIdAndIsDeleted(Long accommodationId, Boolean isDeleted, Pageable pageable);

    Page<RoomType> findByAccommodation_AccommodationIdInAndIsDeleted(List<Long> accommodationIds, Boolean isDeleted, Pageable pageable);
}

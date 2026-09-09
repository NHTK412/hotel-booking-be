package com.example.hotelbooking.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.hotelbooking.enums.StatusEnum;
import com.example.hotelbooking.model.Room;

import jakarta.persistence.LockModeType;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByRoomType_roomtypeIdAndStatus(Long roomTypeId, StatusEnum status);

    @Query("""
            SELECT DISTINCT r
            FROM Room r
            JOIN r.roomType rt
            WHERE rt.roomtypeId = :roomTypeId
                AND rt.isDeleted = false
                AND rt.status = com.example.hotelbooking.enums.StatusEnum.ACTIVE
                AND r.isDeleted = false
                AND r.status = com.example.hotelbooking.enums.StatusEnum.ACTIVE
                AND NOT EXISTS (
                    SELECT 1
                    FROM Booking b
                    WHERE b.room = r
                        AND b.status != com.example.hotelbooking.enums.BookingStatusEnum.CANCELED
                        AND b.checkInAt < :checkOutAt
                        AND b.checkOutAt > :checkInAt
                )
            """)
    List<Room> findRoomAvailableByRoomTypeId(
            @Param("roomTypeId") Long roomTypeId,
            @Param("checkInAt") LocalDateTime checkInAt,
            @Param("checkOutAt") LocalDateTime checkOutAt);

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("SELECT r FROM Room r WHERE r.roomId = :roomId")
    Optional<Room> findByIdWithOptimisticLock(@Param("roomId") Long roomId);
}

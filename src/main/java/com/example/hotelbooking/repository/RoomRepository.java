package com.example.hotelbooking.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.hotelbooking.enums.StatusEnum;
import com.example.hotelbooking.model.Room;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByRoomType_roomtypeIdAndStatus(Long roomTypeId, StatusEnum status);

    @Query("""
            SELECT DISTINCT r
            FROM Room r
            JOIN r.roomType rt
            WHERE rt.roomtypeId = :roomTypeId
                AND r.status = 'ACTIVE'
                AND NOT EXISTS (
                    SELECT 1
                    FROM Booking b
                    WHERE b.room = r
                        AND b.status != 'CANCELLED'
                        AND b.checkInAt < :checkOutAt
                        AND b.checkOutAt > :checkInAt
                )
            """)
    List<Room> findRoomAvailableByRoomTypeId(
            Long roomTypeId,
            LocalDateTime checkInAt,
            LocalDateTime checkOutAt);
}

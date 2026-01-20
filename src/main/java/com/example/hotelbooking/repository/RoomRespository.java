package com.example.hotelbooking.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.hotelbooking.enums.StatusEnum;
import com.example.hotelbooking.model.Rooms;

@Repository
public interface RoomRespository extends JpaRepository<Rooms, Long> {

    List<Rooms> findByRoomType_roomtypeIdAndStatus(Long roomTypeId, StatusEnum status);

    // @Query("""
    // SELECT DISTINCT r
    // FROM Rooms r
    // JOIN r.roomType rt
    // WHERE rt.roomtypeId = :roomTypeId
    // AND r.status = 'ACTIVE'
    // AND NOT EXISTS (
    // SELECT 1
    // FROM Bookings b
    // WHERE b.room = r
    // AND b.status != 'CANCELLED'
    // AND NOT (
    // b.checkOutDate <= :checkInDate
    // OR b.checkInDate >= :checkOutDate
    // )
    // )
    // """)
    // List<Rooms> findRoomAvailableByRoomTypeId(Long roomTypeId, LocalDate
    // checkInDate, LocalDate checkOutDate);

    @Query("""
                SELECT DISTINCT r
                FROM Rooms r
                JOIN r.roomType rt
                WHERE rt.roomtypeId = :roomTypeId
                    AND r.status = 'ACTIVE'
                    AND NOT EXISTS (
                        SELECT 1
                        FROM Bookings b
                        WHERE b.room = r
                            AND b.status != 'CANCELLED'
                            AND b.checkInAt < :checkOutAt
                            AND b.checkOutAt > :checkInAt
                    )
            """)
    List<Rooms> findRoomAvailableByRoomTypeId(
            Long roomTypeId,
            LocalDateTime checkInAt,
            LocalDateTime checkOutAt);

}

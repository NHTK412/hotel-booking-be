package com.example.hotelbooking.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
                        WHERE
                                (:start IS NULL OR b.checkInAt >= :start)
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

        @Query("""
                        SELECT b
                        FROM Bookings b
                        WHERE b.room.roomType.accommodation.accommodationId = :accommodationId
                        AND (:status IS NULL OR b.status = :status)
                                """)
        Page<Bookings> findBookingsByHost(
                        @Param("accommodationId") Long accommodationId,
                        @Param("status") BookingStatusEnum status,
                        Pageable pageable);

        // {
        // "totalBookings": 120,
        // "completedBookings": 95,
        // "canceledBookings": 25,
        // "totalRevenue": 180000000.0,
        // "averageBookingValue": 1500000.0,
        // "occupancyRate": 78.5,
        // "totalGuests": 260,
        // "totalNights": 410
        // }

        // Map<String, Object> statistics = bookingRepository.fetchBookingStatistics(
        // accommodationId, startDateTime, endDateTime);

        @Query(nativeQuery = true, value = """
                        SELECT
                                SUM(CASE WHEN b.status = 'CHECKED_OUT' THEN 1 ELSE 0 END) AS completedBookings, -- Đếm số booking đã hoàn thành
                                SUM(CASE WHEN b.status = 'CANCELED' THEN 1 ELSE 0 END) AS canceledBookings, -- Đếm số booking đã hủy
                                SUM(CASE WHEN b.status = 'CHECKED_OUT' THEN b.final_price ELSE 0 END) AS totalRevenue, -- Đếm số doanh thu từ booking đã hoàn thành
                                AVG(CASE WHEN b.status = 'CHECKED_OUT' THEN b.final_price ELSE NULL END) AS averageBookingValue, -- Đếm số giá trị trung bình của booking đã hoàn thành
                                SUM(DATEDIFF(b.check_out_at, b.check_in_at)) AS totalNights
                        FROM bookings b
                        INNER JOIN rooms r ON b.room_id = r.room_id
                        INNER JOIN room_types rt ON r.roomtype_id = rt.roomtype_id
                        INNER JOIN accommodations a ON rt.accommodation_id = a.accommodation_id
                        WHERE
                                a.accommodation_id = :accommodationId
                                AND b.check_in_at >= :startDateTime
                                AND b.check_out_at <= :endDateTime
                        """)
        Map<String, Object> fetchBookingStatistics(
                        @Param("accommodationId") Long accommodationId,
                        @Param("startDateTime") LocalDateTime startDateTime,
                        @Param("endDateTime") LocalDateTime endDateTime);

        // Doanh thu theo loại phòng
        // {
        // "roomTypeId": 1,
        // "roomTypeName": "Deluxe",
        // "totalBookings": 40,
        // "totalRevenue": 80000000.0,
        // "averagePrice": 2000000.0,
        // "occupancyRate": 85.0
        // }

        @Query(nativeQuery = true, value = """
                        SELECT
                                rt.roomtype_id AS roomTypeId,
                                rt.name AS roomTypeName,
                                COUNT(b.booking_id) AS totalBookings,
                                SUM(CASE WHEN b.status = 'CHECKED_OUT' THEN b.final_price ELSE 0 END) AS totalRevenue,
                                AVG(CASE WHEN b.status = 'CHECKED_OUT' THEN b.final_price ELSE NULL END) AS averagePrice
                        FROM bookings b
                        INNER JOIN rooms r ON b.room_id = r.room_id
                        INNER JOIN room_types rt ON r.roomtype_id = rt.roomtype_id
                        INNER JOIN accommodations a ON rt.accommodation_id = a.accommodation_id
                                WHERE a.accommodation_id = :accommodationId
                                AND b.check_in_at >= :startDateTime
                                AND b.check_out_at <= :endDateTime
                        GROUP BY rt.roomtype_id, rt.name
                        """)
        List<Map<String, Object>> fetchBookingTrends(
                        @Param("accommodationId") Long accommodationId,
                        @Param("startDateTime") LocalDateTime startDateTime,
                        @Param("endDateTime") LocalDateTime endDateTime);

}
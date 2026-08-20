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
import com.example.hotelbooking.model.Booking;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

        @Query("""
                        SELECT b
                        FROM Booking b
                        WHERE b.bookingId = :bookingId
                        """)
        Optional<Booking> findByIdPayment(Long bookingId);

        Page<Booking> findByRoom_RoomType_Accommodation_AccommodationId(Long accommodationId, Pageable pageable);

        @Query("""
                        SELECT b
                        FROM Booking b
                        WHERE
                                (:start IS NULL OR b.checkInAt >= :start)
                                AND (:end IS NULL OR b.checkInAt < :end)
                                AND (:status IS NULL OR b.status = :status)
                                AND b.user.userAuthProvider.providerUserId = :providerId
                        """)
        Page<Booking> findBookingsByCustomer(
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end,
                        @Param("status") BookingStatusEnum status,
                        @Param("providerId") String providerId,
                        Pageable pageable);

        @Query("""
                        SELECT b
                        FROM Booking b
                        WHERE b.room.roomType.accommodation.accommodationId = :accommodationId
                        AND (:status IS NULL OR b.status = :status)
                                """)
        Page<Booking> findBookingsByHost(
                        @Param("accommodationId") Long accommodationId,
                        @Param("status") BookingStatusEnum status,
                        Pageable pageable);

        @Query(nativeQuery = true, value = """
                        SELECT
                                SUM(CASE WHEN b.status = 'CHECKED_OUT' THEN 1 ELSE 0 END) AS completedBookings,
                                SUM(CASE WHEN b.status = 'CANCELED' THEN 1 ELSE 0 END) AS canceledBookings,
                                SUM(CASE WHEN b.status = 'CHECKED_OUT' THEN b.final_price ELSE 0 END) AS totalRevenue,
                                AVG(CASE WHEN b.status = 'CHECKED_OUT' THEN b.final_price ELSE NULL END) AS averageBookingValue,
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

        List<Booking> findByStatusAndExpiredAtBefore(BookingStatusEnum status, LocalDateTime dateTime);

        List<Booking> findByStatusAndCheckInAtBefore(BookingStatusEnum status, LocalDateTime dateTime);
}
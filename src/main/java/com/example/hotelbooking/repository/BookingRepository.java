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
                                AND b.user.id = :userId
                        """)
        Page<Booking> findBookingsByCustomer(
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end,
                        @Param("status") BookingStatusEnum status,
                        @Param("userId") Long userId,
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

        @Query("""
                        SELECT COUNT(b)
                        FROM Booking b
                        WHERE b.room.roomType.accommodation.accommodationId = :accommodationId
                          AND b.status = :status
                          AND b.checkInAt < :end
                          AND b.checkOutAt > :start
                        """)
        Long countGuestsInPeriod(
                        @Param("accommodationId") Long accommodationId,
                        @Param("status") BookingStatusEnum status,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        @Query("""
                        SELECT COUNT(b)
                        FROM Booking b
                        WHERE b.room.roomType.accommodation.accommodationId = :accommodationId
                          AND b.status = :status
                          AND b.checkInAt >= :start
                          AND b.checkInAt < :end
                        """)
        Long countCheckInsBetween(
                        @Param("accommodationId") Long accommodationId,
                        @Param("status") BookingStatusEnum status,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        @Query("""
                        SELECT COALESCE(SUM(b.finalPrice), 0.0)
                        FROM Booking b
                        WHERE b.room.roomType.accommodation.accommodationId = :accommodationId
                          AND b.status = :status
                          AND b.checkOutAt >= :start
                          AND b.checkOutAt < :end
                        """)
        Double calculateRevenueBetween(
                        @Param("accommodationId") Long accommodationId,
                        @Param("status") BookingStatusEnum status,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        @Query(nativeQuery = true, value = """
                        SELECT
                                MONTH(b.check_out_at) AS month,
                                COALESCE(SUM(b.final_price), 0.0) AS revenue
                        FROM bookings b
                        INNER JOIN rooms r ON b.room_id = r.room_id
                        INNER JOIN room_types rt ON r.roomtype_id = rt.roomtype_id
                        WHERE rt.accommodation_id = :accommodationId
                          AND b.status = 'CHECKED_OUT'
                          AND YEAR(b.check_out_at) = :year
                        GROUP BY MONTH(b.check_out_at)
                        ORDER BY MONTH(b.check_out_at) ASC
                        """)
        List<Map<String, Object>> fetchMonthlyRevenue(
                        @Param("accommodationId") Long accommodationId,
                        @Param("year") int year);

        @Query(nativeQuery = true, value = """
                        SELECT
                                YEAR(b.check_out_at) AS year,
                                COALESCE(SUM(b.final_price), 0.0) AS revenue
                        FROM bookings b
                        INNER JOIN rooms r ON b.room_id = r.room_id
                        INNER JOIN room_types rt ON r.roomtype_id = rt.roomtype_id
                        WHERE rt.accommodation_id = :accommodationId
                          AND b.status = 'CHECKED_OUT'
                        GROUP BY YEAR(b.check_out_at)
                        ORDER BY YEAR(b.check_out_at) ASC
                        """)
        List<Map<String, Object>> fetchYearlyRevenue(
                        @Param("accommodationId") Long accommodationId);

        @Query("""
                        SELECT COUNT(b)
                        FROM Booking b
                        WHERE b.room.roomType.accommodation.accommodationId = :accommodationId
                          AND b.checkInAt >= :start
                          AND b.checkInAt < :end
                        """)
        Long countBookingsByCheckInBetween(
                        @Param("accommodationId") Long accommodationId,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        @Query("""
                        SELECT COUNT(b)
                        FROM Booking b
                        WHERE b.room.roomType.accommodation.accommodationId = :accommodationId
                          AND b.status = com.example.hotelbooking.enums.BookingStatusEnum.CANCELED
                          AND b.checkInAt >= :start
                          AND b.checkInAt < :end
                        """)
        Long countCanceledBookingsBetween(
                        @Param("accommodationId") Long accommodationId,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        @Query(nativeQuery = true, value = """
                        SELECT COALESCE(SUM(DATEDIFF(b.check_out_at, b.check_in_at)), 0)
                        FROM bookings b
                        INNER JOIN rooms r ON b.room_id = r.room_id
                        INNER JOIN room_types rt ON r.roomtype_id = rt.roomtype_id
                        WHERE rt.accommodation_id = :accommodationId
                          AND b.status != 'CANCELED'
                          AND b.check_in_at >= :start
                          AND b.check_out_at <= :end
                        """)
        Long calculateTotalNightsBetween(
                        @Param("accommodationId") Long accommodationId,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

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
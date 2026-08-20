package com.example.hotelbooking.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.hotelbooking.model.Review;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByRoomType_RoomtypeId(Long roomTypeId, Pageable pageable);

    boolean existsByBooking_BookingId(Long bookingId);

    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM Review r WHERE r.roomType.roomtypeId = :roomTypeId")
    Double calculateAverageRatingByRoomTypeId(@Param("roomTypeId") Long roomTypeId);
}

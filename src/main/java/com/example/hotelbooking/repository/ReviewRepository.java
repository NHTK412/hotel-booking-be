package com.example.hotelbooking.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.hotelbooking.model.Review;

@Repository
public interface ReviewRepository extends JpaRepository<com.example.hotelbooking.model.Review, Long> {


    Page<Review> findByRoomType_RoomtypeId(Long roomTypeId, Pageable pageable);

}

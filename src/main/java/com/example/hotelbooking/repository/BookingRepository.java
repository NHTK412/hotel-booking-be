package com.example.hotelbooking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.hotelbooking.model.Bookings;

@Repository
public interface BookingRepository extends JpaRepository<Bookings, Long> {

    
}
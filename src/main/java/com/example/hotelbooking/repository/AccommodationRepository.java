package com.example.hotelbooking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.hotelbooking.model.Accommodations;

@Repository
public interface AccommodationRepository extends JpaRepository<Accommodations, Long> {

}

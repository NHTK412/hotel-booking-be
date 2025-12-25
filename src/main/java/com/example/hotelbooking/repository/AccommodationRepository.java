package com.example.hotelbooking.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.hotelbooking.model.Accommodations;

@Repository
public interface AccommodationRepository extends JpaRepository<Accommodations, Long> {

    Page<Accommodations> findByIsDeletedFalse(Pageable pageable);
}

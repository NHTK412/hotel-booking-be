package com.example.hotelbooking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.hotelbooking.model.RoomTypes;

@Repository
public interface RoomTypeRepository  extends JpaRepository<RoomTypes, Long> {

}

package com.example.hotelbooking.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.hotelbooking.enums.StatusEnum;
import com.example.hotelbooking.model.Rooms;

@Repository
public interface RoomRespository extends JpaRepository<Rooms, Long> {

    List<Rooms> findByRoomType_roomtypeIdAndStatus(Long roomTypeId, StatusEnum status);
}

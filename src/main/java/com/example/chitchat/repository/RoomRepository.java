package com.example.chitchat.repository;

import com.example.chitchat.entity.RoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RoomRepository extends JpaRepository<RoomEntity, UUID>{
    List<RoomEntity> findByRoomIdIn(List<UUID> roomIds);
}
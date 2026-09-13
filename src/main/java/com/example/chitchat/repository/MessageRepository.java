package com.example.chitchat.repository;

import com.example.chitchat.entity.MessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<MessageEntity, UUID> {
    @Query("""
    SELECT m
    FROM MessageEntity m
    WHERE m.roomId = :roomId
      AND m.isDeleted = false
    ORDER BY m.createdTimestamp ASC
    """)
    List<MessageEntity> findRecentMessages(
            @Param("roomId") UUID roomId
    );
}

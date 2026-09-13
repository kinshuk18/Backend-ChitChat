package com.example.chitchat.repository;

import com.example.chitchat.entity.MessageReceipt;
import com.example.chitchat.entity.MessageReceiptId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MessageReceiptRepository extends JpaRepository<MessageReceipt, MessageReceiptId> {
    @Query("""
    SELECT r
    FROM MessageReceipt r
    WHERE r.messageId.messageId = :messageId
    """)
    List<MessageReceipt> findAllByMessageId(
            @Param("messageId") UUID messageId
    );
}

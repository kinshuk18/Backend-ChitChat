package com.example.chitchat.dto;

import com.example.chitchat.entity.MessageReceiptId;
import jakarta.persistence.EmbeddedId;

import java.time.LocalDateTime;
import java.util.UUID;

public class MailDeliveryResponse {
    private LocalDateTime messageDelivered;
    private LocalDateTime messageRead;

    private UUID messageId;
    private String username;
    private String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDateTime getMessageDelivered() {
        return messageDelivered;
    }

    public void setMessageDelivered(LocalDateTime messageDelivered) {
        this.messageDelivered = messageDelivered;
    }

    public LocalDateTime getMessageRead() {
        return messageRead;
    }

    public void setMessageRead(LocalDateTime messageRead) {
        this.messageRead = messageRead;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public void setMessageId(UUID messageId) {
        this.messageId = messageId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}

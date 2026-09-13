package com.example.chitchat.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class ChatMessageResponse {
    private String content;
    private UUID messageId;
    private LocalDateTime createdTimestamp;
    private String sender;
    private UUID roomId;
    private String type;

    public UUID getMessageId() {
        return messageId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public UUID getRoomId() {
        return roomId;
    }

    public void setRoomId(UUID roomId) {
        this.roomId = roomId;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public void setMessageId(UUID messageId) {
        this.messageId = messageId;
    }

    public LocalDateTime getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(LocalDateTime createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}

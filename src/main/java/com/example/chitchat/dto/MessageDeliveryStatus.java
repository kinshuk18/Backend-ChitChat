package com.example.chitchat.dto;

import java.util.UUID;

public class MessageDeliveryStatus {
    private UUID messageId;
    private Boolean isMessageDelivered;
    private Boolean isMessageRead;
    private UUID roomId;

    public UUID getMessageId() {
        return messageId;
    }

    public UUID getRoomId() {
        return roomId;
    }

    public void setRoomId(UUID roomId) {
        this.roomId = roomId;
    }

    public void setMessageId(UUID messageId) {
        this.messageId = messageId;
    }

    public Boolean getMessageDelivered() {
        return isMessageDelivered;
    }

    public void setMessageDelivered(Boolean messageDelivered) {
        isMessageDelivered = messageDelivered;
    }

    public Boolean getMessageRead() {
        return isMessageRead;
    }

    public void setMessageRead(Boolean messageRead) {
        isMessageRead = messageRead;
    }
}

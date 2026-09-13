package com.example.chitchat.entity;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class MessageReceiptId implements Serializable {
    private UUID messageId;
    private String username;
    public MessageReceiptId(){};
    public MessageReceiptId(UUID messageId , String username ){
        this.messageId = messageId;
        this.username = username;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageReceiptId that = (MessageReceiptId) o;
        return Objects.equals(messageId, that.messageId) && Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId, username);
    }
}

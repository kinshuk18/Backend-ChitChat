package com.example.chitchat.entity;

import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;


import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name="message_receipt")
public class MessageReceipt {
    private LocalDateTime messageDelivered;
    private LocalDateTime messageRead;

    @EmbeddedId
    private MessageReceiptId messageId;

    public MessageReceipt(){};
    public MessageReceipt(MessageReceiptId messageId , LocalDateTime messageDelivered , LocalDateTime messageRead){
        this.messageDelivered = messageDelivered;
        this.messageRead = messageRead ;
        this.messageId = messageId;
    }

    public LocalDateTime getMessageDelivered() {
        return messageDelivered;
    }

    public void setMessageDelivered(LocalDateTime messageDelivered) {
        this.messageDelivered = messageDelivered;
    }

    public MessageReceiptId getMessageId() {
        return messageId;
    }

    public void setMessageId(MessageReceiptId messageId) {
        this.messageId = messageId;
    }

    public LocalDateTime getMessageRead() {
        return messageRead;
    }

    public void setMessageRead(LocalDateTime messageRead) {
        this.messageRead = messageRead;
    }
}

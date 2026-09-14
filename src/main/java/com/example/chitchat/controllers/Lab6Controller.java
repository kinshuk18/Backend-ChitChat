package com.example.chitchat.controllers;

import com.example.chitchat.entity.MessageEntity;
import com.example.chitchat.repository.MessageRepository;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
public class Lab6Controller {

    private static final UUID LAB_ROOM_ID = UUID.nameUUIDFromBytes(
            "chitchat-lab6-room".getBytes(StandardCharsets.UTF_8));

    private final MessageRepository messageRepository;

    public Lab6Controller(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @PostMapping("/message")
    public ResponseEntity<MessageEntity> postMessage(
            @RequestHeader(value = "X-Message-ID", required = false) String messageIdHeader,
            @RequestBody MessagePostRequest request) {
        String messageId = normalizeMessageId(messageIdHeader);
        MessageEntity message = new MessageEntity(
                messageId,
                LAB_ROOM_ID,
                request.clientName(),
                request.msg(),
                "lab6-nonce",
                "lab6-signature",
                LocalDateTime.now(),
                null,
                false);
        message.setContent(request.msg());

        try {
            return ResponseEntity.ok(messageRepository.saveAndFlush(message));
        } catch (DataIntegrityViolationException conflict) {
            return messageRepository.findByMessageId(messageId)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.ok(message));
        }
    }

    @GetMapping("/feed")
    public List<MessageEntity> feed() {
        List<MessageEntity> messages = messageRepository.findAllByOrderByCreatedTimestampAsc();
        for (MessageEntity message : messages) {
            if (message.getContent() == null) {
                message.setContent(message.getCiphertext());
            }
        }
        return messages;
    }

    private String normalizeMessageId(String messageIdHeader) {
        if (messageIdHeader == null || messageIdHeader.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return messageIdHeader.trim();
    }

    public record MessagePostRequest(
            @JsonProperty("client-name") String clientName,
            @JsonProperty("msg") String msg) {
    }
}
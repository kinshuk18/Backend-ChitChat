package com.example.chitchat.controllers;

import com.example.chitchat.entity.MessageEntity;
import com.example.chitchat.repository.MessageRepository;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
    @Transactional
    public ResponseEntity<Map<String, Object>> postMessage(
            @RequestHeader(value = "X-Message-ID", required = false) String messageIdHeader,
            @RequestBody(required = false) MessagePostRequest request) {
        if (request == null || request.msg() == null || request.msg().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Message payload is required."));
        }

        String messageId = normalizeMessageId(messageIdHeader);
        String clientName = (request.clientName() == null || request.clientName().isBlank()) ? "anonymous"
                : request.clientName().trim();
        String content = request.msg().trim();

        if (messageRepository.findByMessageId(messageId).isPresent()) {
            return ResponseEntity.ok(Map.of(
                    "messageId", messageId,
                    "status", "duplicate",
                    "saved", false,
                    "content", content,
                    "username", clientName));
        }

        MessageEntity message = new MessageEntity(
                messageId,
                LAB_ROOM_ID,
                clientName,
                content,
                "lab6-nonce",
                "lab6-signature",
                LocalDateTime.now(),
                null,
                false);
        message.setContent(content);

        try {
            MessageEntity saved = messageRepository.saveAndFlush(message);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "messageId", saved.getMessageId(),
                    "status", "created",
                    "saved", true,
                    "content", saved.getContent(),
                    "username", saved.getUsername(),
                    "timestamp", saved.getCreatedTimestamp()));
        } catch (DataIntegrityViolationException conflict) {
            return messageRepository.findByMessageId(messageId)
                    .map(existing -> ResponseEntity.ok(Map.of(
                            "messageId", existing.getMessageId(),
                            "status", "duplicate",
                            "saved", false,
                            "content", existing.getContent(),
                            "username", existing.getUsername())))
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(Map.of("error", "Message could not be stored.")));
        }
    }

    @GetMapping("/feed")
    public ResponseEntity<List<Map<String, Object>>> feed() {
        List<Map<String, Object>> payload = messageRepository.findAllByOrderByCreatedTimestampAsc()
                .stream()
                .map(message -> Map.of(
                        "messageId", message.getMessageId(),
                        "roomId", message.getRoomId().toString(),
                        "username", message.getUsername(),
                        "content", message.getContent() != null ? message.getContent() : message.getCiphertext(),
                        "timestamp", message.getCreatedTimestamp()))
                .toList();
        return ResponseEntity.ok(payload);
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
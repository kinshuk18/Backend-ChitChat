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
import java.util.HashMap;
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
                        Map<String, Object> err = new HashMap<>();
                        err.put("error", "Message payload is required.");
                        return ResponseEntity.badRequest().body(err);
                }

                String messageId = normalizeMessageId(messageIdHeader);
                String clientName = (request.clientName() == null || request.clientName().isBlank()) ? "anonymous"
                                : request.clientName().trim();
                String content = request.msg().trim();

                if (messageRepository.findByMessageId(messageId).isPresent()) {
                        Map<String, Object> duplicate = new HashMap<>();
                        duplicate.put("messageId", messageId);
                        duplicate.put("status", "duplicate");
                        duplicate.put("saved", false);
                        duplicate.put("content", content);
                        duplicate.put("username", clientName);
                        return ResponseEntity.ok(duplicate);
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
                        Map<String, Object> created = new HashMap<>();
                        created.put("messageId", saved.getMessageId());
                        created.put("status", "created");
                        created.put("saved", true);
                        created.put("content", saved.getContent());
                        created.put("username", saved.getUsername());
                        created.put("timestamp", saved.getCreatedTimestamp());
                        return ResponseEntity.status(HttpStatus.CREATED).body(created);
                } catch (DataIntegrityViolationException conflict) {
                        return messageRepository.findByMessageId(messageId)
                                        .map(existing -> {
                                                Map<String, Object> duplicate = new HashMap<>();
                                                duplicate.put("messageId", existing.getMessageId());
                                                duplicate.put("status", "duplicate");
                                                duplicate.put("saved", false);
                                                duplicate.put("content", existing.getContent());
                                                duplicate.put("username", existing.getUsername());
                                                return ResponseEntity.ok(duplicate);
                                        })
                                        .orElseGet(() -> {
                                                Map<String, Object> err = new HashMap<>();
                                                err.put("error", "Message could not be stored.");
                                                return ResponseEntity.status(HttpStatus.CONFLICT).body(err);
                                        });
                }
        }

        @GetMapping("/feed")
        public ResponseEntity<List<Map<String, Object>>> feed() {
                List<Map<String, Object>> payload = messageRepository.findAllByOrderByCreatedTimestampAsc()
                                .stream()
                                .map(message -> {
                                        Map<String, Object> m = new HashMap<>();
                                        m.put("messageId", message.getMessageId());
                                        m.put("roomId", message.getRoomId().toString());
                                        m.put("username", message.getUsername());
                                        m.put("content", message.getContent() != null ? message.getContent()
                                                        : message.getCiphertext());
                                        m.put("timestamp", message.getCreatedTimestamp());
                                        return m;
                                })
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
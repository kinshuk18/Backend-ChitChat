package com.example.chitchat.controllers;

import com.example.chitchat.entity.MessageEntity;
import com.example.chitchat.entity.MessageReceipt;
import com.example.chitchat.service.MessageService;
import com.example.chitchat.service.RoomService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
public class MessageController {
    private final MessageService messageService;
    private final RoomService roomService;

    public MessageController( MessageService messageService, RoomService roomService ){
        this.messageService = messageService;
        this.roomService = roomService;
    }

    @GetMapping("/rooms/{roomId}/messages/recent")
    public List<MessageEntity> getRecentMessages(
            @PathVariable UUID roomId, Authentication authentication) {
        String username = authentication.getName();
        if (!roomService.isUserMember(username, roomId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member of this room");
        }
        return messageService.getRecentMessages(roomId);
    }

    @GetMapping("/rooms/{messageId}")
    public List<MessageReceipt> getMessageReceipt(@PathVariable UUID messageId, Authentication authentication){
        String username = authentication.getName();
        return messageService.getMessageReceipt(messageId , username );
    }

}

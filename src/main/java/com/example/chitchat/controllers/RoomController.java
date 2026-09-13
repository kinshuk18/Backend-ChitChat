package com.example.chitchat.controllers;

import com.example.chitchat.dto.CreateRoomRequest;
import com.example.chitchat.dto.GetRoomResponse;
import com.example.chitchat.service.RoomService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.UUID;

@RestController
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService){
        this.roomService = roomService;
    }

    @PostMapping("/room/create")
    public String createRoom(@RequestBody CreateRoomRequest req, Authentication authentication){
        String username = authentication.getName();
        return roomService.createRoom(req, username);
    }
    @GetMapping("/room/all")
    public GetRoomResponse getAllRooms(Authentication authentication){
        String username = authentication.getName();
        return roomService.getAllRooms(username);
    }
    @DeleteMapping("/room/leave/{roomId}")
    public ResponseEntity<Boolean> removeUserFromRoom(@PathVariable UUID roomId, Authentication authentication){
        String username = authentication.getName();
        Boolean deleted = roomService.leaveRoom(username,roomId);
        if( !deleted ) return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        return ResponseEntity.ok(deleted);
    }
    @PostMapping("/room/join/{roomId}")
    public ResponseEntity<Boolean> joinRoom(@PathVariable UUID roomId, Authentication authentication){
        String username = authentication.getName();
        Boolean joinedSuccessully = roomService.joinRoom(username,roomId);
        return ResponseEntity.ok(joinedSuccessully);
    }
}

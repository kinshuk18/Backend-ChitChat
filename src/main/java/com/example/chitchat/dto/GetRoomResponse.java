package com.example.chitchat.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GetRoomResponse {
    private UUID roomId;
    private Map<RoomIdRoomName, List<RoomUserInfo>> rooms;
    public record RoomUserInfo( String username , String role ){};
    public record RoomIdRoomName(UUID roomId,String roomname){};


    public UUID getRoomId() {
        return roomId;
    }

    public void setRoomId(UUID roomId) {
        this.roomId = roomId;
    }

    public Map<RoomIdRoomName, List<RoomUserInfo>> getRooms() {
        return rooms;
    }

    public void setRooms(Map<RoomIdRoomName, List<RoomUserInfo>> rooms) {
        this.rooms = rooms;
    }
}

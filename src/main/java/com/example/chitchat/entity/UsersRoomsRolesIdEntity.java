package com.example.chitchat.entity;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class UsersRoomsRolesIdEntity implements Serializable {
    private String username;
    private UUID roomId;

    public UsersRoomsRolesIdEntity(){}

    public UsersRoomsRolesIdEntity( String username , UUID roomId ){
        this.username = username;
        this.roomId = roomId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public UUID getRoomId() {
        return roomId;
    }

    public void setRoomId(UUID roomId) {
        this.roomId = roomId;
    }

    @Override
    public boolean equals(Object o){
        if(this==o) return true;
        if( !(o instanceof UsersRoomsRolesIdEntity that) ) return false;
        return Objects.equals(username, that.username) && Objects.equals(roomId, that.roomId);
    }
    @Override
    public int hashCode() {
        return Objects.hash(username, roomId);
    }

}

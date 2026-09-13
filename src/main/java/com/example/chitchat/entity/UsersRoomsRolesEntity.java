package com.example.chitchat.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name="rooms_users_roles")
public class UsersRoomsRolesEntity {

    //USERS_ROOMS_ROLES( username, room_id, role_type )
    @EmbeddedId
    private UsersRoomsRolesIdEntity id;
    private String roleType;

    public UsersRoomsRolesEntity(){};

    public UsersRoomsRolesEntity( UsersRoomsRolesIdEntity id , String roleType ){
        this.id = id;
        this.roleType = roleType;
    }

    public UsersRoomsRolesIdEntity getId() {
        return id;
    }

    public void setId(UsersRoomsRolesIdEntity id) {
        this.id = id;
    }

    public String getRoleType() {
        return roleType;
    }

    public void setRoleType(String roleType) {
        this.roleType = roleType;
    }
}

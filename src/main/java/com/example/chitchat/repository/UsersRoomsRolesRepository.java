package com.example.chitchat.repository;

import com.example.chitchat.entity.UsersRoomsRolesEntity;
import com.example.chitchat.entity.UsersRoomsRolesIdEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsersRoomsRolesRepository extends JpaRepository<UsersRoomsRolesEntity, UsersRoomsRolesIdEntity> {
    Optional<UsersRoomsRolesEntity> findByIdUsernameAndIdRoomId(String username, UUID roomId);

    @Query("""
    SELECT e.id.roomId
    FROM UsersRoomsRolesEntity e
    WHERE e.id.username = :username
    """)
    List<UUID> findRoomIdsByUsername(@Param("username") String username);

    @Query("""
    SELECT e
    FROM UsersRoomsRolesEntity e
    WHERE e.id.roomId = :roomId
    """)
    List<UsersRoomsRolesEntity> findMembersByRoomId(
            @Param("roomId") UUID roomId
    );

    @Query("""
    SELECT e.id.username
    FROM UsersRoomsRolesEntity e
    WHERE e.id.roomId = :roomId
    """)
    List<String> findUsernameByRoomId(@Param("roomId") UUID roomId);
}

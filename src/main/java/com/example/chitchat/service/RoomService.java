package com.example.chitchat.service;

import com.example.chitchat.dto.ChatMessageResponse;
import com.example.chitchat.dto.CreateRoomRequest;
import com.example.chitchat.dto.GetRoomResponse;
import com.example.chitchat.entity.RoomEntity;
import com.example.chitchat.entity.UsersRoomsRolesEntity;
import com.example.chitchat.entity.UsersRoomsRolesIdEntity;
import com.example.chitchat.repository.RoomRepository;
import com.example.chitchat.repository.UsersRoomsRolesRepository;
import com.example.chitchat.websocket.WsEventPublisher;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;


@Service
public class RoomService {
    private final RoomRepository roomRepository;
    private final UsersRoomsRolesRepository usersRoomsRolesRepository;
    private final UserService userService;
    private final WsEventPublisher websocketEventPublisher;

    public RoomService(RoomRepository roomRepository, UsersRoomsRolesRepository usersRoomsRolesRepository, UserService userService, WsEventPublisher websocketEventPublisher){
        this.roomRepository = roomRepository;
        this.usersRoomsRolesRepository = usersRoomsRolesRepository;
        this.userService = userService;
        this.websocketEventPublisher = websocketEventPublisher;
    }

    @Transactional
    public String createRoom(CreateRoomRequest req, String username){
        Set<String> users = req.getParticipants() != null ? req.getParticipants() : Collections.emptySet();

        for( String user : users ){
            if( !userService.userExists(user) ){
                // group cannot be created with non-existing user
                return null;
            }
        }

        RoomEntity room = new RoomEntity();

        room.setRoomname(req.getRoomname());
        room.setCreatedTimestamp(LocalDateTime.now());
        room.setMaximumCapacity(req.getMaximumCapacity());

        roomRepository.save(room);

        UUID roomId = room.getRoomId();

        // put participants in usersRoomsRoles table
        List<UsersRoomsRolesEntity> usersRoomsRoles = new ArrayList<>();
        for( String user : users ){
            if (!user.equals(username)) {
                // creating composite id of roomId and username
                UsersRoomsRolesIdEntity id = new UsersRoomsRolesIdEntity(user, roomId);
                UsersRoomsRolesEntity e = new UsersRoomsRolesEntity(id,"MEMBER");

                usersRoomsRoles.add(e);
            }
        }
        // create current user as admin
        UsersRoomsRolesIdEntity adminUserId = new UsersRoomsRolesIdEntity(username, roomId);

        UsersRoomsRolesEntity adminUser = new UsersRoomsRolesEntity(adminUserId,"ADMIN");
        usersRoomsRoles.add(adminUser);

        usersRoomsRolesRepository.saveAll(usersRoomsRoles);
        return "Room created and users, roles, rooms mapped";
    }


    public boolean isUserMember(String username, UUID roomId){
        return usersRoomsRolesRepository.findByIdUsernameAndIdRoomId(username,roomId).isPresent();
    }


    public GetRoomResponse getAllRooms(String username) {

        List<UUID> roomIds =
                usersRoomsRolesRepository
                        .findRoomIdsByUsername(username);

        Map<GetRoomResponse.RoomIdRoomName,
                        List<GetRoomResponse.RoomUserInfo>> rooms =
                new HashMap<>();

        List<RoomEntity> roomEntities =
                roomRepository.findByRoomIdIn(roomIds);

        for (RoomEntity room : roomEntities) {

            UUID roomId = room.getRoomId();

            List<UsersRoomsRolesEntity> members =
                    usersRoomsRolesRepository
                            .findMembersByRoomId(roomId);

            List<GetRoomResponse.RoomUserInfo> users =
                    members.stream()
                            .map(member ->
                                    new GetRoomResponse.RoomUserInfo(
                                            member.getId().getUsername(),
                                            member.getRoleType()
                                    )
                            )
                            .toList();

            GetRoomResponse.RoomIdRoomName roomInfo =
                    new GetRoomResponse.RoomIdRoomName(
                            roomId,
                            room.getRoomname()
                    );

            rooms.put(roomInfo, users);
        }

        GetRoomResponse response = new GetRoomResponse();
        response.setRooms(rooms);

        return response;
    }

    public boolean leaveRoom(String username,UUID roomId){
        if( !isUserMember(username, roomId) ) return true;
        UsersRoomsRolesIdEntity id = new UsersRoomsRolesIdEntity(username,roomId);
        usersRoomsRolesRepository.deleteById(id);

        ChatMessageResponse event = new ChatMessageResponse();
        event.setSender("SYSTEM_DAEMON");
        event.setCreatedTimestamp(LocalDateTime.now());
        event.setContent(username+" left channel");
        event.setMessageId(UUID.randomUUID());
        event.setType("CHAT_MESSAGE");
        event.setRoomId(roomId);
        websocketEventPublisher.publishRaw(roomId,event);

        return true;
    }

    public Boolean joinRoom( String username , UUID roomId ){
        if( isUserMember(username,roomId) ) return true ;
        UsersRoomsRolesEntity newUser = new UsersRoomsRolesEntity( new UsersRoomsRolesIdEntity(username,roomId), "MEMBER" );
        usersRoomsRolesRepository.save(newUser);

        ChatMessageResponse event = new ChatMessageResponse();
        event.setSender("SYSTEM_DAEMON");
        event.setCreatedTimestamp(LocalDateTime.now());
        event.setContent(username+" joined channel");
        event.setMessageId(UUID.randomUUID());
        event.setType("CHAT_MESSAGE");
        event.setRoomId(roomId);

        websocketEventPublisher.publishRaw(roomId,event);
        return true ;
    }

}

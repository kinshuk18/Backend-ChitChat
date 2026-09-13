package com.example.chitchat.websocket;

import com.example.chitchat.dto.ChatMessageResponse;
import com.example.chitchat.dto.MailDeliveryResponse;
import com.example.chitchat.entity.MessageEntity;
import com.example.chitchat.entity.MessageReceipt;
import com.example.chitchat.entity.MessageReceiptId;
import com.example.chitchat.entity.UsersRoomsRolesEntity;
import com.example.chitchat.repository.MessageReceiptRepository;
import com.example.chitchat.repository.MessageRepository;
import com.example.chitchat.repository.UsersRoomsRolesRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

/**
 * Tracks which sockets of THIS instance are in which room and delivers
 * pre-fanned-out events to them. All cross-instance delivery is handled by
 * {@link WsEventPublisher} + {@link WsEventListener}; nothing here leaves the
 * JVM.
 */
@Component
public class WebsocketSessionManager {

    private final ObjectMapper objectMapper;
    private final MessageRepository messageRepository;
    private final MessageReceiptRepository messageReceiptRepository;
    private final UsersRoomsRolesRepository usersRoomsRolesRepository;

    private final ConcurrentHashMap<UUID, Set<WebSocketSession> > roomSessions = new ConcurrentHashMap<>();
    public WebsocketSessionManager(ObjectMapper objectMapper,
                                   MessageRepository messageRepository,
                                   MessageReceiptRepository messageReceiptRepository,
                                   UsersRoomsRolesRepository usersRoomsRolesRepository){
        this.objectMapper = objectMapper;
        this.messageRepository = messageRepository;
        this.messageReceiptRepository = messageReceiptRepository;
        this.usersRoomsRolesRepository = usersRoomsRolesRepository;
    }

    public void joinRoom( UUID roomId, WebSocketSession session){
        roomSessions.computeIfAbsent(roomId,
                id-> ConcurrentHashMap.newKeySet() ).add(session);
    }


    public void leaveRoom( UUID roomId, WebSocketSession session){
        Set<WebSocketSession> sessions = roomSessions.get(roomId);
        if(sessions==null) return;

        sessions.remove(session);

        if(sessions.isEmpty()) roomSessions.remove(roomId);
    }

    public void removeSession( WebSocketSession session ){
        for( var entry : roomSessions.entrySet() ){
            entry.getValue().remove(session);
            if( entry.getValue().isEmpty() ){
                roomSessions.remove(entry.getKey());
            }
        }
    }

    /** Deliver a hydrated chat message to this instance's sockets in the room. */
    public void broadcastLocally(UUID roomId, ChatMessageResponse message){
        String json = objectMapper.writeValueAsString(message);
        deliver(roomId, json);
    }

    /**
     * Re-read receipt state from the (shared) DB and deliver a status update to
     * this instance's sockets in the room. Called by WsEventListener.
     */
    public void broadcastStatusLocally(UUID messageId, UUID roomId , String username ){

        MessageEntity messageEntity = messageRepository.findById(messageId).orElse(null);
        UsersRoomsRolesEntity foundUsersRoomsRolesEntity = usersRoomsRolesRepository.findByIdUsernameAndIdRoomId(username,roomId).orElse(null);

        if( messageEntity==null || foundUsersRoomsRolesEntity==null || !messageEntity.getRoomId().equals(roomId) ){
            return;
        }

        MessageReceipt messageReceipt = messageReceiptRepository.findById(new MessageReceiptId(messageId,username)).orElse(null);
        if( messageReceipt==null ) return;
        MailDeliveryResponse mailDeliveryResponse = new MailDeliveryResponse();
        mailDeliveryResponse.setMessageDelivered(messageReceipt.getMessageDelivered());
        mailDeliveryResponse.setMessageRead(messageReceipt.getMessageRead());
        mailDeliveryResponse.setUsername(username);
        mailDeliveryResponse.setMessageId(messageId);
        mailDeliveryResponse.setType("MESSAGE_STATUS_UPDATE");

        String json = objectMapper.writeValueAsString(mailDeliveryResponse);
        deliver(roomId, json);
    }

    /** Deliver a pre-serialized ephemeral system event to this instance's sockets. */
    public void broadcastRawLocally(UUID roomId, String json){
        deliver(roomId, json);
    }

    /** Single send-loop shared by every delivery path; prunes dead sockets. */
    private void deliver(UUID roomId, String json){
        Set<WebSocketSession> sessions = roomSessions.get(roomId);
        if( sessions==null ) return;

        for( WebSocketSession s : sessions ){
            if( !s.isOpen() ){
                sessions.remove(s);
                continue;
            }
            try{
                s.sendMessage( new TextMessage(json) );
            }
            catch( IOException e ){
                System.out.println("Failed to broadcast to session: " + s.getId());
                sessions.remove(s);
                try{
                    s.close();
                }
                catch(IOException ee){
                    System.out.println("Could not close session:" + s.getId());
                }
            }
        }
    }

}

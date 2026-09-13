package com.example.chitchat.service;
import com.example.chitchat.entity.UserEntity;
import com.example.chitchat.repository.UserRepository;
import java.security.GeneralSecurityException;
import com.example.chitchat.dto.ChatMessageRequest;
import com.example.chitchat.dto.ChatMessageResponse;
import com.example.chitchat.dto.MessageDeliveryStatus;
import com.example.chitchat.entity.MessageEntity;
import com.example.chitchat.entity.MessageReceipt;
import com.example.chitchat.entity.MessageReceiptId;
import com.example.chitchat.entity.UsersRoomsRolesEntity;
import com.example.chitchat.repository.MessageReceiptRepository;
import com.example.chitchat.repository.MessageRepository;
import com.example.chitchat.repository.UsersRoomsRolesRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final UsersRoomsRolesRepository usersRoomsRolesRepository;
    private final RoomService roomService;
    private final MessageReceiptRepository messageReceiptRepository;
    // for msg encryption
    private final MessageCryptoService messageCryptoService;   
    private final UserRepository userRepository;              

    public MessageService(MessageRepository messageRepository,
                          UsersRoomsRolesRepository usersRoomsRolesRepository,
                          RoomService roomService,
                          MessageReceiptRepository messageReceiptRepository,MessageCryptoService messageCryptoService,UserRepository userRepository){
        this.messageRepository = messageRepository;
        this.usersRoomsRolesRepository = usersRoomsRolesRepository;
        this.roomService = roomService;
        this.messageReceiptRepository = messageReceiptRepository;
        // for msg encryption
        this.messageCryptoService = messageCryptoService;
        this.userRepository = userRepository;
    }

    // now save the message with ciphertext
    public MessageEntity saveMessage(ChatMessageRequest req, String username){
    UUID roomId = req.getRoomId();
    String content = req.getContent();
 
    if(!roomService.isUserMember(username,roomId)){
        throw new RuntimeException("User not a member of room");
    }
 
    try {
        // 1) encrypt — DB stores ciphertext, never plaintext
        MessageCryptoService.EncryptedData enc = messageCryptoService.encrypt(content);
 
        // 2) unwrap sender's private key (KEK-encrypted at rest) and sign
        UserEntity sender = userRepository.findById(username).orElseThrow(
                () -> new RuntimeException("Sender not found: " + username));
        String privateKey = messageCryptoService.unwrapPrivateKey(sender.getWrappedPrivateKey());
        String signature = messageCryptoService.sign(
                privateKey, roomId.toString(), username,
                enc.ciphertext(), enc.nonce());
 
        // 3) build with the NEW 8-arg constructor
        MessageEntity messageEntity = new MessageEntity(
                roomId, username,
                enc.ciphertext(), enc.nonce(), signature,
                LocalDateTime.now(), null, false);
 
        // 4) transient field — needed by ChatWebSocketHandler for the live broadcast
        messageEntity.setContent(content);
 
        MessageEntity saveMessageEntity = messageRepository.save(messageEntity);
        initializeMessageReceipt(roomId, saveMessageEntity.getMessageId());
        return saveMessageEntity;
    } catch (GeneralSecurityException e) {
        throw new RuntimeException("Could not encrypt/sign message", e);
    }
}


    /**
     * Hydrate one persisted message for live WebSocket fan-out: verify the
     * sender signature and decrypt, mirroring getRecentMessages() semantics.
     * Called by WsEventListener on every instance that received the event.
     */
    public ChatMessageResponse getLiveMessage(UUID messageId) {
        MessageEntity m = messageRepository.findById(messageId).orElse(null);
        if (m == null) return null;

        UserEntity sender = userRepository.findById(m.getUsername()).orElse(null);
        if (sender == null || sender.getPublicKey() == null) {
            m.setContent("[SIGNATURE VERIFICATION FAILED - unknown sender]");
        } else {
            boolean ok = messageCryptoService.verify(
                    sender.getPublicKey(), m.getRoomId().toString(), m.getUsername(),
                    m.getCiphertext(), m.getNonce(), m.getSignature());
            if (!ok) {
                m.setContent("[SIGNATURE VERIFICATION FAILED - forged or modified]");
            } else {
                try {
                    m.setContent(messageCryptoService.decrypt(m.getCiphertext(), m.getNonce()));
                } catch (GeneralSecurityException e) {
                    m.setContent("[TAMPERED - AES-GCM authentication failed]");
                }
            }
        }

        ChatMessageResponse response = new ChatMessageResponse();
        response.setMessageId(m.getMessageId());
        response.setContent(m.getContent());
        response.setCreatedTimestamp(m.getCreatedTimestamp());
        response.setSender(m.getUsername());
        response.setRoomId(m.getRoomId());
        response.setType("CHAT_MESSAGE");
        return response;
    }

    public void initializeMessageReceipt(UUID roomId , UUID messageId ){
        List<String> userNames = usersRoomsRolesRepository.findUsernameByRoomId(roomId);

        for( String u : userNames ){
            messageReceiptRepository.save(new MessageReceipt( new MessageReceiptId(messageId, u), null, null) );
        }
    }
    // now verify + decrypt before returning
    public List<MessageEntity> getRecentMessages(UUID roomId) {
    List<MessageEntity> messages = messageRepository.findRecentMessages(roomId);
 
    for (MessageEntity m : messages) {
        UserEntity sender = userRepository.findById(m.getUsername()).orElse(null);
        if (sender == null || sender.getPublicKey() == null) {
            m.setContent("[SIGNATURE VERIFICATION FAILED - unknown sender]");
            continue;
        }
        // verify signature FIRST (signature is over ciphertext, no decrypt needed yet)
        boolean ok = messageCryptoService.verify(
                sender.getPublicKey(), m.getRoomId().toString(), m.getUsername(),
                m.getCiphertext(), m.getNonce(), m.getSignature());
        if (!ok) {
            m.setContent("[SIGNATURE VERIFICATION FAILED - forged or modified]");
            continue;
        }
        // then decrypt — throws AEADBadTagException if ciphertext/nonce was tampered
        try {
            m.setContent(messageCryptoService.decrypt(m.getCiphertext(), m.getNonce()));
        } catch (GeneralSecurityException e) {
            m.setContent("[TAMPERED - AES-GCM authentication failed]");
        }
    }
    return messages;
}


    public void updateMessageReceiptDeliveredStatus(MessageDeliveryStatus status, String username){
        UUID messageId = status.getMessageId();

        MessageEntity messageEntity = messageRepository.findById(messageId).orElse(null);
        UsersRoomsRolesEntity foundUsersRoomsRolesEntity = usersRoomsRolesRepository.findByIdUsernameAndIdRoomId(username,status.getRoomId()).orElse(null);

        if( messageEntity==null || foundUsersRoomsRolesEntity==null || !messageEntity.getRoomId().equals(status.getRoomId()) ){
            return;
        }

        MessageReceiptId id = new MessageReceiptId(messageId,username);
        MessageReceipt messageReceipt = messageReceiptRepository.findById(id).orElse(null);
        if(messageReceipt==null) return;
        messageReceipt.setMessageDelivered(LocalDateTime.now());
        messageReceiptRepository.save(messageReceipt);
    }

    public void updateMessageReceiptReadStatus(MessageDeliveryStatus status, String username){
        UUID messageId = status.getMessageId();

        MessageEntity messageEntity = messageRepository.findById(messageId).orElse(null);
        UsersRoomsRolesEntity foundUsersRoomsRolesEntity = usersRoomsRolesRepository.findByIdUsernameAndIdRoomId(username,status.getRoomId()).orElse(null);

        if( messageEntity==null || foundUsersRoomsRolesEntity==null || !messageEntity.getRoomId().equals(status.getRoomId()) ){
            return;
        }

        MessageReceiptId id = new MessageReceiptId(messageId,username);
        MessageReceipt messageReceipt = messageReceiptRepository.findById(id).orElse(null);
        if(messageReceipt==null) return;
        messageReceipt.setMessageRead(LocalDateTime.now());
        messageReceiptRepository.save(messageReceipt);
    }


    public List<MessageReceipt> getMessageReceipt(UUID messageId , String username){
        MessageEntity messageEntity = messageRepository.findById(messageId).orElse(null);
        if( messageEntity==null ) return null;
        UUID roomId = messageEntity.getRoomId();
        UsersRoomsRolesEntity u = usersRoomsRolesRepository.findByIdUsernameAndIdRoomId(username,roomId).orElse(null);
        if(u==null) return null;
        List<MessageReceipt> messageReceipts = messageReceiptRepository.findAllByMessageId(messageId);
        return messageReceipts;
    }
}

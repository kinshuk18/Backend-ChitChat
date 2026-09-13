package com.example.chitchat.service;
 
import com.example.chitchat.dto.CreateUserRequest;
import com.example.chitchat.entity.UserEntity;
import com.example.chitchat.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
 
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.time.LocalDateTime;
 
@Service
public class UserService {
    private final UserRepository userRepository;
    private final MessageCryptoService messageCryptoService;
    private final PasswordEncoder passwordEncoder;
 
    public UserService(UserRepository userRepository, MessageCryptoService messageCryptoService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.messageCryptoService = messageCryptoService;
        this.passwordEncoder = passwordEncoder;
    }
 
    public UserEntity createUser(CreateUserRequest req) {
        if (req.getUsername() == null || req.getUsername().isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Username cannot be empty");
        }
        if (userRepository.existsById(req.getUsername())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT, "Username already taken");
        }

        UserEntity user = new UserEntity();
 
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setTagline(req.getTagline());
        user.setProfilePicture(req.getProfilePicture());
        user.setTimestamp(LocalDateTime.now());
 
        // each user gets a signing key pair at signup; private key is WRAPPED before storage
        try {
            KeyPair keyPair = messageCryptoService.generateSigningKeyPair();
            user.setPublicKey(messageCryptoService.encodePublicKey(keyPair.getPublic()));
            user.setWrappedPrivateKey(
                    messageCryptoService.wrapPrivateKey(
                            messageCryptoService.encodePrivateKey(keyPair.getPrivate())));
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Could not generate signing keys", e);
        }
 
        return userRepository.save(user);
    }
 
    public boolean userExists(String username) {
        return userRepository.existsById(username);
    }}
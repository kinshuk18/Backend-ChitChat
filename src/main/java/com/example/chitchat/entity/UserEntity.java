package com.example.chitchat.entity;
 
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
 
import java.time.LocalDateTime;
 
@Entity
@Table(name = "users")
public class UserEntity {
 
    @Column(unique = true, nullable = false)
    @Id
    private String username;
 
    @JsonIgnore
    @Column(nullable = false)
    private String password;
 
    private LocalDateTime timestamp;
    private String tagline;
    private String profilePicture;
 
    @Column(columnDefinition = "TEXT")
    private String publicKey;   // anyone may read this — used to VERIFY this user's messages
 
    @JsonIgnore                    // never expose in JSON responses
    @Column(name = "wrapped_private_key", columnDefinition = "TEXT")
    private String wrappedPrivateKey;   // AES-GCM(KEK, privateKey) — useless without the KEK
 
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
 
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
 
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
 
    public String getTagline() { return tagline; }
    public void setTagline(String tagline) { this.tagline = tagline; }
 
    public String getProfilePicture() { return profilePicture; }
    public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture; }
 
    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }
 
    public String getWrappedPrivateKey() { return wrappedPrivateKey; }
    public void setWrappedPrivateKey(String wrappedPrivateKey) { this.wrappedPrivateKey = wrappedPrivateKey; }
}

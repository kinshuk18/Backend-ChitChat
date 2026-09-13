package com.example.chitchat.service;
 
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
 
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
 @Service
public class MessageCryptoService {
 
    private static final int GCM_TAG_BITS = 128; // auth tag -> tamper detection
    private static final int NONCE_BYTES = 12;   // 96-bit nonce, standard for AES-GCM
 
    private final SecretKeySpec aesKey;             // encrypts message content
    private final SecretKeySpec keyEncryptionKey;   // encrypts private keys at rest (KEK)
 
    public MessageCryptoService(
            @Value("${app.encryption.secret-key}") String base64Key,
            @Value("${app.encryption.key-encryption-key}") String base64Kek) {
        this.aesKey = new SecretKeySpec(Base64.getDecoder().decode(base64Key), "AES");
        this.keyEncryptionKey = new SecretKeySpec(Base64.getDecoder().decode(base64Kek), "AES");
    }
 
    // confidentiality + integrity 
 
    public record EncryptedData(String ciphertext, String nonce) {}
 
    public EncryptedData encrypt(String plaintext) throws GeneralSecurityException {
        return encryptWith(aesKey, plaintext);
    }
 
    // Throws AEADBadTagException if ciphertext/nonce was modified -> tamper detected
    public String decrypt(String ciphertextB64, String nonceB64) throws GeneralSecurityException {
        return decryptWith(aesKey, ciphertextB64, nonceB64);
    }
 
    private EncryptedData encryptWith(SecretKey key, String plaintext) throws GeneralSecurityException {
        byte[] nonce = new byte[NONCE_BYTES];
        SecureRandom.getInstanceStrong().nextBytes(nonce);
 
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, nonce));
 
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
 
        return new EncryptedData(
                Base64.getEncoder().encodeToString(ciphertext),
                Base64.getEncoder().encodeToString(nonce));
    }
 
    private String decryptWith(SecretKey key, String ciphertextB64, String nonceB64) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key,
                new GCMParameterSpec(GCM_TAG_BITS, Base64.getDecoder().decode(nonceB64)));
 
        byte[] plaintext = cipher.doFinal(Base64.getDecoder().decode(ciphertextB64));
        return new String(plaintext, StandardCharsets.UTF_8);
    }
 
    // Private-key protection at rest (KEK)
 
    // Encrypt the sender's private key BEFORE it is stored in the DB.
    // A DB-only attacker gets ciphertext and cannot sign anything.
    public String wrapPrivateKey(String privateKeyB64) throws GeneralSecurityException {
        EncryptedData enc = encryptWith(keyEncryptionKey, privateKeyB64);
        return enc.ciphertext() + "." + enc.nonce();   // single string: ciphertext.nonce
    }
 
    public String unwrapPrivateKey(String wrapped) throws GeneralSecurityException {
        String[] parts = wrapped.split("\\.");
        return decryptWith(keyEncryptionKey, parts[0], parts[1]);
    }
 
    //  authenticity 
 
    public KeyPair generateSigningKeyPair() throws GeneralSecurityException {
        return KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
    }
 
    public String encodePublicKey(PublicKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }
 
    public String encodePrivateKey(PrivateKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }
 
    public PublicKey decodePublicKey(String base64) throws GeneralSecurityException {
        return KeyFactory.getInstance("Ed25519")
                .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(base64)));
    }
 
    public PrivateKey decodePrivateKey(String base64) throws GeneralSecurityException {
        return KeyFactory.getInstance("Ed25519")
                .generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64)));
    }
 
    // Signature binds message -> (room, sender, ciphertext, nonce).
    // We sign ciphertext (not plaintext) so verification can run BEFORE decryption.
    public String sign(String senderPrivateKeyB64, String roomId, String sender,
                       String ciphertextB64, String nonceB64) throws GeneralSecurityException {
        Signature sig = Signature.getInstance("Ed25519");
        sig.initSign(decodePrivateKey(senderPrivateKeyB64));
        sig.update(payloadBytes(roomId, sender, ciphertextB64, nonceB64));
        return Base64.getEncoder().encodeToString(sig.sign());
    }
 
    public boolean verify(String senderPublicKeyB64, String roomId, String sender,
                          String ciphertextB64, String nonceB64, String signatureB64) {
        try {
            Signature sig = Signature.getInstance("Ed25519");
            sig.initVerify(decodePublicKey(senderPublicKeyB64));
            sig.update(payloadBytes(roomId, sender, ciphertextB64, nonceB64));
            return sig.verify(Base64.getDecoder().decode(signatureB64));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            return false;
        }
    }
 
    private byte[] payloadBytes(String roomId, String sender,
                                String ciphertextB64, String nonceB64) {
        return (roomId + "|" + sender + "|" + ciphertextB64 + "|" + nonceB64)
                .getBytes(StandardCharsets.UTF_8);
    }
}

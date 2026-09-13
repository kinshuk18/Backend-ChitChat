package com.example.chitchat;

import com.example.chitchat.service.MessageCryptoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class MessageCryptoServiceTest {

    private MessageCryptoService cryptoService;

    @BeforeEach
    void setUp() {
        // Sample 256-bit AES keys
        String aesKey = Base64.getEncoder().encodeToString(new byte[32]);
        String kek = Base64.getEncoder().encodeToString(new byte[32]);
        cryptoService = new MessageCryptoService(aesKey, kek);
    }

    @Test
    void testEncryptDecryptSuccess() throws GeneralSecurityException {
        String plaintext = "Hello secure ChitChat world! 🚀";
        MessageCryptoService.EncryptedData enc = cryptoService.encrypt(plaintext);

        assertNotNull(enc.ciphertext());
        assertNotNull(enc.nonce());

        String decrypted = cryptoService.decrypt(enc.ciphertext(), enc.nonce());
        assertEquals(plaintext, decrypted);
    }

    @Test
    void testTamperedCiphertextFails() throws GeneralSecurityException {
        String plaintext = "Tamper test";
        MessageCryptoService.EncryptedData enc = cryptoService.encrypt(plaintext);

        byte[] rawCipher = Base64.getDecoder().decode(enc.ciphertext());
        rawCipher[0] ^= 0xFF; // Flip bit
        String tamperedCipher = Base64.getEncoder().encodeToString(rawCipher);

        assertThrows(GeneralSecurityException.class, () -> {
            cryptoService.decrypt(tamperedCipher, enc.nonce());
        });
    }

    @Test
    void testSigningAndVerification() throws GeneralSecurityException {
        KeyPair kp = cryptoService.generateSigningKeyPair();
        String pub = cryptoService.encodePublicKey(kp.getPublic());
        String priv = cryptoService.encodePrivateKey(kp.getPrivate());

        String roomId = "room-123";
        String sender = "alice";
        String cipher = "dummyCipher";
        String nonce = "dummyNonce";

        String signature = cryptoService.sign(priv, roomId, sender, cipher, nonce);
        assertNotNull(signature);

        boolean valid = cryptoService.verify(pub, roomId, sender, cipher, nonce, signature);
        assertTrue(valid);

        boolean invalidSender = cryptoService.verify(pub, roomId, "mallory", cipher, nonce, signature);
        assertFalse(invalidSender);

        boolean invalidRoom = cryptoService.verify(pub, "room-999", sender, cipher, nonce, signature);
        assertFalse(invalidRoom);
    }

    @Test
    void testPrivateKeyWrapUnwrap() throws GeneralSecurityException {
        KeyPair kp = cryptoService.generateSigningKeyPair();
        String priv = cryptoService.encodePrivateKey(kp.getPrivate());

        String wrapped = cryptoService.wrapPrivateKey(priv);
        assertNotEquals(priv, wrapped);
        assertTrue(wrapped.contains("."));

        String unwrapped = cryptoService.unwrapPrivateKey(wrapped);
        assertEquals(priv, unwrapped);
    }
}

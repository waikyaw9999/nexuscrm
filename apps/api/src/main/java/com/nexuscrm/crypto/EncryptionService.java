package com.nexuscrm.crypto;

import com.nexuscrm.config.NexusProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class EncryptionService {
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public EncryptionService(NexusProperties properties) {
        byte[] raw = decodeKey(properties.getEncryptionKey());
        this.key = new SecretKeySpec(raw, "AES");
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(1 + iv.length + cipherText.length);
            buffer.put((byte) 1);
            buffer.put(iv);
            buffer.put(cipherText);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt secret", e);
        }
    }

    public String decrypt(String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(payload);
            ByteBuffer buffer = ByteBuffer.wrap(decoded);
            buffer.get(); // version
            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);
            byte[] cipherText = new byte[buffer.remaining()];
            buffer.get(cipherText);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt secret", e);
        }
    }

    private static byte[] decodeKey(String configured) {
        String value = configured == null ? "" : configured.trim();
        if (value.length() == 64 && value.matches("[0-9a-fA-F]+")) {
            byte[] out = new byte[32];
            for (int i = 0; i < 32; i++) {
                out[i] = (byte) Integer.parseInt(value.substring(i * 2, i * 2 + 2), 16);
            }
            return out;
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[32];
        System.arraycopy(bytes, 0, out, 0, Math.min(bytes.length, 32));
        return out;
    }
}

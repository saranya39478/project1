package com.love.website;

import org.springframework.stereotype.Component;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class SharePasswordService {

    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_LENGTH_BYTES = 16;

    private final SecureRandom secureRandom = new SecureRandom();

    public HashedPassword hash(String password) {
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        secureRandom.nextBytes(salt);
        byte[] hash = derive(password, salt);
        Base64.Encoder encoder = Base64.getEncoder();
        return new HashedPassword(encoder.encodeToString(salt), encoder.encodeToString(hash));
    }

    public boolean matches(String password, String encodedSalt, String encodedHash) {
        Base64.Decoder decoder = Base64.getDecoder();
        byte[] salt = decoder.decode(encodedSalt);
        byte[] expectedHash = decoder.decode(encodedHash);
        byte[] actualHash = derive(password, salt);
        return MessageDigest.isEqual(expectedHash, actualHash);
    }

    private byte[] derive(String password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not verify the share password.", exception);
        } finally {
            spec.clearPassword();
        }
    }

    public record HashedPassword(String salt, String hash) {
    }
}

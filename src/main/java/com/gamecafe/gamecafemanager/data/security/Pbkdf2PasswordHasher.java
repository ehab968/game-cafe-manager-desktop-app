package com.gamecafe.gamecafemanager.data.security;

import com.gamecafe.gamecafemanager.domain.model.PasswordHash;
import com.gamecafe.gamecafemanager.domain.service.PasswordHasher;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Objects;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Salted PBKDF2 password hashing using Java's standard cryptography provider.
 */
public final class Pbkdf2PasswordHasher implements PasswordHasher {

    public static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int SALT_BYTES = 16;
    private static final int ITERATIONS = 120_000;
    private static final int KEY_BITS = 256;

    private final SecureRandom secureRandom;

    public Pbkdf2PasswordHasher() {
        this(new SecureRandom());
    }

    Pbkdf2PasswordHasher(SecureRandom secureRandom) {
        this.secureRandom = Objects.requireNonNull(secureRandom, "secureRandom");
    }

    @Override
    public PasswordHash hash(char[] password) {
        Objects.requireNonNull(password, "password");
        byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);
        return new PasswordHash(
                ALGORITHM,
                salt,
                ITERATIONS,
                derive(password, salt, ITERATIONS, ALGORITHM));
    }

    @Override
    public boolean verify(char[] password, PasswordHash expected) {
        Objects.requireNonNull(password, "password");
        Objects.requireNonNull(expected, "expected");
        byte[] actual = derive(
                password,
                expected.getSalt(),
                expected.getIterations(),
                expected.getAlgorithm());
        return MessageDigest.isEqual(actual, expected.getHash());
    }

    private byte[] derive(char[] password, byte[] salt, int iterations, String algorithm) {
        PBEKeySpec keySpec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance(algorithm)
                    .generateSecret(keySpec)
                    .getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Password hashing is unavailable", exception);
        } finally {
            keySpec.clearPassword();
        }
    }
}

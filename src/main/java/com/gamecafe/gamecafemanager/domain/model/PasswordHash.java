package com.gamecafe.gamecafemanager.domain.model;

import java.util.Objects;

/**
 * Password verifier material. It intentionally contains no plaintext password.
 */
public final class PasswordHash {

    private final String algorithm;
    private final byte[] salt;
    private final int iterations;
    private final byte[] hash;

    public PasswordHash(String algorithm, byte[] salt, int iterations, byte[] hash) {
        this.algorithm = Objects.requireNonNull(algorithm, "algorithm");
        this.salt = Objects.requireNonNull(salt, "salt").clone();
        this.iterations = iterations;
        this.hash = Objects.requireNonNull(hash, "hash").clone();
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public byte[] getSalt() {
        return salt.clone();
    }

    public int getIterations() {
        return iterations;
    }

    public byte[] getHash() {
        return hash.clone();
    }
}

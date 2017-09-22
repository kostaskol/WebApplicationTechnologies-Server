package com.WAT.airbnb.util.passwordverifier;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 1. Hashes the provided password using BCryptPasswordEncoder
 * 2. Tries to match the provided password against the hashed password stored in the database
 *      and returns according results
 * @author Kostas Kolivas
 * @version 1.0
 */
public class PasswordVerifier {
    private static int workload = 12;
    private String password;
    private boolean hashed;

    public PasswordVerifier(String pass, boolean hashed) {
        this.hashed = hashed;
        this.password = pass;
    }


    public void hash() throws RuntimeException {
        if (hashed) {
            throw new RuntimeException("Hashing hashed password");
        }

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        this.password = encoder.encode(this.password);
    }

    public boolean verify(String hash) throws RuntimeException {
        if (!hashed) {
            throw new RuntimeException("Verifying plain text password");
        }

        if (this.password == null || !this.password.startsWith("$2a$")) {
            throw new IllegalArgumentException("Invalid hash provided");
        }

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        return encoder.matches(hash, this.password);
    }

    public String getHash() { return this.password; }
}

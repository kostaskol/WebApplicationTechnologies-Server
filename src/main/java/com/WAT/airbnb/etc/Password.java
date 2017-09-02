package com.WAT.airbnb.etc;


import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class Password {
    private static int workload = 12;
    private String password;
    private boolean hashed;

    public Password(String pass, boolean hashed) {
        this.hashed = hashed;
        this.password = pass;
    }


    public void hash() throws RuntimeException {
        if (hashed) {
            throw new RuntimeException("Hashing hashed password");
        }

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String tmp = encoder.encode(this.password);
        this.password = tmp;
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

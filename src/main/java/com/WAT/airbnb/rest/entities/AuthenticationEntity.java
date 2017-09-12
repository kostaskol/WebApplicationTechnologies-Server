package com.WAT.airbnb.rest.entities;

public class AuthenticationEntity {
    private String token;

    public AuthenticationEntity() {}

    public String getToken() { return this.token; }

    public void setToken(String token) { this.token = token; }
}

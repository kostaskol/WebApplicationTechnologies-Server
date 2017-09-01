package com.WAT.airbnb.rest.entities;

public class UserUpdateEntity {
    private String bio;
    private String pNum;
    private String token;

    public UserUpdateEntity() {}

    public String getBio() { return this.bio; }

    public void setBio(String bio) { this.bio = bio; }

    public String getpNum() { return this.pNum; }

    public void setpNum(String pNum) { this.pNum = pNum; }

    public String getToken() { return this.token; }

    public void setToken(String token) { this.token = token; }
}

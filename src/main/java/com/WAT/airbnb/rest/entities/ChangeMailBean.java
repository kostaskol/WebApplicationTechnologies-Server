package com.WAT.airbnb.rest.entities;

public class ChangeMailBean {
    private String token;
    private String newMail;

    public ChangeMailBean() {}

    public void setNewMail(String newMail) {
        this.newMail = newMail;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getNewMail() {
        return newMail;
    }

    public String getToken() {
        return token;
    }
}

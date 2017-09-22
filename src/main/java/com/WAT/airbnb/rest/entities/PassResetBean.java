package com.WAT.airbnb.rest.entities;

public class PassResetBean {
    private String email;
    private String password;

    public PassResetBean() {}

    public void setEmail(String email) { this.email = email; }

    public String getEmail() { return this.email; }

    public void setPassword(String password) { this.password = password;}

    public String getPassword() { return this.password; }
}

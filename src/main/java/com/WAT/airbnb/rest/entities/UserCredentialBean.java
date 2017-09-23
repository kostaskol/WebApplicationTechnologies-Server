package com.WAT.airbnb.rest.entities;

public class UserCredentialBean {
    private String email;
    private String passwd;
    public UserCredentialBean() {}

    public String getEmail() { return this.email; }

    public void setEmail(String email) { this.email = email; }

    public String getPasswd() { return this.passwd; }

    public void setPasswd(String passwd) { this.passwd = passwd; }
}

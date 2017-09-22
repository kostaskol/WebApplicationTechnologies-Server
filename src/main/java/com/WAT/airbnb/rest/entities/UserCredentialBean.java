package com.WAT.airbnb.rest.entities;

public class UserCredentialBean {
    private String mail;
    private String passwd;
    public UserCredentialBean() {}

    public String getMail() { return this.mail; }

    public void setMail(String mail) { this.mail = mail; }

    public String getPasswd() { return this.passwd; }

    public void setPasswd(String passwd) { this.passwd = passwd; }
}

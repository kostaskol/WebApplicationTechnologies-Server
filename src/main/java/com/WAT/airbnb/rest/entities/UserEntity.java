package com.WAT.airbnb.rest.entities;


public class UserEntity {
    private String email;
    private String accType;
    private String firstName;
    private String lastName;
    private String pNum;
    private String dateOfBirth;
    private String country;
    private String picture;
    private String bio;
    private boolean approved;
    private boolean enoughData;

    private String token;



    public UserEntity() {}

    public String getEmail() {
        return email;
    }

    public String getBio() { return this.bio; }

    public void setBio(String bio) { this.bio = bio; }

    public String getToken() { return this.token; }

    public void setToken(String token) { this.token = token; }

    public boolean getApproved() { return this.approved; }

    public void setApproved(boolean approved) { this.approved = approved; }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public String getCountry() {
        return country;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public void setAccType(String accType) {
        this.accType = accType;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setpNum(String pNum) {
        this.pNum = pNum;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public String getAccType() {
        return accType;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getpNum() {
        return pNum;
    }

    public String getPicture() {
        return picture;
    }

    public boolean getEnoughData() { return this.enoughData; }

    public void setEnoughData(boolean enoughData) { this.enoughData = enoughData; }
}

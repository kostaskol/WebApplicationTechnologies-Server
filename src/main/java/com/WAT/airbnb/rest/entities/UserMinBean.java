package com.WAT.airbnb.rest.entities;

public class UserMinBean {
    private int userId;
    private String email;
    private String firstName;
    private String accType;
    private String lastName;
    private boolean approved;
    private String picture;

    public UserMinBean() {}

    public void setAccType(String accType) { this.accType = accType; }

    public String getAccType() { return this.accType; }

    public void setUserId(int userId) { this.userId = userId; }

    public int getUserId() { return this.userId; }

    public void setEmail(String email) { this.email = email; }

    public String getEmail() { return this.email; }

    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getFirstName() { return this.firstName; }

    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getLastName() { return this.lastName; }

    public void setApproved(boolean approved) { this.approved = approved; }

    public boolean getApproved() { return this.approved; }

    public void setPicture(String picture) { this.picture = picture; }

    public String getPicture() { return this.picture; }
}

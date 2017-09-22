package com.WAT.airbnb.rest.entities;

public class SignUpBean {
    private String email;
    private String passwd;
    private String accountType;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String dateOfBirth;

    public SignUpBean() {}

    public String getEmail() { return this.email; }

    public void setEmail(String email) { this.email = email; }

    public String getPasswd() { return this.passwd; }

    public void setPasswd(String passwd) { this.passwd = passwd; }

    public String getAccountType() { return this.accountType; }

    public void setAccountType(String accountType) { this.accountType = accountType; }

    public String getFirstName() { return this.firstName; }

    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return this.lastName; }

    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPhoneNumber() { return this.phoneNumber; }

    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getDateOfBirth() { return this.dateOfBirth; }

    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }
}

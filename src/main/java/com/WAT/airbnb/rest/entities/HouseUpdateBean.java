package com.WAT.airbnb.rest.entities;

public class HouseUpdateBean {
    private String token;
    private HouseBean house;

    public HouseUpdateBean() {}

    public String getToken() { return this.token; }

    public void setToken(String token) { this.token = token; }

    public HouseBean getHouse() { return this.house; }

    public void setHouse(HouseBean house) { this.house = house; }
}

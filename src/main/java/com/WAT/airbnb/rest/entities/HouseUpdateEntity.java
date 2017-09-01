package com.WAT.airbnb.rest.entities;

public class HouseUpdateEntity {
    private String token;
    private HouseEntity house;

    public HouseUpdateEntity() {}

    public String getToken() { return this.token; }

    public void setToken(String token) { this.token = token; }

    public HouseEntity getHouse() { return this.house; }

    public void setHouse(HouseEntity house) { this.house = house; }
}

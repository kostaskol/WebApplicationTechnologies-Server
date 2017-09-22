package com.WAT.airbnb.rest.entities;

public class HouseMinBean {
    private int houseId;
    private String city;
    private String country;
    private int numRatings;
    private float minCost;
    private String picture;
    private boolean allowedToComment;

    public HouseMinBean() {}

    public int getHouseId() { return this.houseId; }

    public String getCity() { return this.city; }
    public String getCountry() { return this.country; }

    public String getPicture() { return this.picture; }

    public int getNumRatings() { return this.numRatings; }

    public float getMinCost() { return this.minCost; }

    public boolean getAllowedToComment() { return this.allowedToComment; }

    public void setHouseId(int houseId) { this.houseId = houseId; }

    public void setCity(String city) { this.city = city; }

    public void setCountry(String country) { this.country = country; }

    public void setPicture(String picture) { this.picture = picture; }

    public void setNumRatings(int num) { this.numRatings = num; }

    public void setMinCost(float minCost) { this.minCost = minCost; }

    public void setAllowedToComment(boolean allowedToComment) { this.allowedToComment = allowedToComment; }
}

package com.WAT.airbnb.rest.entities;

import java.util.ArrayList;
import java.util.List;

public class HouseEntity extends House {
    private String houseId;
    private float latitude;
    private float longitude;
    private String address;
    private String city;
    private String country;
    private Integer numBeds;
    private Integer numBaths;
    private Integer accommodates;
    private boolean livingRoom;
    private boolean smoking;
    private boolean pets;
    private boolean events;
    private boolean wifi;
    private boolean airconditioning;
    private boolean heating;
    private boolean kitchen;
    private boolean tv;
    private boolean parking;
    private boolean elevator;
    private Float area;
    private String description;
    private Integer minDays;
    private String instructions;
    private Integer numRatings;
    private String dateFrom;
    private String dateTo;
    private Float minCost;
    private Float costPerPerson;
    private Float costPerDay;
    private ArrayList<String> pictures;
    private ArrayList<String> excludedDates;
    private String ownerName;
    private int ownerId;

    public HouseEntity() {
        pictures = new ArrayList<>();
        excludedDates = new ArrayList<>();
    }

    public void setOwnerId(int id) { this.ownerId = id; }

    public Integer getOwnerId() { return this.ownerId; }

    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getOwnerName() { return this.ownerName; }

    public void addExcludedDate(String date) { this.excludedDates.add(date); }

    public void setExcludedDates(ArrayList<String> dates) { this.excludedDates = dates; }

    public ArrayList<String> getExcludedDates() { return this.excludedDates; }

    public void setCostPerDay(float costPerDay) { this.costPerDay = costPerDay; }

    public Float getCostPerDay() { return this.costPerDay; }


    public void setCountry(String country) {
        this.country = country;
    }

    public void setHouseId(String houseID) {
        this.houseId = houseID;
    }

    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setCity(String city) { this.city = city; }

    public void setNumBeds(int numBeds) {
        this.numBeds = numBeds;
    }

    public void setNumBaths(int numBaths) {
        this.numBaths = numBaths;
    }

    public void setAccommodates(int accommodates) {
        this.accommodates = accommodates;
    }

    public void setLivingRoom(boolean livingRoom) {
        this.livingRoom = livingRoom;
    }

    public void setSmoking(boolean smoking) {
        this.smoking = smoking;
    }

    public void setPets(boolean pets) {
        this.pets = pets;
    }

    public void setWifi(boolean wifi) {
        this.wifi = wifi;
    }

    public void setAirconditioning(boolean airconditioning) {
        this.airconditioning = airconditioning;
    }

    public void setHeating(boolean heating) {
        this.heating = heating;
    }

    public void setKitchen(boolean kitchen) {
        this.kitchen = kitchen;
    }

    public void setTv(boolean tv) {
        this.tv = tv;
    }

    public void setParking(boolean parking) {
        this.parking = parking;
    }

    public void setElevator(boolean elevator) {
        this.elevator = elevator;
    }

    public void setEvents(boolean events) {
        this.events = events;
    }

    public void setArea(float area) {
        this.area = area;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setMinDays(int minDays) {
        this.minDays = minDays;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public void setNumRatings(int numRatings) {
        this.numRatings = numRatings;
    }

    public void setDateFrom(String dateFrom) {
        this.dateFrom = dateFrom;
    }

    public void setDateTo(String dateTo) {
        this.dateTo = dateTo;
    }

    public void setMinCost(float minCost) {
        this.minCost = minCost;
    }

    public void setCostPerPerson(float costPerPerson) {
        this.costPerPerson = costPerPerson;
    }

    public void setPictures(ArrayList<String> pictures) {
        this.pictures = pictures;
    }

    public String getHouseId() {
        return houseId;
    }

    public Float getLatitude() {
        return latitude;
    }

    public Float getLongitude() {
        return longitude;
    }

    public String getAddress() {
        return address;
    }

    public String getCity() { return this.city; }

    public String getCountry() {
        return country;
    }

    public Integer getNumBeds() {
        return numBeds;
    }

    public Integer getNumBaths() {
        return numBaths;
    }

    public Integer getAccommodates() {
        return accommodates;
    }

    public boolean getLivingRoom() {
        return livingRoom;
    }

    public boolean getSmoking() {
        return smoking;
    }

    public boolean getPets() {
        return pets;
    }

    public boolean getEvents() {
        return events;
    }

    public Float getArea() {
        return area;
    }

    public String getDescription() {
        return description;
    }

    public Integer getMinDays() {
        return minDays;
    }

    public String getInstructions() {
        return instructions;
    }

    public Integer getNumRatings() {
        return numRatings;
    }

    public String getDateFrom() {
        return dateFrom;
    }

    public String getDateTo() {
        return dateTo;
    }

    public Float getMinCost() {
        return minCost;
    }

    public Float getCostPerPerson() {
        return costPerPerson;
    }

    public boolean getWifi() { return this.wifi; }

    public boolean getAirconditioning() { return this.airconditioning; }

    public boolean getHeating() { return this.heating; }

    public boolean getKitchen() { return this.kitchen; }

    public boolean getTv() { return this.tv; }

    public boolean getParking() { return this.parking; }

    public boolean getElevator() { return this.elevator; }

    public ArrayList<String> getPictures() {
        return pictures;
    }

    public void addPicture(String picture) { this.pictures.add(picture); }
}

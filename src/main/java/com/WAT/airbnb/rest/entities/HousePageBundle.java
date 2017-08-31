package com.WAT.airbnb.rest.entities;

import java.util.ArrayList;

public class HousePageBundle {
    private ArrayList<House> houses;
    private int numPages;

    public HousePageBundle() {}

    public ArrayList<House> getHouses() {
        return houses;
    }

    public void setHouses(ArrayList<House> houses) { this.houses = houses; }

    public int getNumPages() {
        return numPages;
    }

    public void setNumPages(int numPages) {
        this.numPages = numPages;
    }
}

package com.WAT.airbnb.rest.entities;

import java.util.ArrayList;

public class HouseResponseEntity {
    private ArrayList<HouseMinEntity> entities;
    private int status;

    public ArrayList<HouseMinEntity> getEntities() {
        return entities;
    }

    public void setEntities(ArrayList<HouseMinEntity> entities) {
        this.entities = entities;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}

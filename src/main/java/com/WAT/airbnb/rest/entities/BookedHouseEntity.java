package com.WAT.airbnb.rest.entities;

public class BookedHouseEntity {
    private int bookingId;
    private HouseMinEntity house;
    private String dateFrom;
    private String dateTo;

    public BookedHouseEntity() {}

    public int getBookingId() { return this.bookingId; }

    public void setBookingId(int bookingId) { this.bookingId = bookingId; }

    public HouseMinEntity getHouse() { return this.house; }

    public void setHouse(HouseMinEntity entity) { this.house = entity; }

    public String getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(String date) { this.dateFrom = date; }


    public String getDateTo() {
        return dateTo;
    }

    public void setDateTo(String date) { this.dateTo = date; }

}

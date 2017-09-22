package com.WAT.airbnb.rest.entities;

public class BookedHouseBean {
    private int bookingId;
    private HouseMinBean house;
    private String dateFrom;
    private String dateTo;

    public BookedHouseBean() {}

    public int getBookingId() { return this.bookingId; }

    public void setBookingId(int bookingId) { this.bookingId = bookingId; }

    public HouseMinBean getHouse() { return this.house; }

    public void setHouse(HouseMinBean entity) { this.house = entity; }

    public String getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(String date) { this.dateFrom = date; }


    public String getDateTo() {
        return dateTo;
    }

    public void setDateTo(String date) { this.dateTo = date; }

}

package com.WAT.airbnb.rest.entities;

public class BookingEntity {
    private String token;
    private int bookingId;
    private int houseId;
    private int guests;
    private String dateFrom;
    private String dateTo;

    public BookingEntity() {}

    public String getToken() { return this.token; }

    public void setToken(String token) { this.token = token; }

    public int getBookingId() { return this.bookingId; }

    public void setBookingId(int bookingId) { this.bookingId = bookingId; }

    public int getHouseId() { return this.houseId; }

    public void setHouseId(int houseId) { this.houseId = houseId; }

    public int getGuests() { return this.guests; }

    public void setGuests(int guests) { this.guests = guests; }

    public String getDateFrom() { return this.dateFrom; }

    public void setDateFrom(String dateFrom) { this.dateFrom = dateFrom; }

    public String getDateTo() { return this.dateTo; }

    public void setDateTo(String dateTo) { this.dateTo = dateTo; }
}

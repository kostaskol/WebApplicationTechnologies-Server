package com.WAT.airbnb;

import com.WAT.airbnb.db.DataSource;
import com.WAT.airbnb.util.DateRange;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Path("/date")
public class DateFixer {
    @Path("/fix")
    @GET
    public Response fixDates() {
        Connection housesCon = null;
        Connection bookCon = null;
        ResultSet houseRs = null;
        ResultSet bookRs = null;
        try {
            housesCon = DataSource.getInstance().getConnection();
            bookCon = DataSource.getInstance().getConnection();

            String query = "SELECT houseID, dateFrom, dateTo FROM bookings ORDER BY houseID ASC";
            Statement bookSt = bookCon.createStatement();
            bookRs = bookSt.executeQuery(query);
            query = "SELECT houseID, dateFrom, dateTo FROM houses ORDER BY houseID ASC";
            Statement houseSt = housesCon.createStatement();
            houseRs = houseSt.executeQuery(query);


            HashMap<Integer, ArrayList<Date[]>> bookMap = new HashMap<>();
            HashMap<Integer, Date[]> houseMap = new HashMap<>();

            while (bookRs.next()) {

                Date[] bookDates = new Date[2];
                bookDates[0] = bookRs.getDate("dateFrom");
                bookDates[1] = bookRs.getDate("dateTo");
                if (bookMap.containsKey(bookRs.getInt("houseID"))) {
                    bookMap.get(bookRs.getInt("houseID")).add(bookDates);
                } else {
                    ArrayList<Date[]> dateList = new ArrayList<>();
                    dateList.add(bookDates);
                    bookMap.put(bookRs.getInt("houseID"), dateList);
                }
            }

            try {
                bookCon.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }


            while (houseRs.next()) {
                Date[] houseDates = new Date[2];
                houseDates[0] = houseRs.getDate("dateFrom");
                houseDates[1] = houseRs.getDate("dateTo");
                houseMap.put(houseRs.getInt("houseID"), houseDates);
            }

            try {
                housesCon.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }

            for (Integer houseID : houseMap.keySet()) {

                if (bookMap.containsKey(houseID)) {
                    Date[] houseDates = houseMap.get(houseID);
                    ArrayList<Date[]> bookDatesList = bookMap.get(houseID);

                    Date lastDateTo = null;

                    for (Date[] date : bookDatesList) {
                        Connection availCon = DataSource.getInstance().getConnection();
                        query = "UPDATE available_dates_t set availableFrom = ?, availableTo = ?" +
                                "WHERE houseID = ?";
                        PreparedStatement pSt = availCon.prepareStatement(query);

                        if (lastDateTo == null) {
                            pSt.setDate(1, date[1]);
                        } else {
                            pSt.setDate(1, lastDateTo);
                        }

                        lastDateTo = date[1];
                        pSt.setDate(2, houseDates[1]);
                        pSt.setInt(3, houseID);
                        pSt.execute();
                        try {
                            availCon.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }


                    // List<LocalDate> houseDatesList = new DateRange(houseDates[0], houseDates[1]).toList();
                    // List<LocalDate> bookDatesList = new DateRange(bookDates[0], bookDates[1]).toList();

                }
            }

            return Response.ok().build();
        } catch (IOException | SQLException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

    }

    @Path("/generatebookings")
    @GET
    public Response generateBookings() {
        Connection con = null;
        Connection userCon = null;
        Connection houseCon = null;
        try {
            con = DataSource.getInstance().getConnection();
            userCon = DataSource.getInstance().getConnection();
            houseCon = DataSource.getInstance().getConnection();

            String query = "SELECT userID from users";
            Statement userSt = userCon.createStatement();
            ResultSet userRs = userSt.executeQuery(query);
            query = "SELECT houseID, dateFrom, dateTo FROM houses";
            Statement houseSt = houseCon.createStatement();
            ResultSet houseRs = houseSt.executeQuery(query);
            ArrayList<Integer> userList = new ArrayList<>();
            ArrayList<Integer> houseList = new ArrayList<>();
            HashMap<Integer, Date[]> dateMap = new HashMap<>();

            while (userRs.next()) {
                userList.add(userRs.getInt("userID"));
            }

            try {
                userCon.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }

            try {
                userRs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }

            while (houseRs.next()) {
                houseList.add(houseRs.getInt("houseID"));
                Date[] dates = new Date[2];
                dates[0] = houseRs.getDate("dateFrom");
                dates[1] = houseRs.getDate("dateTo");
                dateMap.put(houseRs.getInt("houseID"), dates);
            }

            try {
                houseCon.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }

            try {
                houseRs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }



            for (int i = 0; i < 30; i++) {
                Connection bookingCon = DataSource.getInstance().getConnection();
                int randUser = userList.get(ThreadLocalRandom.current().nextInt(0, userList.size()));
                int randHouse = ThreadLocalRandom.current().nextInt(0, houseList.size());
                int randHouseId = houseList.get(randHouse);
                Date[] dates = dateMap.get(randHouseId);
                if (dates == null) {
                    System.out.println("Dates is null");
                    return Response.ok().build();
                }
                DateRange dateRange = new DateRange(dates[0], dates[1]);
                List<LocalDate> dateList = dateRange.toList();
                int randDateFrom = ThreadLocalRandom.current().nextInt(0, dateList.size() / 2);
                LocalDate dateFrom = dateList.get(randDateFrom);
                int randDateTo = ThreadLocalRandom.current().nextInt(dateList.size() / 2, dateList.size());
                LocalDate dateTo = dateList.get(randDateTo);
                int randGuests = ThreadLocalRandom.current().nextInt(0, 5);
                query = "INSERT INTO bookings (userID, houseID, guests, dateFrom, dateTo)" +
                        "values (?, ?, ?, ?, ?)";
                PreparedStatement pSt = bookingCon.prepareStatement(query);
                pSt.setInt(1, randUser);
                pSt.setInt(2, randHouseId);
                pSt.setInt(3, randGuests);
                pSt.setDate(4, Date.valueOf(dateFrom));
                pSt.setDate(5, Date.valueOf(dateTo));
                pSt.execute();
                try {
                    bookingCon.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                try {
                    pSt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            return Response.ok().build();
        } catch (IOException | SQLException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Path("/fillavailable")
    @GET
    public Response fillAvailable() {
        Connection getCon = null;
        Connection putCon = null;
        try {
            getCon = DataSource.getInstance().getConnection();
            putCon = DataSource.getInstance().getConnection();

            String query = "SELECT houseID, dateFrom, dateTo from houses";
            Statement st = getCon.createStatement();
            ResultSet rs =  st.executeQuery(query);
            while (rs.next()) {
                query = "INSERT INTO available_dates_t (houseID, availableFrom, availableTo)" +
                        "VALUES (?, ?, ?)";
                PreparedStatement pSt = putCon.prepareStatement(query);
                pSt.setInt(1, rs.getInt("houseID"));
                pSt.setDate(2, rs.getDate("dateFrom"));
                pSt.setDate(3, rs.getDate("dateTo"));
                pSt.execute();
            }

            return Response.ok().build();
        } catch (IOException | SQLException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (getCon != null) {
                try {
                    getCon.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            if (putCon != null) {
                try {
                    putCon.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

package com.WAT.airbnb.rest.houses.bookings;

import com.WAT.airbnb.db.DataSource;
import com.WAT.airbnb.etc.Constants;
import com.WAT.airbnb.etc.Helpers;
import com.WAT.airbnb.rest.Authenticator;
import com.WAT.airbnb.rest.entities.*;
import com.google.gson.Gson;
import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.sql.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Path("/booking")
public class BookingControl {
    @Path("/new")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response newBooking(String json) {
        Gson gson = new Gson();
        BookingEntity entity = gson.fromJson(json, BookingEntity.class);
        List<String> scopes = Helpers.ScopeFiller.fillScope(Constants.TYPE_USER);
        Authenticator auth = new Authenticator(entity.getToken(), scopes);

        if (!auth.authenticate()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        int userId = auth.getId();

        Connection bookCon = null;

        try {
            bookCon = DataSource.getInstance().getConnection();
            String insert = "INSERT INTO bookings (userID, houseID, guests, dateFrom, dateTo)" +
                    "VALUES (?, ?, ?, ?, ?)";
            PreparedStatement pSt = bookCon.prepareStatement(insert);
            pSt.setInt(1, userId);
            pSt.setInt(2, entity.getHouseId());
            pSt.setInt(3, entity.getGuests());
            pSt.setDate(4, Helpers.DateHelper.stringToDate(entity.getDateFrom()));
            pSt.setDate(5, Helpers.DateHelper.stringToDate(entity.getDateTo()));
            pSt.execute();
            return Response.ok().build();
        } catch (SQLException | IOException | ParseException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (bookCon != null) {
                try {
                    bookCon.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Path("/getrentersbookings")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRentersBookings(String token) {
        List<String> scopes = Helpers.ScopeFiller.fillScope(Constants.TYPE_RENTER);
        Authenticator auth = new Authenticator(token, scopes);
        if (!auth.authenticate()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        int userId = auth.getId();
        Connection con = null;
        PreparedStatement pSt = null;
        ResultSet rs = null;
        try {
            con = DataSource.getInstance().getConnection();
            String query = "SELECT " +
                    "users.userID, users.firstName, users.lastName, " + // From users

                    "houses.houseID, houses.city, houses.country, houses.rating, houses.numRatings, houses.minCost, " + // From houses

                    "photographs.pictureURL, " +

                    "bookings.bookingID, bookings.dateFrom, bookings.dateTo " +

                    "FROM houses, bookings, users, photographs " +
                    "WHERE " +
                    "users.userID = ? AND bookings.houseID = houses.houseID AND " +
                    "houses.ownerID = users.userID AND houses.houseID = photographs.houseID";

            pSt = con.prepareStatement(query);
            pSt.setInt(1, userId);
            rs = pSt.executeQuery();

            ArrayList<BookedHouseEntity> entities = new ArrayList<>();
            while (rs.next()) {
                HouseMinEntity minEntity = new HouseMinEntity();
                minEntity.setHouseId(rs.getInt("houseID"));
                minEntity.setCity(rs.getString("city"));
                minEntity.setCountry(rs.getString("country"));
                minEntity.setRating(rs.getFloat("rating"));
                minEntity.setNumRatings(rs.getInt("numRatings"));
                minEntity.setMinCost(rs.getFloat("minCost"));
                minEntity.setPicture(Helpers.FileHelper.getFileAsString(rs.getString("pictureURL")));

                BookedHouseEntity bookedHouseEntity = new BookedHouseEntity();
                bookedHouseEntity.setBookingId(rs.getInt("bookingID"));
                bookedHouseEntity.setDateFrom(Helpers.DateHelper.dateToString(rs.getDate("dateFrom")));
                bookedHouseEntity.setDateTo(Helpers.DateHelper.dateToString(rs.getDate("dateTo")));
                bookedHouseEntity.setHouse(minEntity);

                entities.add(bookedHouseEntity);
            }

            Gson gson = new Gson();
            String jsonString = gson.toJson(entities);
            return Response.ok(jsonString).build();
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            Helpers.ConnectionCloser.closeAll(con, pSt, rs);
        }
    }

    @Path("/getusersbookings")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUsersBookings(String token) {
        List<String> scopes = Helpers.ScopeFiller.fillScope(Constants.TYPE_USER);
        Authenticator auth = new Authenticator(token, scopes);
        if (!auth.authenticate()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        int userId = auth.getId();

        Connection con = null;
        PreparedStatement pSt = null;
        ResultSet rs = null;
        HashMap<Integer, String[]> houseMap = new HashMap<>();
        try {
            con = DataSource.getInstance().getConnection();
            String query = "SELECT houseID, bookingID, dateFrom, dateTo FROM bookings WHERE userID = ?";
            pSt = con.prepareStatement(query);
            pSt.setInt(1, userId);
            rs = pSt.executeQuery();
            while (rs.next()) {
                String[] dates = new String[3];
                dates[0] = Helpers.DateHelper.dateToString(rs.getDate("dateFrom"));
                dates[1] = Helpers.DateHelper.dateToString(rs.getDate("dateTo"));
                dates[2] = String.valueOf(rs.getInt("bookingID"));
                houseMap.put(rs.getInt("houseID"), dates);
            }
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            Helpers.ConnectionCloser.closeAll(con, pSt, rs);
        }

        ArrayList<BookedHouseEntity> entities = new ArrayList<>();

        for (Integer houseId : houseMap.keySet()) {
            try {
                con = DataSource.getInstance().getConnection();
                String query = "SELECT houseID, city, country, rating, numRatings, minCost FROM houses WHERE houseID = ?";
                pSt = con.prepareStatement(query);
                pSt.setInt(1, houseId);
                rs = pSt.executeQuery();
                HousePageBundle entityBundle = Helpers.HouseGetter.getHouseMinList(rs);
                BookedHouseEntity entity = new BookedHouseEntity();
                for (House minEntity : entityBundle.getHouses()) {
                    HouseMinEntity houseMinEntity = (HouseMinEntity) minEntity;
                    String[] dates = houseMap.get(houseId);
                    entity.setHouse(houseMinEntity);
                    entity.setDateFrom(dates[0]);
                    entity.setDateTo(dates[1]);
                    entity.setBookingId(Integer.parseInt(dates[2]));
                    entities.add(entity);
                }
            } catch (SQLException | IOException e) {
                e.printStackTrace();
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            } finally {
                Helpers.ConnectionCloser.closeAll(con, pSt, rs);
            }
        }

        Gson gson = new Gson();
        String jsonString = gson.toJson(entities);
        return Response.ok(jsonString).build();
    }

    @Path("/delete/{bookingId}")
    @DELETE
    @Consumes(MediaType.TEXT_PLAIN)
    public Response deleteBooking(@PathParam("bookingId") int bookingId,
                                  String token) {
        List<String> scopes = Helpers.ScopeFiller.fillScope(Constants.TYPE_USER);
        Authenticator auth = new Authenticator(token, scopes);

        if (!auth.authenticate()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        int userId = auth.getId();

        Connection con = null;
        PreparedStatement pSt = null;
        ResultSet rs = null;
        try {
            con = DataSource.getInstance().getConnection();
            String query = "SELECT NULL FROM bookings WHERE bookingId = ? AND dateTo < NOW() LIMIT 1";
            pSt = con.prepareStatement(query);
            pSt.setInt(1, bookingId);
            rs = pSt.executeQuery();
            if (!rs.next()) {
                return Response.ok("{\"status\": " + Constants.BOOKING_NOT_EXPIRED + "}").build();
            }

            Helpers.ConnectionCloser.closeAll(null, pSt, rs);

            query = "SELECT NULL FROM bookings WHERE bookingId = ? AND userID = ?";
            pSt = con.prepareStatement(query);
            pSt.setInt(1, bookingId);
            pSt.setInt(2, userId);
            rs = pSt.executeQuery();
            if (!rs.next()) {
                return Response.ok("{\"status\": " + Constants.BOOKING_NOT_OWNED + "}").build();
            }
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            Helpers.ConnectionCloser.closeAll(con, pSt, rs);
        }


        try {
            con = DataSource.getInstance().getConnection();
            String delete = "DELETE FROM bookings WHERE bookingID = ? and userID = ?";
            pSt = con.prepareStatement(delete);
            pSt.execute();

            return Response.ok().build();
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            Helpers.ConnectionCloser.closeAll(con, pSt, null);
        }


    }
}

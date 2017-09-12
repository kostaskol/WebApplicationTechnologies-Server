package com.WAT.airbnb;

import com.WAT.airbnb.db.DataSource;
import com.WAT.airbnb.etc.*;
import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.sql.*;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

// TODO: Add Reviews to database

@Path("/fixdb")
public class DBFixer {
    @Path("/fillhouses")
    @GET
    public Response fillHouses() {
        try {
            Reader in = new FileReader(Constants.DIR + "/dataset/listings.csv");
            Connection con = null;
            PreparedStatement pSt = null;
            try {
                int i = 0;
                con = DataSource.getInstance().getConnection();
                for (CSVRecord record : CSVFormat.DEFAULT.withHeader().parse(in)) {
                    System.out.println("Inserting house #" + i++);
                    String desc = record.get("description");
                    String instr = record.get("transit");
                    String city = record.get("city");
                    String country = record.get("country");
                    float lng = Float.parseFloat(record.get("longitude"));
                    float lat = Float.parseFloat(record.get("latitude"));
                    int baths;
                    int beds;
                    try {
                        baths = (int) Float.parseFloat(record.get("bathrooms"));
                        beds = (int) Float.parseFloat(record.get("bedrooms"));
                    } catch (NumberFormatException e) {
                        baths = randomInt(1, 10);
                        beds = randomInt(1, 5);
                    }
                    float area;
                    try {
                        area = Float.parseFloat(record.get("square_feet"));
                    } catch (NumberFormatException e) {
                        area = 0;
                    }
                    String tmp = record.get("price");
                    float price = Float.parseFloat(tmp.substring(1, tmp.length() - 1));
                    Date dateFrom, dateTo, tmpDate;
                    Date today = new Date(Calendar.getInstance().getTime().getTime());
                    Date maxDateFrom = Helpers.DateHelper.stringToDate("2020-12-1");
                    Date maxDate = Helpers.DateHelper.stringToDate("2020-12-31");
                    tmpDate = dateFrom = randomDate(today, maxDateFrom);
                    dateTo = randomDate(tmpDate, maxDate);

                    String insertSt = "INSERT INTO houses (ownerID, latitude, longitude, city, country, numBeds," +
                            "numBaths, accommodates, hasLivingRoom, smokingAllowed, petsAllowed, eventsAllowed, wifi, " +
                            "airconditioning, heating, kitchen, tv, parking, elevator, area," +
                            "description, minDays, instructions, rating, numRatings, dateFrom, dateTo, minCost," +
                            "costPerPerson, costPerDay) " +
                            "VALUES" +
                            "(" +
                            "   ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?" +
                            ")";
                    pSt = con.prepareStatement(insertSt, Statement.RETURN_GENERATED_KEYS);
                    pSt.setInt(1, getRandomUser());  // owner id
                    pSt.setFloat(2, lat); // latitude
                    pSt.setFloat(3, lng); // longitude
                    pSt.setString(4, city);
                    pSt.setString(5, country);
                    pSt.setInt(6, beds); // number of beds
                    pSt.setInt(7, baths); // number of baths
                    pSt.setInt(8, beds);
                    pSt.setBoolean(9, randomBoolean()); // has living room
                    pSt.setBoolean(10, randomBoolean()); // smoking allowed
                    pSt.setBoolean(11, randomBoolean()); // pets allowed
                    pSt.setBoolean(12, randomBoolean()); // events allowed
                    pSt.setBoolean(13, randomBoolean()); // has wifi
                    pSt.setBoolean(14, randomBoolean()); // has airconditioning
                    pSt.setBoolean(15, randomBoolean()); // has heating
                    pSt.setBoolean(16, randomBoolean()); // has a kitchen
                    pSt.setBoolean(17, randomBoolean()); // has a tv
                    pSt.setBoolean(18, randomBoolean()); // offers parking
                    pSt.setBoolean(19, randomBoolean()); // elevator

                    pSt.setFloat(20, area); // area
                    pSt.setString(21, desc); // description
                    pSt.setInt(22, randomInt(0, 20)); // minimum number of days
                    pSt.setString(23, instr); // instructions
                    pSt.setFloat(24, randomFloat(0f, 5f)); // average rating
                    pSt.setInt(25, randomInt(0, 600)); // number of ratings
                    pSt.setDate(26, dateFrom); // available from
                    pSt.setDate(27, dateTo); // available to
                    pSt.setFloat(28, price); // minimum cost
                    pSt.setFloat(29, price / 5f); // cost per person
                    pSt.setFloat(30, price / 3f); // cost per day
                    pSt.execute();
                    pSt.close();
                }
            } catch (SQLException | IOException e) {
                e.printStackTrace();
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            } finally {
                Helpers.ConnectionCloser.closeAll(con, pSt, null);

            }

            return Response.ok().build();
        } catch (IOException | IllegalStateException | IllegalArgumentException | ParseException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    private ArrayList<Integer> getUsers() {
        Connection con = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            con = DataSource.getInstance().getConnection();
            String query = "SELECT userID FROM users";
            st = con.createStatement();
            rs = st.executeQuery(query);
            ArrayList<Integer> result = new ArrayList<>();
            while (rs.next()) {
                result.add(rs.getInt("userID"));
            }

            return result;
        } catch (IOException | SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            Helpers.ConnectionCloser.closeAll(con, st, rs);
        }
    }

    private Integer getRandomUser() {
        ArrayList<Integer> userList = getUsers();
        if (userList == null) return null;
        return userList.get(ThreadLocalRandom.current().nextInt(0, userList.size()));
    }

    private Date randomDate(Date start, Date end) throws ParseException {
        DateRange dr = new DateRange(start, end);
        List<LocalDate> dateList = dr.toList();
        LocalDate randomDate = dateList.get(ThreadLocalRandom.current().nextInt(0, dateList.size()));
        return Helpers.DateHelper.stringToDate(randomDate.getYear() + "-" +
                randomDate.getMonthValue() + "-" +
                randomDate.getDayOfMonth());
    }

    private int randomInt(int start, int end) {
        return ThreadLocalRandom.current().nextInt(start, end);
    }

    private boolean randomBoolean() {
        return ThreadLocalRandom.current().nextInt(1, 100) > 50;
    }

    private float randomFloat(float s, float e) {
        return ThreadLocalRandom.current().nextFloat() % e + s;
    }


}

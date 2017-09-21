package com.WAT.airbnb.rest.houses;

import com.WAT.airbnb.db.DataSource;
import com.WAT.airbnb.etc.Constants;
import com.WAT.airbnb.util.DateRange;
import com.WAT.airbnb.util.MyVector;
import com.WAT.airbnb.util.QueryBuilder;
import com.WAT.airbnb.rest.Authenticator;
import com.WAT.airbnb.rest.entities.*;
import com.WAT.airbnb.util.Tuple;
import com.WAT.airbnb.util.helpers.*;
import com.google.gson.Gson;
import info.debatty.java.lsh.LSHMinHash;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.sql.Date;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.*;

@Path("/house")
public class HouseControl {
    @Path("/register")
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response registerHouse(@FormDataParam("file") InputStream uploadedFileInputStream,
                                  @FormDataParam("file") FormDataContentDisposition fileDetails,
                                  @FormDataParam("token") String token,
                                  @FormDataParam("data") String jsonString) {
        Gson gson = new Gson();
        HouseEntity entity = gson.fromJson(jsonString, HouseEntity.class);
        Authenticator auth = new Authenticator(token, Constants.TYPE_RENTER);
        if (!auth.authenticate()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        entity.setNumRatings(0);

        try {
            String[] addr = ReverseGeocoder.convert(entity.getLatitude(), entity.getLongitude());
            if (addr != null) {
                entity.setAddress(addr[Constants.ADDR_OFFS]);
                entity.setCountry(addr[Constants.COUNTRY_OFFS]);
                entity.setCity(addr[Constants.CITY_OFFS]);
            }
        } catch (Exception e) {
            System.err.println("Caught exception");
            e.printStackTrace();
            entity.setAddress(null);
            entity.setCountry(null);
            entity.setCity(null);
        }

        System.out.println("Address:  " + entity.getAddress());


        int id = auth.getId();
        try (Connection con = DataSource.getInstance().getConnection()) {
            String insertSt = "INSERT INTO houses (ownerID, latitude, longitude, address, city, country, numBeds," +
                    "numBaths, accommodates, hasLivingRoom, smokingAllowed, petsAllowed, eventsAllowed, wifi, " +
                    "airconditioning, heating, kitchen, tv, parking, elevator, area," +
                    "description, minDays, instructions, rating, numRatings, dateFrom, dateTo, minCost," +
                    "costPerPerson, costPerDay) " +
                    "VALUES" +
                    "(" +
                    "   ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?" +
                    ")";
            PreparedStatement pSt = con.prepareStatement(insertSt, Statement.RETURN_GENERATED_KEYS);
            pSt.setInt(1, id);  // owner id
            pSt.setFloat(2, entity.getLatitude()); // latitude
            pSt.setFloat(3, entity.getLongitude()); // longitude
            pSt.setString(4, entity.getAddress());
            pSt.setString(5, entity.getCity());
            pSt.setString(6, entity.getCountry());
            pSt.setInt(7, entity.getNumBeds()); // number of beds
            pSt.setInt(8, entity.getNumBaths()); // number of baths
            pSt.setInt(9, entity.getAccommodates());
            pSt.setBoolean(10, entity.getLivingRoom()); // has living room
            pSt.setBoolean(11, entity.getSmoking()); // smoking allowed
            pSt.setBoolean(12, entity.getPets()); // pets allowed
            pSt.setBoolean(13, entity.getEvents()); // events allowed
            pSt.setBoolean(14, entity.getWifi()); // has wifi
            pSt.setBoolean(15, entity.getAirconditioning()); // has airconditioning
            pSt.setBoolean(16, entity.getHeating()); // has heating
            pSt.setBoolean(17, entity.getKitchen()); // has a kitchen
            pSt.setBoolean(18, entity.getTv()); // has a tv
            pSt.setBoolean(19, entity.getParking()); // offers parking
            pSt.setBoolean(20, entity.getTv());
            pSt.setFloat(21, entity.getArea()); // area
            pSt.setString(22, entity.getDescription()); // description
            pSt.setInt(23, entity.getMinDays()); // minimum number of days
            pSt.setString(24, entity.getInstructions()); // instructions
            pSt.setFloat(25, 0f); // average rating
            pSt.setInt(26, 0); // number of ratings
            pSt.setDate(27, DateHelper.stringToDate(entity.getDateFrom())); // available from
            pSt.setDate(28, DateHelper.stringToDate(entity.getDateTo())); // available to
            pSt.setFloat(29, entity.getMinCost()); // minimum cost
            pSt.setFloat(30, entity.getCostPerPerson()); // cost per person
            pSt.setFloat(31, entity.getCostPerDay()); // cost per day
            pSt.executeUpdate();

            Integer insertedId = null;
            try (ResultSet generatedKeys = pSt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    insertedId = generatedKeys.getInt(1);
                }
            }

            if (insertedId == null) {
                throw new SQLException("No inserted id");
            }

            String fileUrl = FileHelper.saveFile(uploadedFileInputStream, insertedId, fileDetails, false);

            insertSt = "INSERT INTO photographs (houseID, main, pictureURL)" +
                    "VALUES " +
                    "(" +
                    "?, ?, ?" +
                    ")";
            pSt = con.prepareStatement(insertSt);
            pSt.setInt(1, insertedId);
            pSt.setBoolean(2, true);
            pSt.setString(3, fileUrl);
            pSt.executeUpdate();


            return Response.ok().build();
        } catch (SQLException | IOException | ParseException sqle) {
            sqle.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

    }

    @Path("/search")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response searchHouse(String json) {
        Gson gson = new Gson();
        HouseEntity entity = gson.fromJson(json, HouseEntity.class);
        Connection con = null;
        ResultSet rs = null;
        System.out.println("Got request");
        PreparedStatement pSt = null;
        try {
            con = DataSource.getInstance().getConnection();
            boolean[] hasArgs = new boolean[11];
            QueryBuilder queryBuilder = new QueryBuilder("SELECT houseID, city, country, " +
                    "numRatings, minCost FROM houses WHERE 1 = 1 ");
            if (entity.getCountry() != null) {
                queryBuilder.and("country = ?");
                hasArgs[0] = true;
            } else
                hasArgs[0] = false;
            if (entity.getNumBeds() != null) {
                System.out.println("Adding beds");
                queryBuilder.and("numBeds >= ?");
                hasArgs[1] = true;
            } else
                hasArgs[1] = false;
            if (entity.getNumBaths() != null) {
                queryBuilder.and("numBaths >= ?");
                hasArgs[2] = true;
            } else
                hasArgs[2] = false;
            if (entity.getAccommodates() != null) {
                queryBuilder.and("accommodates >= ?");
                hasArgs[3] = true;
            } else
                hasArgs[3] = false;
            if (entity.getLivingRoom())
                queryBuilder.and("hasLivingRoom = 1");
            if (entity.getSmoking())
                queryBuilder.and("smokingAllowed = 1");
            if (entity.getPets())
                queryBuilder.and("petsAllowed = 1");
            if (entity.getEvents())
                queryBuilder.and("eventsAllowed = 1");
            if (entity.getWifi())
                queryBuilder.and("wifi = 1");
            if (entity.getAirconditioning())
                queryBuilder.and("airconditioning = 1");
            if (entity.getHeating())
                queryBuilder.and("heating = 1");
            if (entity.getKitchen())
                queryBuilder.and("kitchen = 1");
            if (entity.getTv())
                queryBuilder.and("tv = 1");
            if (entity.getParking())
                queryBuilder.and("parking = 1");
            if (entity.getElevator())
                queryBuilder.and("elevator = 1");
            if (entity.getArea() != null) {
                queryBuilder.and("area >= ?");
                hasArgs[4] = true;
            } else {
                hasArgs[4] = false;
            }
            if (entity.getMinDays() != null) {
                queryBuilder.and("minDays >= ?");
                hasArgs[5] = true;
            } else
                hasArgs[5] = false;
            if (entity.getDateFrom() != null && entity.getDateTo() != null) {
                System.out.println("None is null");
                queryBuilder.and("dateFrom <= ?").and("dateTo <= ?");
                hasArgs[6] = true;
            } else if (entity.getDateFrom() != null) {
                System.out.println("DateFrom is not null: " + entity.getDateFrom());
                queryBuilder.and("dateFrom <= ?");
                hasArgs[6] = false;
                hasArgs[7] = true;
            } else if (entity.getDateTo() != null) {
                queryBuilder.and("dateTo <= ?").and("dateFrom <= NOW()");
                hasArgs[6] = false;
                hasArgs[7] = false;
                hasArgs[8] = true;
            } else {
                hasArgs[6] = false;
                hasArgs[7] = false;
                hasArgs[8] = false;
            }
            if (entity.getMinCost() != null) {
                queryBuilder.and("minCost >= ?");
                hasArgs[9] = true;
            } else
                hasArgs[9] = false;
            if (entity.getCostPerPerson() != null) {
                queryBuilder.and("costPerPerson >= ?");
                hasArgs[10] = true;
            } else
                hasArgs[10] = false;

            pSt = con.prepareStatement(queryBuilder.toString());
            int curr = 1;
            if (hasArgs[0]) {
                pSt.setString(curr++, entity.getCountry());
            }
            if (hasArgs[1]) {
                pSt.setInt(curr++, entity.getNumBeds());
            }
            if (hasArgs[2]) {
                pSt.setInt(curr++, entity.getNumBaths());
            }
            if (hasArgs[3]) {
                pSt.setInt(curr++, entity.getAccommodates());
            }
            if (hasArgs[4]) {
                pSt.setFloat(curr++, entity.getArea());
            }
            if (hasArgs[5]) {
                pSt.setInt(curr++, entity.getMinDays());
            }
            if (hasArgs[6]) {
                try {
                    pSt.setDate(curr++, DateHelper.stringToDate(entity.getDateFrom()));
                    pSt.setDate(curr++, DateHelper.stringToDate(entity.getDateTo()));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            } else if (hasArgs[7]) {
                try {
                    pSt.setDate(curr++, DateHelper.stringToDate(entity.getDateFrom()));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            } else if (hasArgs[8]) {
                try {
                    pSt.setDate(curr++, DateHelper.stringToDate(entity.getDateTo()));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            if (hasArgs[9])
                pSt.setFloat(curr++, entity.getMinCost());
            if (hasArgs[10])
                pSt.setFloat(curr, entity.getCostPerPerson());


            System.out.println("Executing statement: " + pSt.toString());

            ArrayList<HouseMinEntity> minEntities = new ArrayList<>();

            rs = pSt.executeQuery();
            while (rs.next()) {
                HouseMinEntity minEntity = new HouseMinEntity();
                minEntity.setHouseId(rs.getInt("houseId"));
                minEntity.setCity(rs.getString("city"));
                minEntity.setCountry(rs.getString("country"));
                minEntity.setNumRatings(rs.getInt("numRatings"));
                minEntity.setMinCost(rs.getInt("minCost"));

                Connection picCon = DataSource.getInstance().getConnection();
                String query = "SELECT pictureURL from photographs WHERE houseId = ? LIMIT 1";
                pSt = picCon.prepareStatement(query);
                pSt.setInt(1, minEntity.getHouseId());
                ResultSet picRs = pSt.executeQuery();
                if (picRs.next()) {
                    minEntity.setPicture(FileHelper.getFileAsString(picRs.getString("pictureURL")));
                }

                try {
                    picCon.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                try {
                    picRs.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                try {
                    pSt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                minEntities.add(minEntity);
            }

            String response = gson.toJson(minEntities);
            return Response.ok(response).build();
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            ConnectionCloser.closeAll(con, pSt, rs);
        }
    }

    @Path("/getnumpages")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getNumPages() {
        Connection con = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            con = DataSource.getInstance().getConnection();
            String query = "SELECT COUNT(*) AS c FROM houses WHERE dateTo > NOW()";
            st = con.createStatement();
            rs = st.executeQuery(query);
            if (rs.next()) {
                int numPages = rs.getInt("c");
                numPages = numPages / Constants.PAGE_SIZE + 1;
                return Response.ok(numPages).build();
            } else {
                throw new SQLException("Empty result set");
            }
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            ConnectionCloser.closeAll(con, st, rs);
        }
    }


    @Path("/getpage/{pagenum}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPage(@PathParam("pagenum") int pageNum) {
        Connection con = null;
        ResultSet rs = null;
        PreparedStatement pSt = null;
        ArrayList<HouseMinEntity> entities = new ArrayList<>();
        try {

            con = DataSource.getInstance().getConnection();
            String query = "SELECT houseID, city, country, numRatings, minCost FROM houses " +
                    "WHERE dateTo > NOW() ORDER BY minCost ASC LIMIT ?, ?";
            pSt = con.prepareStatement(query);
            pSt.setInt(1, pageNum * Constants.PAGE_SIZE);
            pSt.setInt(2, Constants.PAGE_SIZE);
            rs = pSt.executeQuery();
            while (rs.next()) {
                HouseMinEntity entity = new HouseMinEntity();
                entity.setCity(rs.getString("city"));
                entity.setCountry(rs.getString("country"));
                entity.setNumRatings(rs.getInt("numRatings"));
                entity.setMinCost(rs.getFloat("minCost"));
                entity.setHouseId(rs.getInt("houseID"));

                Connection picCon = null;
                PreparedStatement picpSt = null;
                ResultSet picRs = null;
                try {
                    picCon = DataSource.getInstance().getConnection();
                    query = "SELECT thumbURL FROM photographs WHERE houseID = ? AND main = 1 LIMIT 1";
                    picpSt = picCon.prepareStatement(query);
                    picpSt.setInt(1, rs.getInt("houseID"));
                    picRs = picpSt.executeQuery();
                    if (picRs.next()) {
                        entity.setPicture(FileHelper.getFileAsString(picRs.getString("thumbURL")));
                    } else {
                        throw new SQLException("Empty result set");
                    }
                    entities.add(entity);
                } catch (SQLException | IOException e) {
                    e.printStackTrace();
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
                } finally {
                    ConnectionCloser.closeAll(picCon, picpSt, picRs);
                }

            }

            Gson gson = new Gson();
            String response = gson.toJson(entities);
            return Response.ok(response).build();
        } catch (IOException | SQLException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            ConnectionCloser.closeAll(con, pSt, rs);
        }
    }

    @Path("/getpredicted")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPredicted(String token) {
        Authenticator auth = new Authenticator(token, Constants.TYPE_USER);
        if (!auth.authenticate()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        int currentUser = auth.getId();
        long start = System.currentTimeMillis();
        Connection con = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            con = DataSource.getInstance().getConnection();
            st = con.createStatement();

            // Create a user and house map
            // (houseID => matrix index)
            String query = "SELECT DISTINCT(houseID) from comments";
            rs = st.executeQuery(query);


            HashMap<Integer, Integer> houseMap = new HashMap<>();
            HashMap<Integer, Integer> reverseHouseMap = new HashMap<>();
            int houseCount;
            for (houseCount = 0; rs.next(); houseCount++) {
                houseMap.put(rs.getInt("houseID"), houseCount);
                reverseHouseMap.put(houseCount, rs.getInt("houseID"));
            }

            try {
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }

            query = "SELECT DISTINCT(userID) FROM comments";
            rs = st.executeQuery(query);
            HashMap<Integer, Integer> userMap = new HashMap<>();
            HashMap<Integer, Integer> reverseUserMap = new HashMap<>();
            int userCount;
            for (userCount = 0; rs.next(); userCount++) {
                userMap.put(rs.getInt("userID"), userCount);
                reverseUserMap.put(userCount, rs.getInt("userID"));
            }



            query = "SELECT COUNT(*) AS c FROM comments WHERE userID = " + currentUser;
            rs = st.executeQuery(query);
            if (rs.next()) {
                int count = rs.getInt("c");
                if (count > 10) {
                    // get normally

                    boolean[][] userHouseMatrix = new boolean[userCount][houseCount];
                    for (int i = 0; i < userCount; i++) {
                        for (int j = 0; j < houseCount; j++) {
                            userHouseMatrix[i][j] = false;
                        }
                    }

                    query = "SELECT houseID, userID, rating FROM comments";
                    rs = st.executeQuery(query);
                    while (rs.next()) {
                        float rating = rs.getFloat("rating");
                        if (rating >= 0.5) {
                            userHouseMatrix[userMap.get(rs.getInt("userID"))]
                                    [houseMap.get(rs.getInt("houseID"))] = true;
                        }
                    }


                    ArrayList<Integer> otherUsers = getBucket(currentUser, houseCount, userHouseMatrix, userMap, false);

                    QueryBuilder qBuilder = new QueryBuilder("SELECT userID, houseID, rating FROM comments " +
                            "WHERE userID = " + currentUser + " ");
                    for (Integer user : otherUsers) {
                        if (user == null) continue; // There has been a case where a null value got into the array list
                        qBuilder.or("userID = " + user);
                    }

                    try {
                        rs.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                    System.out.println("Executing query : " + qBuilder.toString());
                    rs = st.executeQuery(qBuilder.toString());

                    HashMap<Integer, HashMap<Integer, Double>> ratingsMap = new HashMap<>();
                    // houseMap = new HashMap<>();
                    int i = 0;
                    ArrayList<Integer> ids = new ArrayList<>();

                    while (rs.next()) {
                        HashMap<Integer, Double> tmp;
                        if (ratingsMap.containsKey(rs.getInt("userID"))) {
                            tmp = ratingsMap.get(rs.getInt("userID"));
                        } else {
                            tmp = new HashMap<>();
                        }
                        tmp.put(rs.getInt("houseID"), rs.getDouble("rating"));
                        ratingsMap.put(rs.getInt("userID"), tmp);
                    }


                    MyVector[] userMatrix = new MyVector[ratingsMap.size()];

                    userMatrix[0] = new MyVector(currentUser, houseCount, true);

                    HashMap<Integer, Double> currentUserMap = ratingsMap.get(currentUser);

                    fixIndices(userMatrix[0], houseMap, currentUserMap);

                    i = 1;
                    HashMap<Integer, Integer> userIndexMap = new HashMap<>();
                    for (Integer id : ratingsMap.keySet()) {
                        if (id == currentUser) continue;
                        userMatrix[i] = new MyVector(id, houseCount, true);
                        userIndexMap.put(id, i);
                        currentUserMap = ratingsMap.get(id);
                        fixIndices(userMatrix[i++], houseMap, currentUserMap);
                    }

                    Map<Integer, Double> distanceMatrix = new HashMap<>();
                    for (i = 1; i < userMatrix.length; i++) {
                        double distance = userMatrix[0].cosineSim(userMatrix[i]);
                        distanceMatrix.put(userMatrix[i].getId(), distance);
                    }


                    Set<Map.Entry<Integer, Double>> entries = distanceMatrix.entrySet();
                    List<Map.Entry<Integer, Double>> entryList = new ArrayList<Map.Entry<Integer, Double>>(entries);

                    entryList.sort(MyVector.valueComparator);


                    ArrayList<Tuple<Integer, Double>> predicted = new ArrayList<>();
                    for (i = 0; i < Constants.MAX_NEIGHBOURS; i++) {
                        MyVector tmpVector = userMatrix[userIndexMap.get(entryList.get(0).getKey())];
                        for (int j = 0; j < userMatrix[0].size(); j++) {
                            if (userMatrix[0].get(j) == 0d && tmpVector.get(j) != 0d
                                    && !predicted.contains(
                                    new Tuple<Integer, Double>(reverseHouseMap.get(j), null))) {
                                predicted.add(new Tuple<Integer, Double>(reverseHouseMap.get(j), tmpVector.get(j)));
                            }
                        }
                    }

                    Comparator<Tuple<Integer, Double>> comp = new Comparator<Tuple<Integer, Double>>() {
                        @Override
                        public int compare(Tuple<Integer, Double> t1, Tuple<Integer, Double> t2) {
                            if (t1.right > t2.right)
                                return -1;
                            else if (t1.right < t2.right)
                                return 1;
                            return 0;
                        }
                    };

                    predicted.sort(comp);

                    Connection rCon = null;
                    PreparedStatement rSt = null;
                    ResultSet rRs = null;
                    try {
                        rCon = DataSource.getInstance().getConnection();
                        ArrayList<HouseMinEntity> entities = new ArrayList<>();
                        for (i = 0; i < Constants.K; i++) {
                            query = "SELECT houseID, city, country, numRatings, minCost FROM houses WHERE" +
                                    " houseID = ?";
                            rSt = rCon.prepareStatement(query);
                            rSt.setInt(1, predicted.get(i).left);
                            rRs = rSt.executeQuery();
                            while (rRs.next()) {
                                HouseMinEntity entity = new HouseMinEntity();
                                entity.setHouseId(rRs.getInt("houseID"));
                                entity.setCity(rRs.getString("city"));
                                entity.setCountry(rRs.getString("country"));
                                entity.setNumRatings(rRs.getInt("numRatings"));
                                entity.setMinCost(rRs.getFloat("minCost"));

                                query = "SELECT thumbURL FROM airbnb_t.photographs " +
                                        "WHERE houseID = " + rRs.getInt("houseID") + " LIMIT 1";
                                rSt = rCon.prepareStatement(query);
                                ResultSet tmpRs = rSt.executeQuery();
                                if (tmpRs.next()) {
                                    entity.setPicture(FileHelper.getFileAsString(tmpRs.getString("thumbURL")));
                                }

                                entities.add(entity);
                            }

                            ConnectionCloser.closeAll(null, rSt, rRs);
                        }

                        Gson gson = new Gson();
                        String response = gson.toJson(entities);
                        return Response.ok(response).build();
                    } catch (SQLException | IOException e) {
                        e.printStackTrace();
                        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
                    } finally {
                        ConnectionCloser.closeAll(rCon, rSt, rRs);
                    }
                } else if (count > 3){
                    query = "SELECT houseID FROM searches WHERE userID = " + currentUser;
                    ResultSet tmpRs = st.executeQuery(query);

                    try {
                        rs.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    boolean[][] userHouseMatrix = new boolean[userCount][houseCount];
                    for (int i = 0; i < userCount; i++)
                        for (int j = 0; j < houseCount; j++)
                            userHouseMatrix[i][j] = false;

                    query = "SELECT houseID, userID, rating FROM comments";
                    rs = st.executeQuery(query);
                    int currentUserIndex;
                    while (rs.next())
                        if (rs.getDouble("rating") >= 0.5)
                            userHouseMatrix
                                    [userMap.get(rs.getInt("userID"))]
                                    [houseMap.get(rs.getInt("houseID"))] = true;

                    int index;
                    if (count == 0)
                        index = userCount - 1;
                    else
                        index = userMap.get(currentUser);

                    while (rs.next())
                        userHouseMatrix[index][houseMap.get(rs.getInt("houseID"))] = true;

                    try {
                        rs.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                    ArrayList<Integer> otherUsers = getBucket(currentUser, houseCount, userHouseMatrix, reverseUserMap, true);

                    MyVector[] userMatrix = new MyVector[otherUsers.size() + 1];

                    userMatrix[0] = new MyVector(userHouseMatrix[userCount - 1], currentUser);
                    System.out.println("Other users' size = " + otherUsers.size());
                    for (int i = 1; i < otherUsers.size() + 1; i++) {
                        userMatrix[i] =
                                new MyVector(userHouseMatrix[userMap.get(otherUsers.get(i - 1))], otherUsers.get(i - 1));
                    }


                    Map<Integer, Double> distanceMatrix = new HashMap<>();
                    System.out.println("userMatrix length = " + userMatrix.length);
                    for (int i = 0; i < userMatrix.length; i++) {
                        if (userMatrix[i] == null) {
                            System.out.println("UserMatrix @ index " + i + " is null");
                        }
                    }
                    for (int i = 1; i < userMatrix.length; i++) {
                        double distance = userMatrix[0].cosineSim(userMatrix[i], 1);
                        distanceMatrix.put(i - 1, distance);
                    }


                    Set<Map.Entry<Integer, Double>> entries = distanceMatrix.entrySet();
                    List<Map.Entry<Integer, Double>> entryList =
                            new ArrayList<>(entries);

                    Comparator<Map.Entry<Integer, Double>> comp =
                            new Comparator<Map.Entry<Integer, Double>>() {
                                @Override
                                public int compare(Map.Entry<Integer, Double> e1,
                                                   Map.Entry<Integer, Double> e2) {
                                    if (e1.getValue() > e2.getValue()) {
                                        return -1;
                                    } else if (e1.getValue() < e2.getValue()) {
                                        return 1;
                                    }
                                    return 0;
                                }
                            };

                    entryList.sort(comp);

                    ArrayList<Tuple<Integer, Boolean>> predicted = new ArrayList<>();

                    for (int i = 0; i < (Constants.MAX_NEIGHBOURS > userMatrix.length ?
                            userMatrix.length : Constants.MAX_NEIGHBOURS); i++) {
                        MyVector tmpVector = userMatrix[entryList.get(i).getKey()];
                        if (tmpVector == null) {
                            System.out.println("tmpVector is null");
                        }
                        for (int j = 0; j < userMatrix[0].size(); j++) {
                            if (userMatrix[0].get(j, 0) == null) {
                                System.out.println("NULL");
                            }
                            if (!userMatrix[0].get(j, 0) && tmpVector.get(j, 0)
                                    && !predicted.contains(
                                    new Tuple<Integer, Boolean>(reverseHouseMap.get(j), null))) {
                                predicted.add(new Tuple<Integer, Boolean>(reverseHouseMap.get(j), tmpVector.get(j, 0)));
                            }
                        }
                    }

                    Connection rCon = null;
                    PreparedStatement rSt = null;
                    ResultSet rRs = null;
                    try {
                        rCon = DataSource.getInstance().getConnection();
                        ArrayList<HouseMinEntity> entities = new ArrayList<>();
                        for (int i = 0; i < Constants.K; i++) {
                            query = "SELECT houseID, city, country, numRatings, minCost FROM houses WHERE" +
                                    " houseID = ?";
                            rSt = rCon.prepareStatement(query);
                            rSt.setInt(1, predicted.get(i).left);
                            rRs = rSt.executeQuery();
                            while (rRs.next()) {
                                HouseMinEntity entity = new HouseMinEntity();
                                entity.setHouseId(rRs.getInt("houseID"));
                                entity.setCity(rRs.getString("city"));
                                entity.setCountry(rRs.getString("country"));
                                entity.setNumRatings(rRs.getInt("numRatings"));
                                entity.setMinCost(rRs.getFloat("minCost"));

                                query = "SELECT thumbURL FROM airbnb_t.photographs " +
                                        "WHERE houseID = " + rRs.getInt("houseID") + " LIMIT 1";
                                rSt = rCon.prepareStatement(query);
                                tmpRs = rSt.executeQuery();
                                if (tmpRs.next()) {
                                    entity.setPicture(FileHelper.getFileAsString(tmpRs.getString("thumbURL")));
                                }

                                entities.add(entity);
                            }

                            ConnectionCloser.closeAll(null, rSt, rRs);
                        }

                        Gson gson = new Gson();
                        String response = gson.toJson(entities);
                        return Response.ok(response).build();
                    } catch (SQLException | IOException e) {
                        e.printStackTrace();
                        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
                    } finally {
                        ConnectionCloser.closeAll(rCon, rSt, rRs);
                    }
                } else {
                    return Response.ok("{\"status\": " + Constants.NOT_ENOUGH_DATA + "}").build();
                }
            }

            return Response.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            ConnectionCloser.closeAll(con, st, rs);
        }
    }


    private ArrayList<Integer> getBucket(int currentUser, int houseCount, boolean[][] userHouseMatrix,
                                         HashMap<Integer, Integer> userMap, boolean sparse) {
        int currentUserIndex = userMap.get(currentUser);

        int stages = 2;
        int buckets;
        if (sparse) {
            buckets = 500;
        } else {
            buckets = 4700;
        }
        LSHMinHash lsh = new LSHMinHash(stages, buckets, houseCount);

        int k = 0;
        ArrayList<Integer> otherUsers = new ArrayList<>();
        int[] hash = lsh.hash(userHouseMatrix[currentUserIndex]);
        int currentUserBucket = hash[0];

        // We only care about the users that got into the same bucket
        // that the current user did. Thus, there is no need to keep hold of all the buckets
        ArrayList<Integer> hashes = new ArrayList<>();
        for (int i = 0; i < userHouseMatrix.length; i++) {
            if (i == currentUserIndex) continue;
            boolean[] tmpVector = userHouseMatrix[i];

            hash = lsh.hash(tmpVector);

            if (hash[0] == currentUserBucket) {
                otherUsers.add(userMap.get(i));
            }
        }

        return otherUsers;
    }


    private void fixIndices(MyVector vector, HashMap<Integer, Integer> houseMap,
                            HashMap<Integer, Double> currentUserMap) {
        try {
            for (Integer i : currentUserMap.keySet()) {
                vector.set(houseMap.get(i), currentUserMap.get(i));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Path("/getusershousesmin")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUsersHousesMin(String token) {
        Authenticator auth = new Authenticator(token, Constants.TYPE_RENTER);

        if (!auth.authenticate()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        int userId = auth.getId();

        Connection con = null;
        ResultSet rs = null;
        PreparedStatement pSt = null;
        try {
            con = DataSource.getInstance().getConnection();
            String query = "SELECT houseID, city, country, numRatings, minCost FROM houses WHERE ownerID = ?";
            pSt = con.prepareStatement(query);
            pSt.setInt(1, userId);
            rs = pSt.executeQuery();
            System.out.println("Executing query");
            ArrayList<HouseMinEntity> entities = HouseGetter.getHouseMinList(rs);
            System.out.println("Got results");
            Gson gson = new Gson();

            String jsonString = gson.toJson(entities);
            System.out.println("Returning: " + jsonString);
            return Response.ok(jsonString).build();
        } catch (IOException | SQLException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            ConnectionCloser.closeAll(con, pSt, rs);
        }
    }


    @Path("/gethouse/{houseId}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHousePost(@PathParam("houseId") int houseId,
                                 String token) {
        Authenticator auth = new Authenticator(token, Constants.TYPE_USER);
        if (!auth.authenticate()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        int id = auth.getId();

        Connection con = null;
        PreparedStatement pSt = null;
        try {
            con = DataSource.getInstance().getConnection();
            String insert = "INSERT INTO searches (userID, houseID)" +
                    "VALUES (?, ?)";
            pSt = con.prepareStatement(insert);
            pSt.setInt(1, id);
            pSt.setInt(2, houseId);
            pSt.execute();
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            ConnectionCloser.closeAll(con, pSt, null);
        }


        return getHouse(houseId);
    }

    @Path("/gethouse/{houseid}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHouse(@PathParam("houseid") int houseId) {
       Connection con = null,
               con2 = null;
       ResultSet rs = null,
               rs2 = null;
       try {
           // A list of all the start - end date pairs that are already booked
           ArrayList<Date[]> dateList = new ArrayList<>();
           Connection bookCon = null;
           ResultSet bookRs = null;
           try {
               bookCon = DataSource.getInstance().getConnection();
               String query = "SELECT dateFrom, dateTo FROM bookings WHERE houseID = ?";
               PreparedStatement pSt = bookCon.prepareStatement(query);
               pSt.setInt(1, houseId);
               bookRs = pSt.executeQuery();
               while (bookRs.next()) {
                   Date[] dates = new Date[2];
                   dates[0] = bookRs.getDate("dateFrom");
                   dates[1] = bookRs.getDate("dateTo");
                   dateList.add(dates);
               }
           } catch (IOException | SQLException e) {
               e.printStackTrace();
               return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
           } finally {
               ConnectionCloser.closeAll(bookCon, null, bookRs);
           }

           con = DataSource.getInstance().getConnection();
           System.out.println("HouseID = " + houseId);
           String query = "SELECT * FROM houses WHERE houseID = ? LIMIT 1";
           PreparedStatement pSt = con.prepareStatement(query);
           pSt.setInt(1, houseId);
           rs = pSt.executeQuery();
           HouseEntity house = new HouseEntity();
           if (rs.next()) {


               house.setHouseId(String.valueOf(rs.getInt("houseID")));
               house.setLatitude(rs.getFloat("latitude"));
               house.setLongitude(rs.getFloat("longitude"));
               house.setAddress(rs.getString("address"));
               house.setCountry(rs.getString("country"));
               house.setNumBeds(rs.getInt("numBeds"));
               house.setNumBaths(rs.getInt("numBaths"));
               house.setAccommodates(rs.getInt("accommodates"));
               house.setLivingRoom(rs.getBoolean("hasLivingRoom"));
               house.setSmoking(rs.getBoolean("smokingAllowed"));
               house.setPets(rs.getBoolean("petsAllowed"));
               house.setEvents(rs.getBoolean("eventsAllowed"));
               house.setWifi(rs.getBoolean("wifi"));
               house.setAirconditioning(rs.getBoolean("wifi"));
               house.setHeating(rs.getBoolean("heating"));
               house.setKitchen(rs.getBoolean("kitchen"));
               house.setTv(rs.getBoolean("tv"));
               house.setParking(rs.getBoolean("parking"));
               house.setElevator(rs.getBoolean("elevator"));
               house.setNumRatings(rs.getInt("numRatings"));

               house.setArea(rs.getFloat("area"));
               house.setDescription(rs.getString("description"));
               house.setMinDays(rs.getInt("minDays"));
               house.setInstructions(rs.getString("instructions"));
               house.setDateFrom(DateHelper.dateToString(rs.getDate("dateFrom")));
               house.setDateTo(DateHelper.dateToString(rs.getDate("dateTo")));
               house.setMinCost(rs.getFloat("minCost"));
               house.setCostPerPerson(rs.getFloat("costPerPerson"));
               house.setCostPerDay(rs.getFloat("costPerDay"));
               house.setOwnerId(rs.getInt("ownerID"));

               ArrayList<String> excludedDates = new ArrayList<>();

               for (Date[] date : dateList) {
                    List<LocalDate> localDates = new DateRange(date[0], date[1]).toList();
                    for (LocalDate d : localDates) {
                        excludedDates.add(DateHelper.dateToString(Date.valueOf(d)));
                    }
               }

               house.setExcludedDates(excludedDates);

               con2 = DataSource.getInstance().getConnection();

               query = "SELECT pictureURL FROM photographs WHERE houseID = ?";

               pSt = con2.prepareStatement(query);
               pSt.setInt(1, houseId);
               rs2 = pSt.executeQuery();
               while (rs2.next()) {
                   house.addPicture(FileHelper.getFileAsString(rs2.getString("pictureURL")));
               }

               ConnectionCloser.closeAll(con2, pSt, rs2);

               try (Connection userCon = DataSource.getInstance().getConnection()) {
                   query = "SELECT firstName, lastName from users WHERE userID = ? LIMIT 1";
                   PreparedStatement userPSt = userCon.prepareStatement(query);
                   userPSt.setInt(1, rs.getInt("ownerID"));
                   ResultSet userRs = userPSt.executeQuery();
                   if (userRs.next()) {
                       house.setOwnerName(userRs.getString("firstName") + " " + userRs.getString("lastName"));
                   }
               } catch (SQLException e) {
                   e.printStackTrace();
                   return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
               }

           }

           Gson gson = new Gson();
           String response = gson.toJson(house);
           return Response.ok(response).build();
       } catch (IOException | SQLException e) {
           e.printStackTrace();
           return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
       } finally {
           ConnectionCloser.closeAll(con, null, rs);
       }
    }

    @Path("/updatehouse/{houseId}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateHouse(@PathParam("houseId") int houseId,
                                String json) {
        Gson gson = new Gson();
        HouseUpdateEntity entity = gson.fromJson(json, HouseUpdateEntity.class);
        Authenticator auth = new Authenticator(entity.getToken(), Constants.TYPE_RENTER);
        if (!auth.authenticate()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        Connection con = null;
        PreparedStatement pSt = null;
        try {
            con = DataSource.getInstance().getConnection();
            String[] location = ReverseGeocoder.convert(entity.getHouse().getLatitude(), entity.getHouse().getLongitude());
            HouseEntity he = entity.getHouse();
            String update = "UPDATE houses SET " +
                    "latitude = ?, longitude = ?, city = ?, country = ?, numBeds = ?, numBaths = ?, " +
                    "accommodates = ?, hasLivingRoom = ?, smokingAllowed = ?, petsAllowed = ?, eventsAllowed = ?, " +
                    "wifi = ?, airconditioning = ?, heating = ?, kitchen = ?, tv = ?, parking = ?, elevator = ?, " +
                    "area = ?, description = ?, instructions = ?, minDays = ?, " +
                    "dateFrom = ?, dateTo = ?, minCost = ?, costPerPerson = ?, costPerDay = ? WHERE houseID = ?";
            pSt = con.prepareStatement(update);
            pSt.setFloat(1, he.getLatitude());
            pSt.setFloat(2, he.getLongitude());
            pSt.setString(3, he.getCity());
            pSt.setString(4, he.getCountry());
            pSt.setInt(5, he.getNumBeds());
            pSt.setInt(6, he.getNumBaths());
            pSt.setInt(7, he.getAccommodates());
            pSt.setBoolean(8, he.getLivingRoom());
            pSt.setBoolean(9, he.getSmoking());
            pSt.setBoolean(10, he.getPets());
            pSt.setBoolean(11, he.getEvents());
            pSt.setBoolean(12, he.getWifi());
            pSt.setBoolean(13, he.getAirconditioning());
            pSt.setBoolean(14, he.getHeating());
            pSt.setBoolean(15, he.getKitchen());
            pSt.setBoolean(16, he.getTv());
            pSt.setBoolean(17, he.getParking());
            pSt.setBoolean(18, he.getElevator());
            pSt.setFloat(19, he.getArea());
            pSt.setString(20, he.getDescription());
            pSt.setString(21, he.getInstructions());
            pSt.setFloat(22, he.getMinDays());
            pSt.setDate(23, DateHelper.stringToDate(he.getDateFrom()));
            pSt.setDate(24, DateHelper.stringToDate(he.getDateTo()));
            pSt.setFloat(25, he.getMinCost());
            pSt.setFloat(26, he.getCostPerPerson());
            pSt.setFloat(27, he.getCostPerDay());
            pSt.setInt(28, houseId);
            pSt.execute();

            return Response.ok().build();
        } catch (SQLException | IOException | ParseException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            ConnectionCloser.closeAll(con, pSt, null);
        }
    }


    @Path("/uploadphoto/{houseId}")
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadPicture(@FormDataParam("file") InputStream uploadedInputStream,
                                  @FormDataParam("file") FormDataContentDisposition fileDetails,
                                  @FormDataParam("token") String token,
                                  @PathParam("houseId") int houseID) {

        Authenticator auth = new Authenticator(token, Constants.TYPE_RENTER);
        if (!auth.authenticate()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        int id = auth.getId();
        Connection con = null;
        try {
            con = DataSource.getInstance().getConnection();
            String fileUrl = FileHelper.saveFile(uploadedInputStream, id, fileDetails,false);
            String insert = "INSERT INTO photographs (houseID, pictureURL, main)" +
                    "VALUES (" +
                    "?, ?, 0)";
            PreparedStatement pSt = con.prepareStatement(insert);
            pSt.setInt(1, houseID);
            pSt.setString(2, fileUrl);
            pSt.execute();
            return Response.ok().build();
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException sqle) {
                    sqle.printStackTrace();
                }
            }
        }
    }
}

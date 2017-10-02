package com.WAT.airbnb.rest.houses;

import com.WAT.airbnb.db.DataSource;
import com.WAT.airbnb.etc.Constants;
import com.WAT.airbnb.util.DateRange;
import com.WAT.airbnb.util.Vector;
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
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Date;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.*;

/**
 * Handles all house operations
 * Paths:
 * /register
 * /search
 * /getnumpages
 * /getpage/{pageNum}
 * /getpredicted
 * /getusershousesmin
 * /gethouse/{houseId} - Method POST
 * /gethouse/{houseId} - Method GET
 * /updatehouse/{houseId}
 * /uploadphoto/{houseId}
 * @author Kostas Kolivas
 * @version 0.85
 */
@Path("/house")
public class HouseControl {

    /**
     * Allows the user to register a new house
     * and upload exactly one photograph for it (the one shown in the thumbnail)
     */
    @Path("/register")
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response registerHouse(@FormDataParam("file") InputStream uploadedFileInputStream,
                                  @FormDataParam("file") FormDataContentDisposition fileDetails,
                                  @FormDataParam("token") String token,
                                  @FormDataParam("data") String jsonString) {
        // Utilize the Gson library to convert the JSON data to a POJO
        Gson gson = new Gson();
        HouseBean entity = gson.fromJson(jsonString, HouseBean.class);

        // Authenticate the provided token
        Authenticator auth = new Authenticator(token, Constants.TYPE_RENTER);
        if (!auth.authenticate()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        entity.setNumRatings(0);

        // Call the Google Maps API to get information on the provided latitude and longitude
        try {
            String[] addr = ReverseGeocoder.convert(entity.getLatitude(), entity.getLongitude());
            if (addr != null) {
                entity.setAddress(addr[Constants.ADDR_OFFS]);
                entity.setCountry(addr[Constants.COUNTRY_OFFS]);
                entity.setCity(addr[Constants.CITY_OFFS]);
            }
        } catch (Exception e) {
            e.printStackTrace();
            entity.setAddress(null);
            entity.setCountry(null);
            entity.setCity(null);
        }

        int id = auth.getId();

        PreparedStatement pSt = null;
        try (Connection con = DataSource.getInstance().getConnection()) {
            con.setAutoCommit(false);
            String insertSt = "INSERT INTO houses " +
                    "(ownerID, latitude, longitude, address, city, country, numBeds, numBaths, " +
                    "accommodates, hasLivingRoom, smokingAllowed, " +
                    "petsAllowed, eventsAllowed, wifi, " +
                    "airconditioning, heating, kitchen, tv, parking, elevator, area," +
                    "description, minDays, instructions, numRatings, dateFrom, dateTo, " +
                    "minCost, costPerPerson, costPerDay) " +
                    "VALUES" +
                    "(" +                    "   ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " +
                    "?, ?, ?, ?, ?, ?, ?)";
            pSt = con.prepareStatement(insertSt, Statement.RETURN_GENERATED_KEYS);
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
            pSt.setInt(25, 0); // number of ratings
            pSt.setDate(26, DateHelper.stringToDate(entity.getDateFrom())); // available from
            pSt.setDate(27, DateHelper.stringToDate(entity.getDateTo())); // available to
            pSt.setFloat(28, entity.getMinCost()); // minimum cost
            pSt.setFloat(29, entity.getCostPerPerson()); // cost per person
            pSt.setFloat(30, entity.getCostPerDay()); // cost per day
            pSt.executeUpdate();

            Integer insertedId = null;
            try (ResultSet generatedKeys = pSt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    insertedId = generatedKeys.getInt(1);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (insertedId == null) {
                throw new SQLException("No inserted id");
            }

            // Save the picture of the house
            String fileUrl = FileHelper.saveFile(uploadedFileInputStream, insertedId, fileDetails,
                    false);

            // Also create and save a thumbnail of it
            String thumbUrl = FileHelper.saveFileThumb(fileUrl, insertedId);

            // Insert both of these paths to the database
            insertSt = "INSERT INTO photographs (houseID, main, pictureURL, thumbURL)" +
                    "VALUES " +
                    "(?, ?, ?, ?)";
            pSt = con.prepareStatement(insertSt);
            pSt.setInt(1, insertedId);
            pSt.setBoolean(2, true);
            pSt.setString(3, fileUrl);
            pSt.setString(4, thumbUrl);
            pSt.executeUpdate();

            // Commit only after all actions have been successfully completed
            con.commit();

            return Response.ok().build();
        } catch (Exception sqle) {
            sqle.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            ConnectionCloser.getCloser()
                .closeStatement(pSt);
        }

    }

    /**
     * Searches for houses matching the given JSON
     * @return A list of (minified) houses along with their thumbnail picture.
     */
    @Path("/search")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response searchHouse(String json) {
        Gson gson = new Gson();
        HouseBean entity = gson.fromJson(json, HouseBean.class);
        Connection con = null;
        ResultSet rs = null;
        PreparedStatement pSt = null;
        try {
            con = DataSource.getInstance().getConnection();

            boolean[] hasArgs = new boolean[11];
            Arrays.fill(hasArgs, false);
            QueryBuilder queryBuilder = new QueryBuilder("SELECT houseID, city, country, " +
                    "numRatings, minCost FROM houses WHERE 1 = 1 ");
            if (entity.getCountry() != null) {
                queryBuilder.and("country = ?");
                hasArgs[0] = true;
            }
            if (entity.getNumBeds() != null) {
                queryBuilder.and("numBeds >= ?");
                hasArgs[1] = true;
            }
            if (entity.getNumBaths() != null) {
                queryBuilder.and("numBaths >= ?");
                hasArgs[2] = true;
            }
            if (entity.getAccommodates() != null) {
                queryBuilder.and("accommodates >= ?");
                hasArgs[3] = true;
            }
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
            }
            if (entity.getMinDays() != null) {
                queryBuilder.and("minDays >= ?");
                hasArgs[5] = true;
            }
            if (entity.getDateFrom() != null && entity.getDateTo() != null) {
                queryBuilder.and("dateFrom <= ?").and("dateTo <= ?");
                hasArgs[6] = true;
            } else if (entity.getDateFrom() != null) {
                queryBuilder.and("dateFrom <= ?");
                hasArgs[7] = true;
            } else if (entity.getDateTo() != null) {
                queryBuilder.and("dateTo <= ?").and("dateFrom <= NOW()");
                hasArgs[8] = true;
            }
            if (entity.getMinCost() != null) {
                queryBuilder.and("minCost >= ?");
                hasArgs[9] = true;
            }
            if (entity.getCostPerPerson() != null) {
                queryBuilder.and("costPerPerson >= ?");
                hasArgs[10] = true;
            }

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


            ArrayList<HouseMinBean> minEntities = new ArrayList<>();

            rs = pSt.executeQuery();
            while (rs.next()) {
                HouseMinBean minEntity = new HouseMinBean();
                minEntity.setHouseId(rs.getInt("houseId"));
                minEntity.setCity(rs.getString("city"));
                minEntity.setCountry(rs.getString("country"));
                minEntity.setNumRatings(rs.getInt("numRatings"));
                minEntity.setMinCost(rs.getInt("minCost"));

                Connection picCon = DataSource.getInstance().getConnection();
                String query = "SELECT thumbURL from photographs WHERE houseId = ? LIMIT 1";
                pSt = picCon.prepareStatement(query);
                pSt.setInt(1, minEntity.getHouseId());
                ResultSet picRs = pSt.executeQuery();
                if (picRs.next()) {
                    minEntity.setPicture(FileHelper.getFileAsString(picRs.getString("thumbURL")));
                }

                ConnectionCloser.getCloser()
                        .closeAll(picCon, pSt, picRs);

                minEntities.add(minEntity);
            }

            String response = gson.toJson(minEntities);
            return Response.ok(response).build();
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            ConnectionCloser.getCloser()
                    .closeAll(con, pSt, rs);
        }
    }

    /**
     * The size of each page is set to be 8 (defined in com.WAT.airbnb.etc.Constants$PAGE_SIZE)
     * @return The number of pages (Overall number of houses / PAGE_SIZE (=8)).
     */
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
            ConnectionCloser.getCloser().closeAll(con, st, rs);
        }
    }

    /**
     * @return A list of PAGE_SIZE (=8) minified houses
     */
    @Path("/getpage/{pagenum}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPage(@PathParam("pagenum") int pageNum) {
        Connection con = null;
        ResultSet rs = null;
        PreparedStatement pSt = null;
        ArrayList<HouseMinBean> entities = new ArrayList<>();
        try {

            con = DataSource.getInstance().getConnection();
            String query = "SELECT houseID, city, country, numRatings, minCost FROM houses " +
                    "WHERE dateTo > NOW() ORDER BY minCost ASC LIMIT ?, ?";
            pSt = con.prepareStatement(query);
            pSt.setInt(1, pageNum * Constants.PAGE_SIZE);
            pSt.setInt(2, Constants.PAGE_SIZE);
            rs = pSt.executeQuery();
            while (rs.next()) {
                HouseMinBean entity = new HouseMinBean();
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
                    ConnectionCloser.getCloser().closeAll(picCon, picpSt, picRs);
                }

            }

            Gson gson = new Gson();
            String response = gson.toJson(entities);
            return Response.ok(response).build();
        } catch (IOException | SQLException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            ConnectionCloser.getCloser().closeAll(con, pSt, rs);
        }
    }

    /**
     * Performs the NNCF algorithm to predict houses the user might be interested in
     * @return A list of 4 minified suggested houses
     */
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
        Connection con = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            con = DataSource.getInstance().getConnection();
            st = con.createStatement();

            // Create a user and house map
            String query = "SELECT DISTINCT(houseID) from comments";
            rs = st.executeQuery(query);

            // The houseMap object maps House IDs to Vector indices
            HashMap<Integer, Integer> houseMap = new HashMap<>();

            // The reverseHouseMap maps Vector indices to House IDs
            HashMap<Integer, Integer> reverseHouseMap = new HashMap<>();
            int houseCount;
            for (houseCount = 0; rs.next(); houseCount++) {
                houseMap.put(rs.getInt("houseID"), houseCount);
                reverseHouseMap.put(houseCount, rs.getInt("houseID"));
            }

            ConnectionCloser.getCloser()
                    .closeResultSet(rs);

            query = "SELECT DISTINCT(userID) FROM comments";
            rs = st.executeQuery(query);

            // The userMap maps User IDs to Matrix rows
            HashMap<Integer, Integer> userMap = new HashMap<>();

            // The reverseUserMap maps Matrix rows to User IDs
            HashMap<Integer, Integer> reverseUserMap = new HashMap<>();
            int userCount;
            for (userCount = 0; rs.next(); userCount++) {
                userMap.put(rs.getInt("userID"), userCount);
                reverseUserMap.put(userCount, rs.getInt("userID"));
            }

            // Check if there is enough information for
            query = "SELECT COUNT(*) AS c FROM comments WHERE userID = " + currentUser;
            rs = st.executeQuery(query);
            int count = rs.next() ? rs.getInt("c") : 0;
            rs.previous();
            if (rs.next() && count > 10) {
                // Predict houses based on the user's actual ratings

                // Create a boolean Matrix where each row corresponds to a user
                // and each column to a house (both are mapped above)
                // Cell[i][j] = 1 if user i has rated house j with a rating
                // of over 0.5 (normalized values [0, 1])
                boolean[][] userHouseMatrix = new boolean[userCount][houseCount];
                for (int i = 0; i < userCount; i++) {
                    Arrays.fill(userHouseMatrix[i], false);
                }

                // Fill the matrix with data
                query = "SELECT houseID, userID, rating FROM comments";
                rs = st.executeQuery(query);
                while (rs.next()) {
                    float rating = rs.getFloat("rating");
                    int row = userMap.get(rs.getInt("userID"));
                    int col = houseMap.get(rs.getInt("houseID"));
                    userHouseMatrix[row][col] = rating > 0.5;
                }


                // Calculate what other users hashed into the same bucket as
                // the current user
                ArrayList<Integer> otherUsers = getBucket(currentUser,
                        houseCount, userHouseMatrix, userMap, false);

                // Get the actual ratings from the other users
                QueryBuilder qBuilder = new QueryBuilder("SELECT userID, houseID, " +
                        "rating FROM comments WHERE userID = " + currentUser + " ");
                for (Integer user : otherUsers) {
                    if (user == null) continue; // There has been a case where a null value got
                                                // into the array list
                    qBuilder.or("userID = " + user);
                }

                ConnectionCloser.getCloser()
                        .closeResultSet(rs);

                rs = st.executeQuery(qBuilder.toString());

                // Build a ratings map that maps the users to another map that maps
                // the users' ratings to each houseID
                HashMap<Integer, HashMap<Integer, Double>> ratingsMap = new HashMap<>();
                int i;

                while (rs.next()) {
                    HashMap<Integer, Double> tmp;
                    int userId = rs.getInt("userID");
                    int houseId = rs.getInt("houseID");
                    double rating = rs.getDouble("rating");
                    if (ratingsMap.containsKey(userId)) {
                        tmp = ratingsMap.get(userId);
                    } else {
                        tmp = new HashMap<>();
                    }
                    tmp.put(houseId, rating);
                    ratingsMap.put(userId, tmp);
                }


                // Build the actual user matrix from the above data

                // Each user is represented by a com.WAT.airbnb.util.Vector object
                // that provides easy dot product and cosine similarity calculation
                com.WAT.airbnb.util.Vector[] userMatrix = new Vector[ratingsMap.size()];

                // The first user in the matrix is the user of interest (index 0)
                userMatrix[0] = new Vector(currentUser, houseCount, true);

                //
                HashMap<Integer, Double> currentUserMap = ratingsMap.get(currentUser);

                // See com.WAT.airbnb.rest.HouseControl$fixIndices on what this function does
                fixIndices(userMatrix[0], houseMap, currentUserMap);

                i = 1;

                // userIndexMap maps each user to a Matrix index
                HashMap<Integer, Integer> userIndexMap = new HashMap<>();
                for (Integer id : ratingsMap.keySet()) {
                    if (id == currentUser) continue;
                    userMatrix[i] = new Vector(id, houseCount, true);
                    userIndexMap.put(id, i);
                    currentUserMap = ratingsMap.get(id);
                    fixIndices(userMatrix[i++], houseMap, currentUserMap);
                }

                // Create the distance matrix and populate it
                // with the cosine similarities between the current user
                // and each other user
                Map<Integer, Double> distanceMap = new HashMap<>();
                for (i = 1; i < userMatrix.length; i++) {
                    double distance = userMatrix[0].cosineSim(userMatrix[i]);
                    distanceMap.put(userMatrix[i].getId(), distance);
                }

                // Transform the distanceMap map object to a Set and then a List
                // (used in sorting the distances)
                Set<Map.Entry<Integer, Double>> entries = distanceMap.entrySet();
                List<Map.Entry<Integer, Double>> entryList = new ArrayList<Map.Entry<Integer, Double>>(entries);

                // The com.WAT.airbnb.util.Vector class provides a
                // value comparator (descending order)
                entryList.sort(Vector.valueComparator);

                // Create a list of predicted houseIDs
                ArrayList<Tuple<Integer, Double>> predicted = new ArrayList<>();
                for (i = 0; i < Constants.MAX_NEIGHBOURS; i++) {
                    // For the top MAX_NEIGHBOURS in the sorted list, check if they have positively
                    // rated a house that the current user hasn't AND that the house is not already
                    // in the predicted list
                    // If both of the above conditions are true, add it to the predicted list
                    Vector tmpVector = userMatrix[userIndexMap.get(entryList.get(0).getKey())];
                    for (int j = 0; j < userMatrix[0].size(); j++) {
                        if (userMatrix[0].get(j) == 0d && tmpVector.get(j) >= 0.5
                                && !predicted.contains(
                                new Tuple<Integer, Double>(reverseHouseMap.get(j), null))) {
                            predicted.add(new Tuple<>(reverseHouseMap.get(j), tmpVector.get(j)));
                        }
                    }
                }

                Comparator<Tuple<Integer, Double>> comp = (t1, t2) -> {
                    if (t1.right > t2.right)
                        return -1;
                    else if (t1.right < t2.right)
                        return 1;
                    return 0;
                };

                // After finding all the matches for the top MAX_NEIGHBOURS users
                // we sort the predicted list and pick the K highest rated houses
                predicted.sort(comp);

                Connection rCon = null;
                PreparedStatement rSt = null;
                ResultSet rRs = null;
                try {
                    rCon = DataSource.getInstance().getConnection();
                    ArrayList<HouseMinBean> entities = new ArrayList<>();
                    for (i = 0; i < Constants.K; i++) {
                        query = "SELECT houseID, city, country, numRatings, minCost FROM " +
                                "houses WHERE houseID = ?";
                        rSt = rCon.prepareStatement(query);
                        rSt.setInt(1, predicted.get(i).left);
                        rRs = rSt.executeQuery();
                        while (rRs.next()) {
                            HouseMinBean entity = new HouseMinBean();
                            entity.setHouseId(rRs.getInt("houseID"));
                            entity.setCity(rRs.getString("city"));
                            entity.setCountry(rRs.getString("country"));
                            entity.setNumRatings(rRs.getInt("numRatings"));
                            entity.setMinCost(rRs.getFloat("minCost"));

                            query = "SELECT thumbURL FROM photographs " +
                                    "WHERE houseID = " + rRs.getInt("houseID") + " LIMIT 1";
                            rSt = rCon.prepareStatement(query);
                            ResultSet tmpRs = rSt.executeQuery();
                            if (tmpRs.next()) {
                                entity.setPicture(
                                        FileHelper.getFileAsString(tmpRs.getString("thumbURL")
                                        ));
                            }

                            entities.add(entity);
                        }

                        ConnectionCloser.getCloser().closeAll(null, rSt, rRs);
                    }

                    Gson gson = new Gson();
                    String response = gson.toJson(entities);
                    return Response.ok(response).build();
                } catch (SQLException | IOException e) {
                    e.printStackTrace();
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
                } finally {
                    ConnectionCloser.getCloser()
                            .closeAll(rCon, rSt, rRs);
                }
            } else {

                ResultSet tmpRs;
                query = "SELECT COUNT(*) AS c FROM searches WHERE userID = " + currentUser;
                tmpRs = st.executeQuery(query);
                if (tmpRs.next()) {
                    count = tmpRs.getInt("c");
                }

                ConnectionCloser.getCloser()
                        .closeResultSet(tmpRs);

                // To get at least some relevancy in the results for low rating count users
                // we require them to have looked at at least 5 houses
                if (count >= 5) {

                    boolean[][] userHouseMatrix = new boolean[userCount][houseCount];
                    for (int i = 0; i < userCount; i++) {
                        Arrays.fill(userHouseMatrix[i], false);
                    }

                    query = "SELECT houseID, userID, rating FROM comments";
                    rs = st.executeQuery(query);
                    int currentUserIndex;
                    while (rs.next()) {
                        double rating = rs.getDouble("rating");
                        int row = userMap.get(rs.getInt("userID"));
                        int col = houseMap.get(rs.getInt("houseID"));
                        userHouseMatrix[row][col] = rating >= 0.5;
                    }

                    ConnectionCloser.getCloser()
                            .closeResultSet(rs);

                    {
                        int index = userMap.get(currentUser);


                        // Get the current user's searches
                        query = "SELECT houseID FROM searches WHERE userID = " + currentUser;
                        rs = st.executeQuery(query);
                        while (rs.next()) {
                            userHouseMatrix[index][houseMap.get(rs.getInt("houseID"))] = true;
                        }
                    }


                    ConnectionCloser.getCloser()
                            .closeResultSet(rs);

                    // Get the other users that also hashed to the same bucket
                    // as the current user
                    ArrayList<Integer> otherUsers =
                            getBucket(currentUser, houseCount, userHouseMatrix,
                                    reverseUserMap, true);

                    com.WAT.airbnb.util.Vector[] userMatrix = new Vector[otherUsers.size() + 1];

                    // Populate the user house matrix
                    userMatrix[0] = new Vector(userHouseMatrix[userCount - 1], currentUser);
                    for (int i = 1; i < otherUsers.size() + 1; i++) {
                        int id = otherUsers.get(i - 1);
                        int index = userMap.get(id);
                        boolean[] v = userHouseMatrix[index];
                        userMatrix[i] = new Vector(v, id);
                    }


                    // Calculate the user of interest's distance with every other user
                    // that hashed to the same bucket
                    Map<Integer, Double> distanceMap = new HashMap<>();

                    for (int i = 1; i < userMatrix.length; i++) {
                        double distance = userMatrix[0].cosineSim(userMatrix[i], 1);
                        distanceMap.put(i - 1, distance);
                    }

                    // Convert the distance map to a Set and then a List in order
                    // to properly sort it
                    Set<Map.Entry<Integer, Double>> entries = distanceMap.entrySet();
                    List<Map.Entry<Integer, Double>> entryList =
                            new ArrayList<>(entries);

                    Comparator<Map.Entry<Integer, Double>> comp = (e1, e2) -> {
                        if (e1.getValue() > e2.getValue()) {
                            return -1;
                        } else if (e1.getValue() < e2.getValue()) {
                            return 1;
                        }
                        return 0;
                    };

                    entryList.sort(comp);

                    ArrayList<Tuple<Integer, Boolean>> predicted = new ArrayList<>();

                    for (int i = 0; i < (Constants.MAX_NEIGHBOURS > userMatrix.length ?
                            userMatrix.length : Constants.MAX_NEIGHBOURS); i++) {
                        Vector tmpVector = userMatrix[entryList.get(i).getKey()];

                        for (int j = 0; j < userMatrix[0].size(); j++) {
                            Tuple<Integer, Boolean> tmp = new Tuple<>(reverseHouseMap.get(j),
                                    tmpVector.getB(j));

                            if (!userMatrix[0].getB(j) && tmpVector.getB(j)
                                    && !predicted.contains(tmp)) {
                                predicted.add(tmp);
                            }
                        }
                    }

                    Connection rCon = null;
                    PreparedStatement rSt = null;
                    ResultSet rRs = null;
                    try {
                        rCon = DataSource.getInstance().getConnection();
                        ArrayList<HouseMinBean> entities = new ArrayList<>();
                        // Pick the top K (=4) matching houses and send them to the client
                        // in JSON format
                        for (int i = 0; i < Constants.K; i++) {
                            query = "SELECT houseID, city, country, numRatings, minCost " +
                                    "FROM houses WHERE houseID = ?";
                            rSt = rCon.prepareStatement(query);
                            rSt.setInt(1, predicted.get(i).left);
                            rRs = rSt.executeQuery();
                            while (rRs.next()) {
                                HouseMinBean entity = new HouseMinBean();
                                entity.setHouseId(rRs.getInt("houseID"));
                                entity.setCity(rRs.getString("city"));
                                entity.setCountry(rRs.getString("country"));
                                entity.setNumRatings(rRs.getInt("numRatings"));
                                entity.setMinCost(rRs.getFloat("minCost"));

                                query = "SELECT thumbURL FROM photographs " +
                                        "WHERE houseID = " + rRs.getInt("houseID") + " LIMIT 1";
                                rSt = rCon.prepareStatement(query);
                                tmpRs = rSt.executeQuery();
                                if (tmpRs.next()) {
                                    entity.setPicture(FileHelper.getFileAsString(
                                            tmpRs.getString("thumbURL")));
                                }

                                entities.add(entity);
                            }

                            ConnectionCloser.getCloser()
                                    .closeResultSet(rRs)
                                    .closeStatement(rSt);
                        }

                        Gson gson = new Gson();
                        String response = gson.toJson(entities);
                        return Response.ok(response).build();
                    } catch (SQLException | IOException e) {
                        e.printStackTrace();
                        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
                    } finally {
                        ConnectionCloser.getCloser().closeAll(rCon, rSt, rRs);
                    }
                } else {
                    return Response
                            .ok(Constants.NOT_ENOUGH_DATA_RESP)
                            .build();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .build();
        } finally {
            ConnectionCloser.getCloser().closeAll(con, st, rs);
        }
    }

    /**
     * Utilises TDEBatty's java-LSH library to hash the given boolean array
     * @param currentUser The ID of the current user
     * @param houseCount The number of rated houses
     * @param userHouseMatrix The actual matrix whose rows will be hashed
     * @param userMap A HashMap object that maps matrix indices to user IDs
     * @param sparse Specifies whether the given matrix is sparse, in which case, the number of
     *               buckets is smaller
     * @return A list of userIDs that hashed into the same bucket as the user of interest
     */
    private ArrayList<Integer> getBucket(int currentUser, int houseCount,
                                         boolean[][] userHouseMatrix,
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


    /**
     * Helper function for the NNCF algorithm
     * Sets the com.WAT.airbnb.util.Vector object's
     * values properly
     * @param vector The vector to be altered
     * @param houseMap The houseMap according to which the indices are determined
     * @param currentUserMap The current user's ratings for each rated house
     */
    private void fixIndices(Vector vector, HashMap<Integer, Integer> houseMap,
                            HashMap<Integer, Double> currentUserMap) {
        try {
            for (Integer i : currentUserMap.keySet()) {
                vector.set(houseMap.get(i), currentUserMap.get(i));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @return The list of minified houses that the user has put up for renting
     */
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
            String query = "SELECT houseID, city, country, numRatings, minCost " +
                    "FROM houses " +
                    "WHERE ownerID = ?";
            pSt = con.prepareStatement(query);
            pSt.setInt(1, userId);
            rs = pSt.executeQuery();
            ArrayList<HouseMinBean> entities = HouseGetter.getHouseMinList(rs);
            Gson gson = new Gson();

            String jsonString = gson.toJson(entities);
            return Response.ok(jsonString).build();
        } catch (IOException | SQLException e) {
            e.printStackTrace();
            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .build();
        } finally {
            ConnectionCloser.getCloser().closeAll(con, pSt, rs);
        }
    }

    /**
     * If we don't have enough data for a user's preferences (comments)
     * we keep track of the fact that they looked at the house in the database (airbnb.searches)
     * We differentiate between the /gethouse/{houseId} paths by the method used (POST - GET)
     * @return All of the specified house's information
     */
    @Path("/gethouse/{houseId}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHousePost(@PathParam("houseId") int houseId,
                                 String token) {
        Authenticator auth = new Authenticator(token, Constants.TYPE_USER);

        if (!auth.authenticate()) {
            // We do not return a 401 on authentication failure because the user
            // should be able to see the house even if their token is not valid
            return getHouse(houseId);
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
            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .build();
        } finally {
            ConnectionCloser.getCloser()
                    .closeAll(con, pSt, null);
        }


        return getHouse(houseId);
    }

    /**
     * @return The specified house's information and a list of dates that the house is not available
     */
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
               ConnectionCloser.getCloser().closeAll(bookCon, null, bookRs);
           }

           con = DataSource.getInstance().getConnection();
           String query = "SELECT * FROM houses WHERE houseID = ? LIMIT 1";
           PreparedStatement pSt = con.prepareStatement(query);
           pSt.setInt(1, houseId);
           rs = pSt.executeQuery();
           HouseBean house = new HouseBean();
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

               ConnectionCloser.getCloser().closeAll(con2, pSt, rs2);

               try (Connection userCon = DataSource.getInstance().getConnection()) {
                   query = "SELECT firstName, lastName from users WHERE userID = ? LIMIT 1";
                   PreparedStatement userPSt = userCon.prepareStatement(query);
                   userPSt.setInt(1, rs.getInt("ownerID"));
                   ResultSet userRs = userPSt.executeQuery();
                   if (userRs.next()) {
                       house.setOwnerName(userRs.getString("firstName") + " "
                               + userRs.getString("lastName"));
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
           ConnectionCloser.getCloser()
                   .closeAll(con, null, rs);
       }
    }

    /**
     * Updates the specified house with according to the JSON input
     */
    @Path("/updatehouse/{houseId}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateHouse(@PathParam("houseId") int houseId,
                                String json) {
        Gson gson = new Gson();
        HouseUpdateBean entity = gson.fromJson(json, HouseUpdateBean.class);
        Authenticator auth = new Authenticator(entity.getToken(), Constants.TYPE_RENTER);
        if (!auth.authenticate()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        Connection con = null;
        PreparedStatement pSt = null;
        try {
            con = DataSource.getInstance().getConnection();
            String[] location = ReverseGeocoder.convert(entity.getHouse().getLatitude(), entity.getHouse().getLongitude());
            HouseBean he = entity.getHouse();
            String update = "UPDATE houses " +
                    "SET latitude = ?, longitude = ?, city = ?, country = ?, numBeds = ?, " +
                    "numBaths = ?, accommodates = ?, hasLivingRoom = ?, smokingAllowed = ?, " +
                    "petsAllowed = ?, eventsAllowed = ?, wifi = ?, airconditioning = ?, " +
                    "heating = ?, kitchen = ?, tv = ?, parking = ?, elevator = ?, " +
                    "area = ?, description = ?, instructions = ?, minDays = ?, " +
                    "dateFrom = ?, dateTo = ?, minCost = ?, costPerPerson = ?, costPerDay = ? " +
                    "WHERE houseID = ?";

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
            ConnectionCloser.getCloser().closeAll(con, pSt, null);
        }
    }

    /**
     * Allows the user to upload more photos for their house
     */
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
        PreparedStatement pSt = null;
        try {
            con = DataSource.getInstance().getConnection();
            String fileUrl = FileHelper.saveFile(uploadedInputStream, id, fileDetails,false);
            String thumbUrl = FileHelper.saveFileThumb(fileUrl, id);
            String insert = "INSERT INTO photographs (houseID, pictureURL, thumbURL, main)" +
                    "VALUES (" +
                    "?, ?, ?, 0)";
            pSt = con.prepareStatement(insert);
            pSt.setInt(1, houseID);
            pSt.setString(2, fileUrl);
            pSt.setString(3, thumbUrl);
            pSt.execute();
            return Response.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            ConnectionCloser.getCloser()
                    .closeConnection(con)
                    .closeStatement(pSt);
        }
    }
}

//package com.WAT.airbnb.rest;
//
//import com.WAT.airbnb.db.DataSource;
//import com.WAT.airbnb.etc.Constants;
//import com.WAT.airbnb.rest.entities.HouseMinEntity;
//import com.WAT.airbnb.util.DateRange;
//import com.WAT.airbnb.util.MyVector;
//import com.WAT.airbnb.util.QueryBuilder;
//import com.WAT.airbnb.util.Tuple;
//import com.WAT.airbnb.util.helpers.ConnectionCloser;
//import com.WAT.airbnb.util.helpers.DateHelper;
//import com.WAT.airbnb.util.helpers.FileHelper;
//import com.google.gson.Gson;
//import info.debatty.java.lsh.LSHMinHash;
//
//import javax.ws.rs.*;
//import javax.ws.rs.core.GenericEntity;
//import javax.ws.rs.core.Response;
//import java.io.*;
//import java.sql.*;
//import java.sql.Date;
//import java.text.ParseException;
//import java.text.SimpleDateFormat;
//import java.time.LocalDate;
//import java.util.*;
//import java.util.concurrent.ThreadLocalRandom;
//
//@Path("/test")
//public class Test {
//    @Path("/image")
//    @GET
//    @Produces("image/jpg")
//    public Response getImage() {
//        try {
//            List<FileInputStream> list = new ArrayList<>();
//            for (int i = 1; i < 4; i++) {
//                list.add(new FileInputStream(new File(Constants.DIR + "/img/houses/" + i + ".jpg")));
//            }
//            GenericEntity<List<FileInputStream>> entity = new GenericEntity<List<FileInputStream>>(list) {};
//            return Response.ok(entity).build();
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
//        }
//    }
//
//    @Path("/daterange")
//    @GET
//    public Response dateRange(@QueryParam("f") String from,
//                              @QueryParam("t") String to) {
//        try {
//            DateRange range = new DateRange(
//                    DateHelper.stringToDate(from),
//                    DateHelper.stringToDate(to)
//            );
//
//            return Response.ok(range.toList().size()).build();
//        } catch (ParseException e) {
//            e.printStackTrace();
//            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
//        }
//    }
//
//    @Path("/datetester")
//    @POST
//    public Response dateTester(String dateStr) {
//        Connection con = null;
//        Statement st = null;
//        ResultSet rs = null;
//        try {
//            con = DataSource.getInstance().getConnection();
//            String query = "SELECT lastUpdated FROM houses LIMIT 1";
//            st = con.createStatement();
//            rs = st.executeQuery(query);
//            if (rs.next()) {
//                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
//                java.util.Date date = sdf.parse(dateStr);
//                Date sqlDate = new java.sql.Date(date.getTime());
//                System.out.println(sqlDate.compareTo(rs.getTimestamp("lastUpdated")));
//            }
//            return Response.ok().build();
//        } catch (SQLException | IOException | ParseException e) {
//            e.printStackTrace();
//            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
//        } finally {
//            ConnectionCloser.closeAll(con, st, rs);
//        }
//    }
//
//    @Path("/remover")
//    @GET
//    public Response remover() {
//        ArrayList<LocalDate> dates = new ArrayList<>();
//        for (int i = 0; i < 5; i++) {
//            LocalDate d = LocalDate.now();
//            dates.add(d);
//        }
//
//        dates.removeAll(Collections.singleton(LocalDate.now()));
//        System.out.println(dates.size());
//        return Response.ok().build();
//    }
//
//    @Path("/testcontains")
//    @GET
//    public Response testContains() {
//        ArrayList<String> list = new ArrayList<>();
//        list.add("Hello");
//        list.add("World");
//        System.out.println(list.contains("Hello"));
//        return Response.ok().build();
//    }
//
//    @Path("/fixnullhouses")
//    @GET
//    public Response fixNullHouses() {
//        Connection getCon = null;
//        ResultSet rs = null;
//        try {
//            getCon = DataSource.getInstance().getConnection();
//            String query = "SELECT houseID FROM houses WHERE tv IS NULL";
//            Statement st = getCon.createStatement();
//            rs = st.executeQuery(query);
//            ArrayList<Integer> ids = new ArrayList<>();
//            while (rs.next()) {
//                ids.add(rs.getInt("houseID"));
//            }
//
//            for (Integer id : ids) {
//                Connection con = null;
//                try {
//                    con = DataSource.getInstance().getConnection();
//                    String update = "UPDATE houses SET tv = ? WHERE houseID = ? LIMIT 1";
//                    PreparedStatement pSt = con.prepareStatement(update);
//                    pSt.setBoolean(1, getRandomBoolean());
//                    pSt.setInt(2, id);
//                    pSt.execute();
//                } catch (SQLException | IOException e) {
//                    e.printStackTrace();
//                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
//                } finally {
//                    if (con != null) {
//                        try {
//                            con.close();
//                        } catch (SQLException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }
//            }
//
//            return Response.ok().build();
//        } catch (IOException | SQLException e) {
//            e.printStackTrace();
//            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
//        } finally {
//            if (getCon != null) {
//                try {
//                    getCon.close();
//                } catch (SQLException e) {
//                    e.printStackTrace();
//                }
//            }
//
//            if (rs != null) {
//                try {
//                    rs.close();
//                } catch (SQLException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//    }
//
//    @Path("/fixImgs")
//    @GET
//    public Response fixImages() {
//        File imgFolder = new File(Constants.DIR + "/img/houses");
//        File[] listOfFiles = imgFolder.listFiles();
//        try {
//            for (int i = 0; i < listOfFiles.length; i++) {
//                if (listOfFiles[i].isFile()) {
//                    FileHelper.saveFileThumb(listOfFiles[i].getPath(), false);
//                }
//            }
//            return Response.ok().build();
//        } catch (Exception e) {
//            e.printStackTrace();
//            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
//        }
//    }
//
//    @Path("/testlsh")
//    @GET
//    public Response testLsh(@QueryParam("id") int currentUser) {
//        long start = System.currentTimeMillis();
//        Connection con = null;
//        Statement st = null;
//        ResultSet rs = null;
//        try {
//            con = DataSource.getInstance().getConnection();
//            st = con.createStatement();
//
//            // Create a user and house map
//            // (houseID => matrix index)
//            String query = "SELECT DISTINCT(houseID) from comments";
//            rs = st.executeQuery(query);
//
//
//            HashMap<Integer, Integer> houseMap = new HashMap<>();
//            HashMap<Integer, Integer> reverseHouseMap = new HashMap<>();
//            int houseCount;
//            for (houseCount = 0; rs.next(); houseCount++) {
//                houseMap.put(rs.getInt("houseID"), houseCount);
//                reverseHouseMap.put(houseCount, rs.getInt("houseID"));
//            }
//
//            try {
//                rs.close();
//            } catch (SQLException e) {
//                e.printStackTrace();
//            }
//
//            query = "SELECT DISTINCT(userID) FROM comments";
//            rs = st.executeQuery(query);
//            HashMap<Integer, Integer> userMap = new HashMap<>();
//            HashMap<Integer, Integer> reverseUserMap = new HashMap<>();
//            int userCount;
//            for (userCount = 0; rs.next(); userCount++) {
//                userMap.put(rs.getInt("userID"), userCount);
//                reverseUserMap.put(userCount, rs.getInt("userID"));
//            }
//
//
//
//            query = "SELECT COUNT(*) AS c FROM comments WHERE userID = " + currentUser;
//            rs = st.executeQuery(query);
//            if (rs.next()) {
//                int count = rs.getInt("c");
//                if (count > 10) {
//                    // get normally
//
//                    boolean[][] userHouseMatrix = new boolean[userCount][houseCount];
//                    for (int i = 0; i < userCount; i++) {
//                        for (int j = 0; j < houseCount; j++) {
//                            userHouseMatrix[i][j] = false;
//                        }
//                    }
//
//                    query = "SELECT houseID, userID, rating FROM comments";
//                    rs = st.executeQuery(query);
//                    int currentUserIndex;
//                    while (rs.next()) {
//                        float rating = rs.getFloat("rating");
//                        if (rating >= 0.5) {
//                            userHouseMatrix[userMap.get(rs.getInt("userID"))][houseMap.get(rs.getInt("houseID"))] = true;
//                        }
//                    }
//
//
//                    ArrayList<Integer> otherUsers = getBucket(currentUser, houseCount, userHouseMatrix, userMap, false);
//
//                    QueryBuilder qBuilder = new QueryBuilder("SELECT userID, houseID, rating FROM comments " +
//                            "WHERE userID = " + currentUser + " ");
//                    for (Integer user : otherUsers) {
//                        if (user == null) continue; // There has been a case where a null value got into the array list
//                        qBuilder.or("userID = " + user);
//                    }
//
//                    try {
//                        rs.close();
//                    } catch (SQLException e) {
//                        e.printStackTrace();
//                    }
//
//                    System.out.println("Executing query : " + qBuilder.toString());
//                    rs = st.executeQuery(qBuilder.toString());
//
//                    HashMap<Integer, HashMap<Integer, Double>> ratingsMap = new HashMap<>();
//                    // houseMap = new HashMap<>();
//                    int i = 0;
//                    ArrayList<Integer> ids = new ArrayList<>();
//
//                    while (rs.next()) {
//                        HashMap<Integer, Double> tmp;
//                        if (ratingsMap.containsKey(rs.getInt("userID"))) {
//                            tmp = ratingsMap.get(rs.getInt("userID"));
//                        } else {
//                            tmp = new HashMap<>();
//                        }
//                        tmp.put(rs.getInt("houseID"), rs.getDouble("rating"));
//                        ratingsMap.put(rs.getInt("userID"), tmp);
//                    }
//
//
//                    MyVector[] userMatrix = new MyVector[ratingsMap.size()];
//
//                    userMatrix[0] = new MyVector(currentUser, houseCount, true);
//
//                    HashMap<Integer, Double> currentUserMap = ratingsMap.get(currentUser);
//
//                    fixIndices(userMatrix[0], houseMap, currentUserMap);
//
//                    i = 1;
//                    HashMap<Integer, Integer> userIndexMap = new HashMap<>();
//                    for (Integer id : ratingsMap.keySet()) {
//                        if (id == currentUser) continue;
//                        userMatrix[i] = new MyVector(id, houseCount, true);
//                        userIndexMap.put(id, i);
//                        currentUserMap = ratingsMap.get(id);
//                        fixIndices(userMatrix[i++], houseMap, currentUserMap);
//                    }
//
//                    Map<Integer, Double> distanceMatrix = new HashMap<>();
//                    for (i = 1; i < userMatrix.length; i++) {
//                        double distance = userMatrix[0].cosineSim(userMatrix[i]);
//                        distanceMatrix.put(userMatrix[i].getId(), distance);
//                    }
//
//
//                    Set<Map.Entry<Integer, Double>> entries = distanceMatrix.entrySet();
//                    List<Map.Entry<Integer, Double>> entryList = new ArrayList<Map.Entry<Integer, Double>>(entries);
//
//                    entryList.sort(MyVector.valueComparator);
//
//
//                    ArrayList<Tuple<Integer, Double>> predicted = new ArrayList<>();
//                    for (i = 0; i < Constants.MAX_NEIGHBOURS; i++) {
//                        MyVector tmpVector = userMatrix[userIndexMap.get(entryList.get(0).getKey())];
//                        for (int j = 0; j < userMatrix[0].size(); j++) {
//                            if (userMatrix[0].get(j) == 0d && tmpVector.get(j) != 0d
//                                    && !predicted.contains(
//                                            new Tuple<Integer, Double>(reverseHouseMap.get(j), null))) {
//                                predicted.add(new Tuple<Integer, Double>(reverseHouseMap.get(j), tmpVector.get(j)));
//                            }
//                        }
//                    }
//
//                    Comparator<Tuple<Integer, Double>> comp = new Comparator<Tuple<Integer, Double>>() {
//                        @Override
//                        public int compare(Tuple<Integer, Double> t1, Tuple<Integer, Double> t2) {
//                            if (t1.right > t2.right)
//                                return -1;
//                            else if (t1.right < t2.right)
//                                return 1;
//                            return 0;
//                        }
//                    };
//
//                    predicted.sort(comp);
//
//                    Connection rCon = null;
//                    PreparedStatement rSt = null;
//                    ResultSet rRs = null;
//                    try {
//                        rCon = DataSource.getInstance().getConnection();
//                        ArrayList<HouseMinEntity> entities = new ArrayList<>();
//                        for (i = 0; i < Constants.K; i++) {
//                            query = "SELECT houseID, city, country, rating, numRatings, minCost FROM houses WHERE" +
//                                    " houseID = ?";
//                            rSt = rCon.prepareStatement(query);
//                            rSt.setInt(1, predicted.get(i).left);
//                            rRs = rSt.executeQuery();
//                            while (rRs.next()) {
//                                HouseMinEntity entity = new HouseMinEntity();
//                                entity.setHouseId(rRs.getInt("houseID"));
//                                entity.setCity(rRs.getString("city"));
//                                entity.setCountry(rRs.getString("country"));
//                                entity.setRating(rRs.getFloat("rating"));
//                                entity.setNumRatings(rRs.getInt("numRatings"));
//                                entity.setMinCost(rRs.getFloat("minCost"));
//
//                                query = "SELECT thumbURL FROM airbnb_t.photographs " +
//                                        "WHERE houseID = " + rRs.getInt("houseID") + " LIMIT 1";
//                                rSt = rCon.prepareStatement(query);
//                                ResultSet tmpRs = rSt.executeQuery();
//                                if (tmpRs.next()) {
//                                    entity.setPicture(FileHelper.getFileAsString(tmpRs.getString("thumbURL")));
//                                }
//
//                                entities.add(entity);
//                            }
//
//                            ConnectionCloser.closeAll(null, rSt, rRs);
//                        }
//
//                        Gson gson = new Gson();
//                        String response = gson.toJson(entities);
//                        return Response.ok(response).build();
//                    } catch (SQLException | IOException e) {
//                        e.printStackTrace();
//                        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
//                    } finally {
//                        ConnectionCloser.closeAll(rCon, rSt, rRs);
//                    }
////
//                } else {
//                    query = "SELECT houseID FROM searches WHERE userID = " + currentUser;
//                    ResultSet tmpRs = st.executeQuery(query);
//
//                    // If there user hasn't rated any houses
//                    // then the user count hasn't included them
//                    if (count == 0) {
//                        userMap.put(currentUser, ++userCount);
//                    }
//
//                    try {
//                        rs.close();
//                    } catch (SQLException e) {
//                        e.printStackTrace();
//                    }
//                    boolean[][] userHouseMatrix = new boolean[userCount][houseCount];
//                    for (int i = 0; i < userCount; i++)
//                        for (int j = 0; j < houseCount; j++)
//                            userHouseMatrix[i][j] = false;
//
//                    query = "SELECT houseID, userID, rating FROM comments";
//                    rs = st.executeQuery(query);
//                    int currentUserIndex;
//                    while (rs.next())
//                        if (rs.getDouble("rating") >= 0.5)
//                            userHouseMatrix
//                                    [userMap.get(rs.getInt("userID"))]
//                                    [houseMap.get(rs.getInt("houseID"))] = true;
//
//                    int index;
//                    if (count == 0)
//                        index = userCount - 1;
//                    else
//                        index = userMap.get(currentUser);
//
//                    while (rs.next())
//                        userHouseMatrix[index][houseMap.get(rs.getInt("houseID"))] = true;
//
//                    try {
//                        rs.close();
//                    } catch (SQLException e) {
//                        e.printStackTrace();
//                    }
//
//                    ArrayList<Integer> otherUsers = getBucket(currentUser, houseCount, userHouseMatrix, reverseUserMap, true);
//
//                    MyVector[] userMatrix = new MyVector[otherUsers.size() + 1];
//
//                    userMatrix[0] = new MyVector(userHouseMatrix[userCount - 1], currentUser);
//                    System.out.println("Other users' size = " + otherUsers.size());
//                    for (int i = 1; i < otherUsers.size() + 1; i++) {
//                        userMatrix[i] =
//                                new MyVector(userHouseMatrix[userMap.get(otherUsers.get(i - 1))], otherUsers.get(i - 1));
//                    }
//
//
//                    Map<Integer, Double> distanceMatrix = new HashMap<>();
//                    System.out.println("userMatrix length = " + userMatrix.length);
//                    for (int i = 0; i < userMatrix.length; i++) {
//                        if (userMatrix[i] == null) {
//                            System.out.println("UserMatrix @ index " + i + " is null");
//                        }
//                    }
//                    for (int i = 1; i < userMatrix.length; i++) {
//                        double distance = userMatrix[0].cosineSim(userMatrix[i], 1);
//                        distanceMatrix.put(i - 1, distance);
//                    }
//
//
//                    Set<Map.Entry<Integer, Double>> entries = distanceMatrix.entrySet();
//                    List<Map.Entry<Integer, Double>> entryList =
//                            new ArrayList<>(entries);
//
//                    Comparator<Map.Entry<Integer, Double>> comp =
//                            new Comparator<Map.Entry<Integer, Double>>() {
//                        @Override
//                        public int compare(Map.Entry<Integer, Double> e1,
//                                           Map.Entry<Integer, Double> e2) {
//                            if (e1.getValue() > e2.getValue()) {
//                                return -1;
//                            } else if (e1.getValue() < e2.getValue()) {
//                                return 1;
//                            }
//                            return 0;
//                        }
//                    };
//
//                    entryList.sort(comp);
//
//                    ArrayList<Tuple<Integer, Boolean>> predicted = new ArrayList<>();
//
//                    for (int i = 0; i < (Constants.MAX_NEIGHBOURS > userMatrix.length ?
//                            userMatrix.length : Constants.MAX_NEIGHBOURS); i++) {
//                        MyVector tmpVector = userMatrix[entryList.get(i).getKey()];
//                        if (tmpVector == null) {
//                            System.out.println("tmpVector is null");
//                        }
//                        for (int j = 0; j < userMatrix[0].size(); j++) {
//                            if (userMatrix[0].get(j, 0) == null) {
//                                System.out.println("NULL");
//                            }
//                            if (!userMatrix[0].get(j, 0) && tmpVector.get(j, 0)
//                                    && !predicted.contains(
//                                    new Tuple<Integer, Boolean>(reverseHouseMap.get(j), null))) {
//                                predicted.add(new Tuple<Integer, Boolean>(reverseHouseMap.get(j), tmpVector.get(j, 0)));
//                            }
//                        }
//                    }
//
//                    Connection rCon = null;
//                    PreparedStatement rSt = null;
//                    ResultSet rRs = null;
//                    try {
//                        rCon = DataSource.getInstance().getConnection();
//                        ArrayList<HouseMinEntity> entities = new ArrayList<>();
//                        for (int i = 0; i < Constants.K; i++) {
//                            query = "SELECT houseID, city, country, rating, numRatings, minCost FROM houses WHERE" +
//                                    " houseID = ?";
//                            rSt = rCon.prepareStatement(query);
//                            rSt.setInt(1, predicted.get(i).left);
//                            rRs = rSt.executeQuery();
//                            while (rRs.next()) {
//                                HouseMinEntity entity = new HouseMinEntity();
//                                entity.setHouseId(rRs.getInt("houseID"));
//                                entity.setCity(rRs.getString("city"));
//                                entity.setCountry(rRs.getString("country"));
//                                entity.setRating(rRs.getFloat("rating"));
//                                entity.setNumRatings(rRs.getInt("numRatings"));
//                                entity.setMinCost(rRs.getFloat("minCost"));
//
//                                query = "SELECT thumbURL FROM airbnb_t.photographs " +
//                                        "WHERE houseID = " + rRs.getInt("houseID") + " LIMIT 1";
//                                rSt = rCon.prepareStatement(query);
//                                tmpRs = rSt.executeQuery();
//                                if (tmpRs.next()) {
//                                    entity.setPicture(FileHelper.getFileAsString(tmpRs.getString("thumbURL")));
//                                }
//
//                                entities.add(entity);
//                            }
//
//                            ConnectionCloser.closeAll(null, rSt, rRs);
//                        }
//
//                        Gson gson = new Gson();
//                        String response = gson.toJson(entities);
//                        return Response.ok(response).build();
//                    } catch (SQLException | IOException e) {
//                        e.printStackTrace();
//                        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
//                    } finally {
//                        ConnectionCloser.closeAll(rCon, rSt, rRs);
//                    }
//
//
//
//                }
//            }
//            System.out.println("Finished in " + (System.currentTimeMillis() - start) + " millis");
//
//            return Response.ok().build();
//        } catch (Exception e) {
//            e.printStackTrace();
//            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
//        } finally {
//            ConnectionCloser.closeAll(con, st, rs);
//        }
//    }
//
//    private ArrayList<Integer> getBucket(int currentUser, int houseCount, boolean[][] userHouseMatrix,
//                           HashMap<Integer, Integer> userMap, boolean sparse) {
//        int currentUserIndex = userMap.get(currentUser);
//
//        int stages = 2;
//        int buckets;
//        if (sparse) {
//            buckets = 500;
//        } else {
//            buckets = 4700;
//        }
//        LSHMinHash lsh = new LSHMinHash(stages, buckets, houseCount);
//
//        int k = 0;
//        ArrayList<Integer> otherUsers = new ArrayList<>();
//        int[] hash = lsh.hash(userHouseMatrix[currentUserIndex]);
//        int currentUserBucket = hash[0];
//
//        // We only care about the users that got into the same bucket
//        // that the current user did. Thus, there is no need to keep hold of all the buckets
//        ArrayList<Integer> hashes = new ArrayList<>();
//        for (int i = 0; i < userHouseMatrix.length; i++) {
//            if (i == currentUserIndex) continue;
//            boolean[] tmpVector = userHouseMatrix[i];
//
//            hash = lsh.hash(tmpVector);
//
//            if (hash[0] == currentUserBucket) {
//                otherUsers.add(userMap.get(i));
//            }
//        }
//
//        return otherUsers;
//    }
//
//
//    private void fixIndices(MyVector vector, HashMap<Integer, Integer> houseMap,
//                            HashMap<Integer, Double> currentUserMap) {
//        try {
//            for (Integer i : currentUserMap.keySet()) {
//                vector.set(houseMap.get(i), currentUserMap.get(i));
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
////    @Path("/fixDb")
////    @GET
////    public Response fixDb() {
////        try {
////            File imgFolder = new File(Constants.DIR + "/img/houses");
////            File[] listOfFiles = imgFolder.listFiles();
////            for (int i = 0; i < listOfFiles.length; i++) {
////                if (listOfFiles[i].isFile()) {
////                    Connection con = null;
////                    Statement st = null;
////                    try {
////                        con = DataSource.getInstance().getConnection();
////                        String pictureUrl = listOfFiles[i].getPath();
////                        String[] tmp = pictureUrl.split("/");
////                        String fileName = tmp[tmp.length - 1];
////                        tmp = fileName.split("\\.");
////                        String fileExt = tmp[tmp.length - 1];
////
////                        String newFileName = tmp[0] + "_thumb." + fileExt;
////                        String thumbLocalUrl = Constants.DIR + "/thumbnails/houses/" + newFileName;
////                        String update = "UPDATE photographs SET thumbURL = \"" + thumbLocalUrl
////                                + "\" WHERE pictureURL = \"" + pictureUrl + "\" LIMIT 1";
////                        st = con.createStatement();
////                        st.executeUpdate(update);
//////                        String query = "SELECT photoID from photographs where pictureURL = " + pictureUrl
//////                                + " LIMIT 1";
//////                        st = con.createStatement();
//////                        rs = st.executeQuery(query);
//////                        if (rs.next()) {
//////                            int photoID = rs.getInt("photoID");
//////                            Connection wCon = null;
//////                            Statement wSt = null;
//////                            try {
//////                                wCon = DataSource.getInstance().getConnection();
//////                                String update = "UPDATE photographs SET thumbURL = " + thumbUrl +
//////                                        " WHERE photoID = " +
//////                            } catch (SQLException | IOException e) {
//////                                e.printStackTrace();
//////                                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
//////                            } finally {
//////                                Helpers.ConnectionCloser.closeAll(wCon, wSt, null);
//////                            }
//////                        }
////                    } catch (IOException | SQLException e) {
////                        e.printStackTrace();
////                        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
////                    } finally {
////                        ConnectionCloser.closeAll(con, st, null);
////                    }
////                }
////            }
////            return Response.ok().build();
////        } catch (Exception e) {
////            e.printStackTrace();
////            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
////        }
////    }
//
//
//    private Boolean getRandomBoolean() {
//        return ThreadLocalRandom.current().nextInt(0, 100) >= 50;
//    }
//}

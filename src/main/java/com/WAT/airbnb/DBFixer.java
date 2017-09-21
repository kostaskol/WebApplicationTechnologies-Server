package com.WAT.airbnb;

import com.WAT.airbnb.db.DataSource;
import com.WAT.airbnb.etc.*;
import com.WAT.airbnb.util.DateRange;
import com.WAT.airbnb.util.helpers.ConnectionCloser;
import com.WAT.airbnb.util.helpers.DateHelper;
import com.WAT.airbnb.util.helpers.FileHelper;
import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import info.debatty.java.lsh.LSHMinHash;
import jdk.nashorn.internal.parser.JSONParser;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import javax.imageio.ImageIO;
import javax.json.*;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;
import java.sql.Date;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

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
                    Date maxDateFrom = DateHelper.stringToDate("2020-12-1");
                    Date maxDate = DateHelper.stringToDate("2020-12-31");
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
                ConnectionCloser.closeAll(con, pSt, null);

            }

            return Response.ok().build();
        } catch (IOException | IllegalStateException | IllegalArgumentException | ParseException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    private ArrayList<Integer> getHouses() {
        Connection con = null;
        Statement st = null;
        ResultSet rs = null;
        ArrayList<Integer> houses = new ArrayList<>();
        try {
            con = DataSource.getInstance().getConnection();
            String query = "SELECT houseID FROM houses";
            st = con.createStatement();
            rs = st.executeQuery(query);
            while (rs.next()) {
                houses.add(rs.getInt("houseID"));
            }
            return houses;
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            ConnectionCloser.closeAll(con, st, rs);
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
            ConnectionCloser.closeAll(con, st, rs);
        }
    }



    private String savePicture(URL url, String name) throws IOException {
        BufferedImage img = ImageIO.read(url);
        String localUrl = Constants.DIR + "/img/users/" + name + ".jpg";
        File output = new File(localUrl);
        ImageIO.write(img, "jpg", output);
        return localUrl;
    }

    @Path("fixpicnames/{type}")
    @GET
    public Response fixPicNames(@PathParam("type") String type) {
        Connection idCon = null;
        Statement idSt = null;
        ResultSet idRs = null;

        Connection con = null;
        Statement st = null;
        ResultSet rs = null;

        Connection wCon = null;
        PreparedStatement wSt = null;
        String update = "UPDATE photographs SET pictureURL = ?, thumbURL = ? WHERE houseID = ?";
        try {
            idCon = DataSource.getInstance().getConnection();
            con = DataSource.getInstance().getConnection();
            wCon = DataSource.getInstance().getConnection();
            String query = "SELECT houseID from houses";
            idSt = idCon.createStatement();
            idRs = idSt.executeQuery(query);
            while (idRs.next()) {
                int id = idRs.getInt("houseID");
                query = "SELECT pictureURL, thumbURL FROM photographs WHERE houseID = " + id;
                try {
                    st = con.createStatement();
                    rs = st.executeQuery(query);
                    while (rs.next()) {
                        String picUrl = rs.getString("pictureURL");
                        String thumbUrl = rs.getString("thumbURL");
                        File oldFile = new File(picUrl);
                        String newFileUrl = Constants.DIR + "/img/houses/" + id + "/" + Calendar.getInstance().getTimeInMillis() + ".jpg";
                        oldFile.renameTo(new File(newFileUrl));

                        File oldThumbFile = new File(thumbUrl);
                        String newThumbUrl = Constants.DIR + "/img/houses/" + id + "/thumb/" + Calendar.getInstance().getTimeInMillis() + ".jpg";
                        oldThumbFile.renameTo(new File(newThumbUrl));

                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        wSt = wCon.prepareStatement(update);
                        wSt.setString(1, newFileUrl);
                        wSt.setString(2, newThumbUrl);
                        wSt.setInt(3, id);
                        wSt.execute();
                        try {
                            wSt.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            return Response.ok().build();
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            ConnectionCloser.closeAll(idCon, idSt, idRs);
            ConnectionCloser.closeAll(con, st, rs);
            ConnectionCloser.closeAll(wCon, wSt, null);
        }
    }

    @Path("/createdirs")
    @GET
    public Response createDirs() {
        Connection con = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            con = DataSource.getInstance().getConnection();
            String query = "SELECT houseID FROM houses";
            st = con.createStatement();
            rs = st.executeQuery(query);
            while(rs.next()) {
                new File(Constants.DIR + "/img/houses/" + rs.getInt("houseID") + "/thumb").mkdirs();
            }
            return Response.ok().build();
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            ConnectionCloser.closeAll(con, st, rs);
        }
    }

    @Path("/fillcomments")
    @GET
    public Response fillComments() {
        ArrayList<Integer> userIds = getUsers();
        ArrayList<Integer> houseIds = getHouses();
        if (userIds == null || houseIds == null) return Response.ok().build();
        Connection con = null;
        PreparedStatement pSt = null;
        Reader in = null;
        ArrayList<String> comments = new ArrayList<>();
        try {
            // Fill in the comments
            in = new FileReader(Constants.DIR + "/dataset/reviews.csv");
            for (CSVRecord record : CSVFormat.DEFAULT.withHeader().parse(in)) {
                comments.add(record.get("comments"));
            }
        } catch (IOException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        try {
            con = DataSource.getInstance().getConnection();
            int inserted = 0;
            for (Integer userId : userIds) {
                int size = randomInt(houseIds.size() / 4, houseIds.size() / 2);
                ArrayList<Integer> tmpHouseIds = new ArrayList<>(size);
                int houses = 0;
                for (int i = 0; i < size;) {
                    int randId = choice(houseIds);
                    if (tmpHouseIds.contains(randId)) continue;
                    tmpHouseIds.add(randId);
                    i++;
                }

                String insert = "INSERT INTO comments (userID, houseID, comm, rating) VALUES (?, ?, ?, ?)";
                pSt = con.prepareStatement(insert);
                con.setAutoCommit(false);
                for (Integer houseId : tmpHouseIds) {
                    System.out.println("User #" + inserted + " house #" + houses++);
                    pSt.setInt(1, userId);
                    pSt.setInt(2, houseId);
                    pSt.setString(3, choice(comments, null));
                    pSt.setDouble(4, (double) randomFloat(0f, 1f));
                    pSt.addBatch();
                }
                inserted++;
                pSt.executeBatch();
                con.commit();
                try {
                    pSt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            return Response.ok().build();
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            ConnectionCloser.closeAll(con, null, null);
        }

    }

    @Path("/populatesearch")
    @GET
    public Response populateSearch() {
        ArrayList<Integer> houseIds = getHouses();
        int user = 33;
        Connection con = null;
        PreparedStatement pSt = null;
        try {
            con = DataSource.getInstance().getConnection();
            String insert = "INSERT INTO searches (userID, houseID) VALUES(?, ?)";
            pSt = con.prepareStatement(insert);
            con.setAutoCommit(false);
            ArrayList<Integer> blackList = new ArrayList<>();
            for (int i = 0; i < randomInt(30, 50); i++) {
                pSt.setInt(1, user);
                int houseId;
                do {
                    houseId = houseIds.get(randomInt(0, houseIds.size()));
                } while (blackList.contains(houseId));
                blackList.add(houseId);
                pSt.setInt(2, houseId);
                pSt.addBatch();
                System.out.println("Adding house " + i);
            }
            pSt.executeBatch();
            con.commit();
            return Response.ok().build();
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            ConnectionCloser.closeAll(con, pSt, null);
        }
    }

    private Integer choice(ArrayList<Integer> list) {
        return list.get(randomInt(0, list.size()));
    }

    private String choice(ArrayList<String> list, Object ph) {
        return list.get(randomInt(0, list.size()));
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
        return DateHelper.stringToDate(randomDate.getYear() + "-" +
                randomDate.getMonthValue() + "-" +
                randomDate.getDayOfMonth());
    }

    @Path("/fixratings")
    @GET
    public Response fixRatings() {
        Connection con = null;
        PreparedStatement pSt = null;
        ResultSet rs = null;
        try {
            con = DataSource.getInstance().getConnection();
            String query = "SELECT DISTINCT(houseID), COUNT(houseID) AS c FROM comments GROUP BY houseID";
            Statement st = con.createStatement();
            rs = st.executeQuery(query);
            HashMap<Integer, Integer> idCountMap = new HashMap<>();
            while (rs.next()) {
                idCountMap.put(rs.getInt("houseID"), rs.getInt("c"));
            }

            try {
                st.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }


            query = "UPDATE houses SET numRatings = ? WHERE houseID = ?";
            pSt = con.prepareStatement(query);
            con.setAutoCommit(false);
            for (Integer id : idCountMap.keySet()) {
                pSt.setInt(1, idCountMap.get(id));
                pSt.setInt(2, id);
                pSt.addBatch();
                System.out.println("Setting id " + id + " to " + idCountMap.get(id));
            }


            pSt.executeBatch();
            con.commit();
            return Response.ok().build();
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            ConnectionCloser.closeAll(con, pSt, rs);
        }
    }

    @Path("/populatenames")
    @GET
    public Response populateNames() throws Exception {
        StringBuilder responseBuilder = new StringBuilder();
        URL url = new URL("https://randomuser.me/api/?results=864");
        HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
        httpCon.setRequestMethod("GET");
        BufferedReader br = new BufferedReader(new InputStreamReader(httpCon.getInputStream()));
        String line;
        while ((line = br.readLine()) != null) {
            responseBuilder.append(line);
        }
        br.close();
        String response = responseBuilder.toString();

        JsonReader jsonReader = Json.createReader(new StringReader(response));
        JsonObject object = jsonReader.readObject();
        if (object == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        jsonReader.close();
        JsonArray results = object.getJsonArray("results");
        ArrayList<String> firstNames = new ArrayList<>();
        ArrayList<String> lastNames = new ArrayList<>();
        ArrayList<String> urls = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            JsonObject obj = results.getJsonObject(i);
            firstNames.add(obj.getJsonObject("name").getString("first"));
            lastNames.add(obj.getJsonObject("name").getString("last"));
            urls.add(obj.getJsonObject("picture").getString("thumbnail"));
        }

        ArrayList<Integer> userIds = getUsers();

        Connection con = null;
        PreparedStatement pSt = null;
        try {
            con = DataSource.getInstance().getConnection();
            String update = "UPDATE users_t SET firstName = ?, lastName = ?, pictureURL = ?, approved = ? WHERE userID = ?";
            for (int i = 0; i < userIds.size(); i++) {
                String fName = firstNames.get(i);
                String lName = lastNames.get(i);
                String localUrl = savePicture(new URL(urls.get(i)), String.valueOf(i));

                pSt = con.prepareStatement(update);
                pSt.setString(1, fName);
                pSt.setString(2, lName);
                pSt.setString(3, localUrl);
                pSt.setBoolean(4, randomBoolean());
                pSt.setInt(5, userIds.get(i));
                pSt.execute();
                try {
                    pSt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            return Response.ok().build();

        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            ConnectionCloser.closeAll(con, pSt, null);
        }
    }

    @Path("/fixuserpictures")
    @GET
    public Response fixUserPictures() throws Exception {
        StringBuilder responseBuilder = new StringBuilder();
        URL url = new URL("https://randomuser.me/api/?results=864");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            responseBuilder.append(line);
        }
        reader.close();
        JsonReader jsonReader = Json.createReader(new StringReader(responseBuilder.toString()));
        JsonObject responseObject = jsonReader.readObject();
        jsonReader.close();

        JsonArray jsonArray = responseObject.getJsonArray("results");
        ArrayList<Integer> users = getUsers();
        Connection con = null;
        PreparedStatement pSt = null;
        try {
            con = DataSource.getInstance().getConnection();
            String update = "UPDATE users SET pictureURL = ? WHERE userID = ?";
            for (int i = 0; i < users.size(); i++) {
                url = new URL(jsonArray.getJsonObject(i).getJsonObject("picture").getString("large"));
                BufferedImage bi = ImageIO.read(url);
                String path = Constants.DIR + "/img/users/" + users.get(i) + ".jpg";
                File file = new File(path);
                ImageIO.write(bi, "jpg", file);

                pSt = con.prepareStatement(update);
                pSt.setString(1, path);
                pSt.setInt(2, users.get(i));
                pSt.execute();
                try {
                    pSt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            return Response.ok().build();
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            ConnectionCloser.closeAll(con, pSt, null);
        }

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


    @Path("/fixpictures")
    @GET
    public Response fixPictures() {
        ArrayList<String> urls = new ArrayList<>();
        try {
            Reader in = new FileReader(Constants.DIR + "/dataset/listings.csv");
            for (CSVRecord record : CSVFormat.DEFAULT.withHeader().parse(in)) {
                urls.add(record.get("picture_url"));
            }
        } catch (IOException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        ArrayList<Integer> houseIds = getHouses();

        Connection con = null;
        PreparedStatement pSt = null;
        try {
            con = DataSource.getInstance().getConnection();
            String insert = "INSERT INTO photographs (houseID, pictureURL, thumbURL, main)" +
                    "VALUES (?, ?, ?, 1)";
            for (int i = 0; i < houseIds.size(); i++) {
                System.out.println("Saving picture " + i);
                try {
                    String localUrl = Constants.DIR + "/img/houses/" + i + ".jpg";
                    String localThumbUrl = FileHelper.saveFileThumb(localUrl, false);

                    pSt = con.prepareStatement(insert);
                    pSt.setInt(1, houseIds.get(i));
                    pSt.setString(2, localUrl);
                    pSt.setString(3, localThumbUrl);
                    pSt.execute();
                    try {
                        pSt.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                } catch (IOException | SQLException e) {
                    e.printStackTrace();
                }
            }
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            ConnectionCloser.closeAll(con, pSt, null);
        }
        return Response.ok().build();
    }


    @Path("/savepicture")
    @GET
    public Response savePicture() {
        Reader reader = null;
        HttpURLConnection connection = null;
        try {
            reader = new FileReader(Constants.DIR + "/dataset/listings.csv");
            int i = 0;
            for (CSVRecord record : CSVFormat.DEFAULT.withHeader().parse(reader)) {
                if (i++ == 3) return Response.ok().build();
                try {
                    URL url = new URL(record.get("picture_url"));
                    BufferedImage img = ImageIO.read(url);
                    File outp = new File(Constants.DIR + "/test" + i + ".jpg");
                    ImageIO.write(img, "jpg", outp);
                } catch (IOException e) {
                    e.printStackTrace();
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }
            return Response.ok().build();

        } catch (IOException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private ArrayList<Integer> getPicturelessHouses() {
        Connection con = null;
        Statement st = null;
        ResultSet rs = null;
        ArrayList<Integer> tmp = new ArrayList<>();
        try {
            con = DataSource.getInstance().getConnection();
            String query = "SELECT h.houseID FROM houses h " +
                    "LEFT JOIN photographs p on p.houseID = h.houseID " +
                    "WHERE p.houseID IS NULL";
            st = con.createStatement();
            rs = st.executeQuery(query);
            while (rs.next()) {
                tmp.add(rs.getInt("h.houseID"));
            }
            return tmp;
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            ConnectionCloser.closeAll(con, st, rs);
        }
    }

    @Path("/testlsh")
    @GET
    public Response testLsh() {
        double sparsity = 0.75;
        int count = 10000;
        int n = 100;
        int stages = 2;

        int buckets = 10;

        boolean[][] vectors = new boolean[count][n];
        Random rand = new Random();
        for (int i = 0; i < count; i++) {
            for (int j = 0; j < n; j++) {
                vectors[i][j] = rand.nextDouble() > sparsity;
            }
        }

        LSHMinHash lsh = new LSHMinHash(stages, buckets, n);

        int[][] counts = new int[stages][buckets];
        for (boolean[] vector : vectors) {
            int[] hash = lsh.hash(vector);
            for (int i = 0; i < hash.length; i++) {
                counts[i][hash[i]]++;

                print(vector);
                System.out.println(" : ");
                print(hash);
                System.out.print("\n");
            }
        }

        System.out.println("Number of elements per bucket at each stage: ");
        for (int i = 0; i < stages; i++) {
            print(counts[i]);
            System.out.print("]");
        }
        return Response.ok().build();
    }

    private void print(int[] array) {
        System.out.print("[");
        for (int v : array) {
            System.out.print("" + v + " ");
        }
        System.out.print("]");
    }

    private void print(boolean[] array) {
        System.out.print("[");
        for (boolean v : array) {
            System.out.print(v ? "1" : "0");
        }
        System.out.print("]");
    }
}

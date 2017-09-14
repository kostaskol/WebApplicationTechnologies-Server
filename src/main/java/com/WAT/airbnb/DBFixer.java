package com.WAT.airbnb;

import com.WAT.airbnb.db.DataSource;
import com.WAT.airbnb.etc.*;
import com.WAT.airbnb.util.DateRange;
import com.WAT.airbnb.util.helpers.ConnectionCloser;
import com.WAT.airbnb.util.helpers.DateHelper;
import com.WAT.airbnb.util.helpers.FileHelper;
import info.debatty.java.lsh.LSHMinHash;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import javax.imageio.ImageIO;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
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

    @Path("/fillcomments")
    @GET
    public Response fillComments() {
        ArrayList<Integer> userIds = getUsers();
        ArrayList<Integer> houseIds = getHouses();
        if (userIds == null || houseIds == null) return Response.ok().build();
        HashMap<Integer, Integer> blackList = new HashMap<>();
        Connection con = null;
        PreparedStatement pSt = null;
        int houseNum = 0;
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
                int size = randomInt(20, 50);
                ArrayList<Integer> tmpHouseIds = new ArrayList<>(size);
                int houses = 0;
                for (int i = 0; i < size;) {
                    int randId = choice(houseIds);
                    if (tmpHouseIds.contains(randId)) continue;
                    tmpHouseIds.add(randId);
                    i++;
                }

                for (Integer houseId : tmpHouseIds) {
                    System.out.println("User #" + inserted + " house #" + houses++);
                    String insert = "INSERT INTO comments (userID, houseID, comm) VALUES (?, ?, ?)";
                    pSt = con.prepareStatement(insert);
                    pSt.setInt(1, userId);
                    pSt.setInt(2, houseId);
                    pSt.setString(3, choice(comments, null));
                    pSt.execute();
                    try {
                        pSt.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                inserted++;
            }
            return Response.ok().build();
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            ConnectionCloser.closeAll(con, null, null);
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
            String update = "UPDATE photographs_t SET pictureURL = ?, thumbURL = ? WHERE houseID = ? LIMIT 1";
            for (int i = 0; i < houseIds.size(); i++) {
                System.out.println("Saving picture " + i);
                try {
                    String localUrl = savePicture(new URL(urls.get(randomInt(0, urls.size() - 1))),
                            String.valueOf(i));
                    String localThumbUrl = FileHelper.saveFileThumb(localUrl, false);

                    pSt = con.prepareStatement(update);
                    pSt.setString(1, localUrl);
                    pSt.setString(2, localThumbUrl);
                    pSt.setInt(3, houseIds.get(i));

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

    private String savePicture(URL url, String name) throws IOException {
            BufferedImage img = ImageIO.read(url);
            String localUrl = Constants.DIR + "/img/houses/" + name + ".jpg";
            File output = new File(localUrl);
            ImageIO.write(img, "jpg", output);
            return localUrl;
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

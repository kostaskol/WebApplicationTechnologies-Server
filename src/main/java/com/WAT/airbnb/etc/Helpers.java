package com.WAT.airbnb.etc;

import com.WAT.airbnb.db.DataSource;
import com.WAT.airbnb.rest.entities.House;
import com.WAT.airbnb.rest.entities.HouseMinEntity;
import com.WAT.airbnb.rest.entities.HousePageBundle;
import com.jamesmurty.utils.XMLBuilder;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.sql.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.lang.String.valueOf;

public class Helpers {
    static public class DateHelper {
        static public java.sql.Date stringToDate(String strDate) throws ParseException {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            java.util.Date date = sdf.parse(strDate);
            return new java.sql.Date(date.getTime());
        }

        static public String dateToString(java.util.Date date) {
            if (date != null) {
                java.util.Date utilDate = new java.util.Date(date.getTime());
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
                return dateFormat.format(utilDate);
            }
            return null;
        }

        static public Timestamp stringToDateTime(String strDate) throws ParseException {
            if (strDate == null || strDate.equals("null")) return null;
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            java.util.Date date = sdf.parse(strDate);
            return new java.sql.Timestamp(date.getTime());
        }
    }

    static public class ConnectionCloser {
        public static void closeAll(Connection con, Statement statement, ResultSet rs) {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    e.printStackTrace();

                }
            }
        }
    }

    static public class HouseGetter {
        public static HousePageBundle getHouseMinList(ResultSet minHouseRs) throws SQLException, IOException {
            ArrayList<House> entities = new ArrayList<>();

            while (minHouseRs.next()) {
                HouseMinEntity entity = new HouseMinEntity();
                entity.setHouseId(minHouseRs.getInt("houseID"));
                entity.setCity(minHouseRs.getString("city"));
                entity.setCountry(minHouseRs.getString("country"));
                entity.setRating(minHouseRs.getFloat("rating"));
                entity.setNumRatings(minHouseRs.getInt("numRatings"));
                entity.setMinCost(minHouseRs.getFloat("minCost"));

                Connection picCon = DataSource.getInstance().getConnection();
                String query = "SELECT pictureURL FROM photographs WHERE houseID = ? AND main = 1";
                PreparedStatement picSt = picCon.prepareStatement(query);
                picSt.setInt(1, minHouseRs.getInt("houseID"));
                ResultSet picRs = picSt.executeQuery();
                if (picRs.next()) {
                    entity.setPicture(Helpers.FileHelper.getFileAsString(picRs.getString("pictureURL")));
                }
                entities.add(entity);
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
            }
            HousePageBundle bundle = new HousePageBundle();
            bundle.setHouses(entities);
            bundle.setNumPages(entities.size() / Constants.PAGE_SIZE + 1);
            return bundle;
        }
    }

    static public class ScopeFiller {
        static public List<String> fillScope(int minScope) {
            List<String> scopes = new ArrayList<>();
            switch (minScope) {
                case Constants.TYPE_USER:
                    scopes.add(Constants.SCOPE_ADMINS);
                    scopes.add(Constants.SCOPE_RENTERS);
                    scopes.add(Constants.SCOPE_USERS);
                    break;
                case Constants.TYPE_RENTER:
                    scopes.add(Constants.SCOPE_ADMINS);
                    scopes.add(Constants.SCOPE_RENTERS);
                    break;
                case Constants.TYPE_ADMIN:
                    scopes.add(Constants.SCOPE_ADMINS);
                    break;
            }

            return scopes;
        }

    }

    static public class FileHelper {
        static public String saveFile(InputStream uploadedInputStream, int id,
                                      FormDataContentDisposition fileDetails,
                                      boolean user) throws IOException {
            String fileUrl = null;
            String[] split = fileDetails.getFileName().split("\\.");
            if (user) {
                fileUrl = Constants.DIR + "/img/users/" + id;
            } else {
                fileUrl = Constants.DIR + "/img/houses/" + id;
            }
            File tmpFile = new File(fileUrl);

            for (int i = 0; tmpFile.exists(); i++) {
                System.out.println("Looping " + i);
                if (i < 10) {
                    fileUrl = fileUrl.substring(0, fileUrl.length() - 1);
                } else if (i < 100) {
                    fileUrl = fileUrl.substring(0, fileUrl.length() - 2);
                }

                fileUrl += i;
            }

            for (String s : split) {
                System.out.println(s);
            }

            System.out.println("Mime: " + split[split.length - 1]);

            fileUrl += "." + split[split.length - 1];

            System.out.println("FileUrl: " + fileUrl);

            OutputStream out = null;
            try {
                out = new FileOutputStream(new File(fileUrl));
                int read;
                byte[] bytes = new byte[1024];
                while ((read = uploadedInputStream.read(bytes)) != -1) {
                    out.write(bytes, 0, read);
                }
                return fileUrl;
            } finally {
                if (out != null) {
                    out.flush();
                    out.close();
                }
            }
        }

        static public String getFileAsString(String localUrl) throws IOException {
            File file = new File(localUrl);
            return Base64.getEncoder().withoutPadding().encodeToString(Files.readAllBytes(file.toPath()));
        }
    }

    static public class XmlBuilder {
        static public XMLBuilder getXml(ResultSet users, ResultSet houses, ResultSet comments, ResultSet bookings,
                                    ResultSet messages) {
            try {
                XMLBuilder xmlBuilder = XMLBuilder.create("Airbnb")
                        .e("Users");

                while (users.next()) {
                    if (users.getString("email").equals("root")) continue;
                    xmlBuilder = xmlBuilder.e("User")
                            .e("UserID")
                                .t(valueOf(users.getInt("userID")))
                            .up()
                            .e("Email")
                                .t(users.getString("email"))
                            .up()
                            .e("AccountType")
                                .t(users.getString("accType"))
                            .up()
                            .e("FirstName")
                                .t(users.getString("firstName"))
                            .up()
                            .e("LastName")
                                .t(users.getString("lastName"))
                            .up()
                            .e("PhoneNumber")
                                .t(users.getString("phoneNumber"))
                            .up()
                            .e("DateOfBirth")
                                .t(Helpers.DateHelper.dateToString(users.getDate("dateOfBirth")))
                            .up()
                            .e("Country")
                                .t(users.getString("country"))
                            .up();
                            String bio = users.getString("bio");
                            if (bio == null) {
                                bio = "Unavailable";
                            }
                            xmlBuilder = xmlBuilder
                            .e("Bio")
                                .t(bio)
                            .up()
                            .e("Approved")
                                .t(valueOf(users.getBoolean("approved")))
                            .up()
                            .up();
                }

                xmlBuilder = xmlBuilder.up();

                xmlBuilder = xmlBuilder.e("Houses");

                while(houses.next()) {
                    xmlBuilder = xmlBuilder.e("House")
                            .e("HouseID")
                                .t(valueOf(houses.getInt("houseID")))
                            .up()
                            .e("OwnerID")
                                .t(valueOf(houses.getInt("ownerID")))
                            .up()
                            .e("Latitude")
                                .t(valueOf(houses.getFloat("latitude")))
                            .up()
                            .e("Longitude")
                                .t(valueOf(houses.getFloat("longitude")))
                            .up();

                            String address = houses.getString("address");
                            if (address == null) {
                                address = "Unavailable";
                            }

                            xmlBuilder = xmlBuilder
                            .e("Address")
                                .t(address)
                            .up();

                            String city = houses.getString("city");
                            if (city == null) {
                                city = "Unavailable";
                            }

                            xmlBuilder = xmlBuilder
                            .e("City")
                                .t(city)
                            .up();

                            String country = houses.getString("country");
                            if (country == null) {
                                country = "Unavailable";
                            }

                            xmlBuilder = xmlBuilder
                            .e("Country")
                                .t(country)
                            .up()
                            .e("NumberOfBeds")
                                .t(valueOf(houses.getInt("numBeds")))
                            .up()
                            .e("NumberOfBaths")
                                .t(valueOf(houses.getInt("numBaths")))
                            .up()
                            .e("Accommodates")
                                .t(valueOf(houses.getInt("accommodates")))
                            .up()
                            .e("LivingRoom")
                                .t(valueOf(houses.getBoolean("hasLivingRoom")))
                            .up()
                            .e("SmokingAllowed")
                                .t(valueOf(houses.getBoolean("smokingAllowed")))
                            .up()
                            .e("PetsAllowed")
                                .t(valueOf(houses.getBoolean("petsAllowed")))
                            .up()
                            .e("EventsAllowed")
                                .t(valueOf(houses.getBoolean("eventsAllowed")))
                            .up()
                            .e("WiFi")
                                .t(valueOf(houses.getBoolean("wifi")))
                            .up()
                            .e("Airconditioning")
                                .t(valueOf(houses.getBoolean("airconditioning")))
                            .up()
                            .e("Heating")
                                .t(valueOf(houses.getBoolean("heating")))
                            .up()
                            .e("Kitchen")
                                .t(valueOf(houses.getBoolean("kitchen")))
                            .up()
                            .e("TV")
                                .t(valueOf(houses.getBoolean("tv")))
                            .up()
                            .e("Parking")
                                .t(valueOf(houses.getBoolean("parking")))
                            .up()
                            .e("Elevator")
                                .t(valueOf(houses.getBoolean("elevator")))
                            .up()
                            .e("Area")
                                .t(valueOf(houses.getFloat("area")))
                            .up()
                            .e("Description")
                                .t(houses.getString("description"))
                            .up()
                            .e("Instructions")
                                .t(houses.getString("instructions"))
                            .up()
                            .e("MinimumDays")
                                .t(valueOf(houses.getInt("minDays")))
                            .up()
                            .e("Rating")
                                .t(valueOf(houses.getFloat("rating")))
                            .up()
                            .e("NumberOfRatings")
                                .t(valueOf(houses.getInt("numRatings")))
                            .up()
                            .e("DateFrom")
                                .t(Helpers.DateHelper.dateToString(houses.getDate("dateFrom")))
                            .up()
                            .e("DateTo")
                                .t(Helpers.DateHelper.dateToString(houses.getDate("dateTo")))
                            .up()
                            .e("MinimumCost")
                                .t(valueOf(houses.getFloat("minCost")))
                            .up()
                            .e("CostPerPerson")
                                .t(valueOf(houses.getFloat("costPerPerson")))
                            .up()
                            .up();
                }

                xmlBuilder = xmlBuilder.up();

                xmlBuilder = xmlBuilder.e("Comments");

                while (comments.next()) {
                    xmlBuilder = xmlBuilder.e("Comment")
                            .e("CommentID")
                                .t(valueOf(comments.getInt("commentID")))
                            .up()
                            .e("UserID")
                                .t(valueOf(comments.getInt("userID")))
                            .up()
                            .e("HouseID")
                                .t(valueOf(comments.getInt("houseID")))
                            .up()
                            .e("Comment")
                                .t(comments.getString("comm"))
                            .up()
                            .e("Rating")
                                .t(valueOf(comments.getFloat("rating")))
                            .up()
                            .up();
                }

                xmlBuilder = xmlBuilder.up();

                xmlBuilder = xmlBuilder.e("Messages");

                while (messages.next()) {
                    xmlBuilder = xmlBuilder.e("Message")
                            .e("MessageID")
                                .t(valueOf(messages.getInt("messageID")))
                            .up()
                            .e("SenderID")
                                .t(valueOf(messages.getInt("senderID")))
                            .up()
                            .e("ReceiverID")
                                .t(valueOf(messages.getInt("receiverID")))
                            .up()
                            .e("Message")
                                .t(messages.getString("message"))
                            .up()
                            .e("Deleted")
                                .t(messages.getString("deleted"))
                            .up()
                            .up();
                }

                xmlBuilder = xmlBuilder.up();

                xmlBuilder = xmlBuilder.e("Bookings");

                while (bookings.next()) {
                    xmlBuilder = xmlBuilder.e("Booking")
                            .e("BookingID")
                                .t(valueOf(bookings.getInt("bookingID")))
                            .up()
                            .e("UserID")
                                .t(valueOf(bookings.getInt("userID")))
                            .up()
                            .e("HouseID")
                                .t(valueOf(bookings.getInt("houseID")))
                            .up()
                            .e("NumberOfGuests")
                                .t(valueOf(bookings.getInt("guests")))
                            .up()
                            .e("DateFrom")
                                .t(Helpers.DateHelper.dateToString(bookings.getDate("dateFrom")))
                            .up()
                            .e("DateTo")
                                .t(Helpers.DateHelper.dateToString(bookings.getDate("dateTo")))
                            .up()
                            .up();
                }

                xmlBuilder = xmlBuilder.up();

                return xmlBuilder;
            } catch (ParserConfigurationException  | SQLException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    static public class ReverseGeocoder {
        static public String[] convert(float lat, float lng) throws MalformedURLException, IOException {
            XmlParser parser = new XmlParser(Constants.DIR + "/config.xml");
            URL mapsUrl = new URL("https://maps.googleapis.com/maps/api/geocode/json?latlng=" + lat + "," + lng +
                    "&key=" + parser.get("api-key"));
            HttpURLConnection con = (HttpURLConnection) mapsUrl.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Accept", "application/json");
            if (con.getResponseCode() != 200) {
                throw new RuntimeException("Connection failed with http error code " + con.getResponseCode());
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String outp;
            StringBuilder addrBuilder = new StringBuilder();
            while ((outp = reader.readLine()) != null) {
                addrBuilder.append(outp);
            }


            JsonReader jsonReader = Json.createReader(new StringReader(addrBuilder.toString()));
            JsonObject object = jsonReader.readObject();
            if (object == null) {
                System.err.println("Json Object is null");
                return null;
            }
            jsonReader.close();
            if (object.getString("status").equals("OK")) {

                String[] response = new String[3];
                JsonObject result = object.getJsonArray("results").getJsonObject(0);
                response[Constants.ADDR_OFFS] = result.getString("formatted_address");
                JsonArray arr = result.getJsonArray("address_components");
                for (int i = 0; i < arr.size(); i++) {
                    JsonObject tmpObject = arr.getJsonObject(i);
                    if (tmpObject.getJsonArray("types").getString(0).equals("country")) {
                        response[Constants.COUNTRY_OFFS] = tmpObject.getString("long_name");
                        break;
                    } else if (tmpObject.getJsonArray("types").getString(0).equals("locality")) {
                        response[Constants.CITY_OFFS] = tmpObject.getString("long_name");
                    }
                }
                return response;
            }
            throw new RuntimeException("Bad status code: " + object.getString("status"));
        }
    }
}

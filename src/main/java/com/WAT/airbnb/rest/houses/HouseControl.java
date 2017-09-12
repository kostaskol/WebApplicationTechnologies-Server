package com.WAT.airbnb.rest.houses;

import com.WAT.airbnb.db.DataSource;
import com.WAT.airbnb.etc.Constants;
import com.WAT.airbnb.etc.DateRange;
import com.WAT.airbnb.etc.Helpers;
import com.WAT.airbnb.etc.QueryBuilder;
import com.WAT.airbnb.rest.Authenticator;
import com.WAT.airbnb.rest.entities.*;
import com.google.gson.Gson;
import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import io.jsonwebtoken.SignatureException;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
        List<String> scopes = Helpers.ScopeFiller.fillScope(Constants.TYPE_RENTER);
        Authenticator auth = new Authenticator(token, scopes);
        if (!auth.authenticate()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        entity.setRating(0f);
        entity.setNumRatings(0);

        try {
            String[] addr = Helpers.ReverseGeocoder.convert(entity.getLatitude(), entity.getLongitude());
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
            pSt.setDate(27, Helpers.DateHelper.stringToDate(entity.getDateFrom())); // available from
            pSt.setDate(28, Helpers.DateHelper.stringToDate(entity.getDateTo())); // available to
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

            String fileUrl = Helpers.FileHelper.saveFile(uploadedFileInputStream, insertedId, fileDetails, false);

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

    @Path("/showactive")
    @GET
    public Response showActive() {
        try {
            DataSource.getInstance().printCon();
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
        return Response.ok().build();
    }

    @Path("/thumbtest")
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response thumbTest(@FormDataParam("file") InputStream uploadedFileInputStream,
                              @FormDataParam("file") FormDataContentDisposition fileDetails) {
        try {
            Helpers.FileHelper.saveFile(uploadedFileInputStream, 0, fileDetails, false);
            return Response.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Path("/thumbtestdown")
    @GET
    @Produces("image/jpeg")
    public Response thumbTestDown() {
        String localUrl = Constants.DIR + "/thumbnails/houses/0.jpg_thumb.jpg";
        try {
            String base64 = Helpers.FileHelper.getFileAsString(localUrl);
            return Response.ok(base64).build();
        } catch (Exception e) {
            e.printStackTrace();
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
            boolean[] hasArgs = new boolean[12];
            QueryBuilder queryBuilder = new QueryBuilder("SELECT houseID, city, country, rating, " +
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
            if (entity.getRating() != null) {
                queryBuilder.and("rating >= ?");
                hasArgs[6] = true;
            } else
                hasArgs[6] = false;
            if (entity.getDateFrom() != null && entity.getDateTo() != null) {
                System.out.println("None is null");
                queryBuilder.and("dateFrom <= ?").and("dateTo <= ?");
                hasArgs[7] = true;
            } else if (entity.getDateFrom() != null) {
                System.out.println("DateFrom is not null: " + entity.getDateFrom());
                queryBuilder.and("dateFrom <= ?");
                hasArgs[7] = false;
                hasArgs[8] = true;
            } else if (entity.getDateTo() != null) {
                queryBuilder.and("dateTo <= ?").and("dateFrom <= NOW()");
                hasArgs[7] = false;
                hasArgs[8] = false;
                hasArgs[9] = true;
            } else {
                hasArgs[7] = false;
                hasArgs[8] = false;
                hasArgs[9] = false;
            }
            if (entity.getMinCost() != null) {
                queryBuilder.and("minCost >= ?");
                hasArgs[10] = true;
            } else
                hasArgs[10] = false;
            if (entity.getCostPerPerson() != null) {
                queryBuilder.and("costPerPerson >= ?");
                hasArgs[11] = true;
            } else
                hasArgs[11] = false;

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
                pSt.setFloat(curr++, entity.getRating());
            }
            if (hasArgs[7]) {
                try {
                    pSt.setDate(curr++, Helpers.DateHelper.stringToDate(entity.getDateFrom()));
                    pSt.setDate(curr++, Helpers.DateHelper.stringToDate(entity.getDateTo()));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            } else if (hasArgs[8]) {
                try {
                    pSt.setDate(curr++, Helpers.DateHelper.stringToDate(entity.getDateFrom()));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            } else if (hasArgs[9]) {
                try {
                    pSt.setDate(curr++, Helpers.DateHelper.stringToDate(entity.getDateTo()));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            if (hasArgs[10])
                pSt.setFloat(curr++, entity.getMinCost());
            if (hasArgs[11])
                pSt.setFloat(curr, entity.getCostPerPerson());


            System.out.println("Executing statement: " + pSt.toString());

            ArrayList<HouseMinEntity> minEntities = new ArrayList<>();

            rs = pSt.executeQuery();
            while (rs.next()) {
                HouseMinEntity minEntity = new HouseMinEntity();
                minEntity.setHouseId(rs.getInt("houseId"));
                minEntity.setCity(rs.getString("city"));
                minEntity.setCountry(rs.getString("country"));
                minEntity.setRating(rs.getFloat("rating"));
                minEntity.setNumRatings(rs.getInt("numRatings"));
                minEntity.setMinCost(rs.getInt("minCost"));

                Connection picCon = DataSource.getInstance().getConnection();
                String query = "SELECT pictureURL from photographs WHERE houseId = ? LIMIT 1";
                pSt = picCon.prepareStatement(query);
                pSt.setInt(1, minEntity.getHouseId());
                ResultSet picRs = pSt.executeQuery();
                if (picRs.next()) {
                    minEntity.setPicture(Helpers.FileHelper.getFileAsString(picRs.getString("pictureURL")));
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
            if (con != null) {
                try {
                    con.close();
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

            if (pSt != null) {
                try {
                    pSt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    @Path("/getpage/{pagenum}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPage(@PathParam("pagenum") int pageNum) {
        Connection con = null;
        ResultSet rs = null;
        PreparedStatement pSt = null;
        ArrayList<House> entities = new ArrayList<>();
        try {

            con = DataSource.getInstance().getConnection();
            String query = "SELECT houseID, city, country, rating, numRatings, minCost FROM houses " +
                    "WHERE dateTo > NOW() AND available = 1 ORDER BY minCost ASC LIMIT ?, ?";
            pSt = con.prepareStatement(query);
            pSt.setInt(1, pageNum * Constants.PAGE_SIZE);
            pSt.setInt(2, pageNum * Constants.PAGE_SIZE + Constants.PAGE_SIZE);
            rs = pSt.executeQuery();
            HousePageBundle bundle = new HousePageBundle();
            while (rs.next()) {
                HouseMinEntity entity = new HouseMinEntity();
                entity.setCity(rs.getString("city"));
                entity.setCountry(rs.getString("country"));
                entity.setRating(rs.getFloat("rating"));
                entity.setNumRatings(rs.getInt("numRatings"));
                entity.setMinCost(rs.getFloat("minCost"));

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
                        entity.setPicture(Helpers.FileHelper.getFileAsString(picRs.getString("thumbURL")));
                    } else {
                        throw new SQLException("Empty result set");
                    }
                    entities.add(entity);
                } catch (SQLException | IOException e) {
                    e.printStackTrace();
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
                } finally {
                    Helpers.ConnectionCloser.closeAll(picCon, picpSt, picRs);
                }

            }

            if (entities.size() != 0) {
                bundle.setHouses(entities);
                bundle.setNumPages(entities.size() / Constants.PAGE_SIZE + 1);
                bundle.setStatus(Constants.STATUS_MODIFIED);
            } else {
                bundle.setStatus(Constants.STATUS_NOT_MODIFIED);
            }

            Gson gson = new Gson();
            String response = gson.toJson(bundle);
            System.gc();
            return Response.ok(response).build();
        } catch (IOException | SQLException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            Helpers.ConnectionCloser.closeAll(con, pSt, rs);
        }
    }

    @Path("/test")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response test(@QueryParam("lat") float lat, @QueryParam("lng") float lng) {
        try {
            System.out.println("Called");
            String[] s = Helpers.ReverseGeocoder.convert(lat, lng);
            for (String str : s) {
                System.out.println(str);
            }
            Gson gson = new Gson();
            return Response.ok(gson.toJson(s)).build();
        } catch (IOException e) {
            return Response.ok("{ 'err': 500 }").build();
        }
    }

    @Path("/getusershousesmin")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUsersHousesMin(String token) {
        List<String> scopes = Helpers.ScopeFiller.fillScope(Constants.TYPE_RENTER);
        Authenticator auth = new Authenticator(token, scopes);

        if (!auth.authenticate()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        int userId = auth.getId();

        Connection con = null;
        ResultSet rs = null;
        PreparedStatement pSt = null;
        try {
            con = DataSource.getInstance().getConnection();
            String query = "SELECT houseID, city, country, rating, numRatings, minCost FROM houses WHERE ownerID = ?";
            pSt = con.prepareStatement(query);
            pSt.setInt(1, userId);
            rs = pSt.executeQuery();
            HousePageBundle bundle = Helpers.HouseGetter.getHouseMinList(rs);
            Gson gson = new Gson();
            String jsonString = gson.toJson(bundle);
            return Response.ok(jsonString).build();
        } catch (IOException | SQLException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            Helpers.ConnectionCloser.closeAll(con, pSt, rs);
        }
    }

    @Path("/gethouse/{houseid}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response downloadAllPhotos(@PathParam("houseid") int houseId) {
       Connection con = null,
               picCon = null;
       ResultSet rs = null,
               picRs = null;
       try {
           // An list of all the start - end date pairs that are already booked
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
               if (bookCon != null) {
                   try {
                       bookCon.close();
                   } catch (SQLException e) {
                       e.printStackTrace();
                   }
               }

               if (bookRs != null) {
                   try {
                       bookRs.close();
                   } catch (SQLException e) {
                       e.printStackTrace();
                   }
               }
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
               house.setArea(rs.getFloat("area"));
               house.setDescription(rs.getString("description"));
               house.setMinDays(rs.getInt("minDays"));
               house.setInstructions(rs.getString("instructions"));
               house.setRating(rs.getFloat("rating"));
               house.setNumRatings(rs.getInt("numRatings"));
               house.setDateFrom(Helpers.DateHelper.dateToString(rs.getDate("dateFrom")));
               house.setDateTo(Helpers.DateHelper.dateToString(rs.getDate("dateTo")));
               house.setMinCost(rs.getFloat("minCost"));
               house.setCostPerPerson(rs.getFloat("costPerPerson"));
               house.setCostPerDay(rs.getFloat("costPerDay"));
               house.setOwnerId(rs.getInt("ownerID"));

               ArrayList<String> excludedDates = new ArrayList<>();

               for (Date[] date : dateList) {
                    List<LocalDate> localDates = new DateRange(date[0], date[1]).toList();
                    for (LocalDate d : localDates) {
                        excludedDates.add(Helpers.DateHelper.dateToString(Date.valueOf(d)));
                    }
               }

               house.setExcludedDates(excludedDates);

               picCon = DataSource.getInstance().getConnection();

               query = "SELECT pictureURL FROM photographs WHERE houseID = ?";

               pSt = picCon.prepareStatement(query);
               pSt.setInt(1, houseId);
               picRs = pSt.executeQuery();
               while (picRs.next()) {
                   house.addPicture(Helpers.FileHelper.getFileAsString(picRs.getString("pictureURL")));
               }

               try {
                   picCon.close();
               } catch (SQLException e) {
                   e.printStackTrace();
               }

               picCon = null;

               try {
                   picRs.close();
               } catch (SQLException e) {
                   e.printStackTrace();
               }

               picRs = null;

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
           if (con != null) {
               try {
                   con.close();
               } catch(SQLException e) {
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

           if (picCon != null) {
               try {
                   picCon.close();
               } catch (SQLException e) {
                   e.printStackTrace();
               }
           }

           if (picRs != null) {
               try {
                   picRs.close();
               } catch (SQLException e) {
                   e.printStackTrace();
               }
           }
       }
    }

    @Path("/updatehouse/{houseId}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateHouse(@PathParam("houseId") int houseId,
                                String json) {
        Gson gson = new Gson();
        HouseUpdateEntity entity = gson.fromJson(json, HouseUpdateEntity.class);
        List<String> scopes = Helpers.ScopeFiller.fillScope(Constants.TYPE_RENTER);
        Authenticator auth = new Authenticator(entity.getToken(), scopes);
        if (!auth.authenticate()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        Connection con = null;
        PreparedStatement pSt = null;
        try {
            con = DataSource.getInstance().getConnection();
            String[] location = Helpers.ReverseGeocoder.convert(entity.getHouse().getLatitude(), entity.getHouse().getLongitude());
            // TODO: Add new location to udpate
            HouseEntity he = entity.getHouse();
            String update = "UPDATE houses SET " +
                    "latitude = ?, longitude = ?, city = ?, country = ?, numBeds = ?, numBaths = ?, " +
                    "accommodates = ?, hasLivingRoom = ?, smokingAllowed = ?, petsAllowed = ?, eventsAllowed = ?, " +
                    "wifi = ?, airconditioning = ?, heating = ?, kitchen = ?, tv = ?, parking = ?, elevator = ?, " +
                    "area = ?, description = ?, instructions = ?, minDays = ?, " +
                    "dateFrom = ?, dateTo = ?, minCost = ?, costPerPerson = ?, costPerDay = ?, lastUpdated = NOW() WHERE houseID = ?";
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
            pSt.setDate(23, Helpers.DateHelper.stringToDate(he.getDateFrom()));
            pSt.setDate(24, Helpers.DateHelper.stringToDate(he.getDateTo()));
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
            Helpers.ConnectionCloser.closeAll(con, pSt, null);
        }
    }


    @Path("/uploadphoto/{houseId}")
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadPicture(@FormDataParam("file") InputStream uploadedInputStream,
                                  @FormDataParam("file") FormDataContentDisposition fileDetails,
                                  @FormDataParam("token") String token,
                                  @PathParam("houseId") int houseID) {

        List<String> scopes = Helpers.ScopeFiller.fillScope(Constants.TYPE_RENTER);
        Authenticator auth = new Authenticator(token, scopes);
        if (!auth.authenticate()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        int id = auth.getId();
        Connection con = null;
        try {
            con = DataSource.getInstance().getConnection();
            String fileUrl = Helpers.FileHelper.saveFile(uploadedInputStream, id, fileDetails,false);
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

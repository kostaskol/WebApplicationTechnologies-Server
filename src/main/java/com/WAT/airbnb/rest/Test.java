package com.WAT.airbnb.rest;

import com.WAT.airbnb.db.DataSource;
import com.WAT.airbnb.etc.Constants;
import com.WAT.airbnb.etc.DateRange;
import com.WAT.airbnb.etc.Helpers;
import com.WAT.airbnb.rest.entities.HouseMinEntity;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.*;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Path("/test")
public class Test {
    @Path("/image")
    @GET
    @Produces("image/jpg")
    public Response getImage() {
        try {
            List<FileInputStream> list = new ArrayList<>();
            for (int i = 1; i < 4; i++) {
                list.add(new FileInputStream(new File(Constants.DIR + "/img/houses/" + i + ".jpg")));
            }
            GenericEntity<List<FileInputStream>> entity = new GenericEntity<List<FileInputStream>>(list) {};
            return Response.ok(entity).build();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Path("/daterange")
    @GET
    public Response dateRange(@QueryParam("f") String from,
                              @QueryParam("t") String to) {
        try {
            DateRange range = new DateRange(
                    Helpers.DateHelper.stringToDate(from),
                    Helpers.DateHelper.stringToDate(to)
            );

            return Response.ok(range.toList().size()).build();
        } catch (ParseException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Path("/remover")
    @GET
    public Response remover() {
        ArrayList<LocalDate> dates = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            LocalDate d = LocalDate.now();
            dates.add(d);
        }

        dates.removeAll(Collections.singleton(LocalDate.now()));
        System.out.println(dates.size());
        return Response.ok().build();
    }

    @Path("/testcontains")
    @GET
    public Response testContains() {
        ArrayList<String> list = new ArrayList<>();
        list.add("Hello");
        list.add("World");
        System.out.println(list.contains("Hello"));
        return Response.ok().build();
    }

    @Path("/fixnullhouses")
    @GET
    public Response fixNullHouses() {
        Connection getCon = null;
        ResultSet rs = null;
        try {
            getCon = DataSource.getInstance().getConnection();
            String query = "SELECT houseID FROM houses WHERE tv IS NULL";
            Statement st = getCon.createStatement();
            rs = st.executeQuery(query);
            ArrayList<Integer> ids = new ArrayList<>();
            while (rs.next()) {
                ids.add(rs.getInt("houseID"));
            }

            for (Integer id : ids) {
                Connection con = null;
                try {
                    con = DataSource.getInstance().getConnection();
                    String update = "UPDATE houses SET tv = ? WHERE houseID = ? LIMIT 1";
                    PreparedStatement pSt = con.prepareStatement(update);
                    pSt.setBoolean(1, getRandomBoolean());
                    pSt.setInt(2, id);
                    pSt.execute();
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
                }
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

            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private Boolean getRandomBoolean() {
        return ThreadLocalRandom.current().nextInt(0, 100) >= 50;
    }
}

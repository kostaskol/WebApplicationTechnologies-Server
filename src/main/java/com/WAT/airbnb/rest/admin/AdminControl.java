package com.WAT.airbnb.rest.admin;

import com.WAT.airbnb.db.DataSource;
import com.WAT.airbnb.etc.Constants;
import com.WAT.airbnb.rest.Authenticator;
import com.WAT.airbnb.rest.entities.UserEntity;
import com.WAT.airbnb.rest.entities.UserMinEntity;
import com.WAT.airbnb.util.helpers.*;
import com.google.gson.Gson;
import com.jamesmurty.utils.XMLBuilder;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.sql.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Handles all operations for the admin
 * Paths:
 *  /verifytoken: Verifies the given token (Used to verify the admin cookie)
 *  /getusers: Returns an array of JSON of the basic info of all the users
 *  /alteruser/{userId}: Consumes a JSON object and alters the given user
 *  /approve/{userId}: Approved the given user
 *  /get/{userId}: Returns a JSON object of the full info of the given user
 *  /rawexport: Returns an XML string with all the data from the database
 *      Format: <Airbnb>
 *                  <Users>
 *                      <User>
 *                          <UserID>{userId}</UserID>
 *                          ...
 *                      </User>
 *                      ...
 *                  </Users>
 *                  <Houses>
 *                      <House>
 *                          <HouseID>{houseId}</HouseID>
 *                          ...
 *                      </House>
 *                      ...
 *                  </Houses>
 *                  <Comments>
 *                      <Comment>
 *                          <CommentID>{commentId}</CommentID>
 *                          ...
 *                      </Comment>
 *                      ...
 *                  </Comments>
 *                  <Messages>
 *                      <Message>
 *                          <MessageID>{messageId}</MessageID>
 *                          ...
 *                      </Message>
 *                      ...
 *                  </Messages>
 *                  <Bookings>
 *                      <Booking>
 *                          <BookingID>{bookingId}</BookingID>
 *                          ...
 *                      </Booking>
 *                      ...
 *                  </Bookings>
 *              </Airbnb>
 */
@Path("/admin")
public class AdminControl {

    @Path("/verifytoken")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response verifyToken(String token) {
        Authenticator auth = new Authenticator(token, Constants.TYPE_ADMIN);
        System.out.println("Token = " + token);
        try {
            auth.authenticate();
            return Response.ok().build();
        } catch (Exception e) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
    }

    @Path("/getusers")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getApprove(String token) {
        Authenticator auth = new Authenticator(token, Constants.TYPE_ADMIN);
        try {
            auth.authenticate();
        } catch (Exception e) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        Connection con = null;
        ResultSet rs = null;
        try {
            con = DataSource.getInstance().getConnection();
            String query = "SELECT userId, email, accType, firstName, lastName, approved, pictureURL FROM users";
            Statement st = con.createStatement();
            rs = st.executeQuery(query);
            ArrayList<UserMinEntity> users = new ArrayList<>();
            while (rs.next()) {
                if (rs.getString("email").equals("root")) continue;
                UserMinEntity user = new UserMinEntity();
                user.setUserId(rs.getInt("userId"));
                user.setEmail(rs.getString("email"));
                user.setAccType(rs.getString("accType"));
                user.setFirstName(rs.getString("firstName"));
                user.setLastName(rs.getString("lastName"));
                user.setApproved(rs.getBoolean("approved"));
                String base64;
                if (rs.getString("pictureURL") != null)
                    base64 = FileHelper.getFileAsString(rs.getString("pictureURL"));
                else
                    base64 = FileHelper.getFileAsString(Constants.DIR + "/img/users/profile_default.jpg");
                user.setPicture(base64);
                users.add(user);
            }

            Gson gson = new Gson();
            String json = gson.toJson(users);
            return Response.ok(json).build();
        } catch (IOException | SQLException e) {
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
        }
    }

    @Path("/alteruser/{userId}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response alterUser(@PathParam("userId") int userId,
                              String json) {
        Gson gson = new Gson();
        UserEntity entity = gson.fromJson(json, UserEntity.class);
        Authenticator auth = new Authenticator(entity.getToken(), Constants.TYPE_ADMIN);
        try {
            auth.authenticate();
        } catch (Exception e) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        Connection con = null;
        try {
            con = DataSource.getInstance().getConnection();
            String update = "UPDATE users SET " +
                    "email = ?, " +
                    "firstName = ?, " +
                    "lastName = ?, " +
                    "phoneNumber = ?, " +
                    "dateOfBirth = ?, " +
                    "country = ?, " +
                    "bio = ? WHERE userID = ?";
            PreparedStatement pSt = con.prepareStatement(update);
            pSt.setString(1, entity.getEmail());
            pSt.setString(2, entity.getFirstName());
            pSt.setString(3, entity.getLastName());
            pSt.setString(4, entity.getpNum());
            pSt.setDate(5, DateHelper.stringToDate(entity.getDateOfBirth()));
            pSt.setString(6, entity.getCountry());
            pSt.setString(7, entity.getBio());
            pSt.setInt(8, userId);

            pSt.execute();
            return Response.ok().build();
        } catch (SQLException | IOException | ParseException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Path("/approve/{userId}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response approve(@PathParam(value="userId") int userId,
                            String token) {
        Authenticator auth = new Authenticator(token, Constants.TYPE_ADMIN);
        try {
            auth.authenticate();
        } catch (Exception e) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        Connection con = null;
        try {
            con = DataSource.getInstance().getConnection();
            String update = "UPDATE users SET approved = 1 WHERE userID = ? LIMIT 1";
            PreparedStatement pSt = con.prepareStatement(update);
            pSt.setInt(1, userId);
            pSt.execute();
            return Response.ok().build();
        } catch (IOException | SQLException e) {
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

    @Path("/get/{userId}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUser(@PathParam(value="userId") int userId,
                            String token) {
        Authenticator auth = new Authenticator(token, Constants.TYPE_ADMIN);
        try {
            auth.authenticate();
        } catch (Exception e) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        Connection con = null;
        ResultSet rs = null;
        try {
            con = DataSource.getInstance().getConnection();
            String query = "SELECT * FROM users WHERE userId = ? LIMIT 1";
            PreparedStatement pSt = con.prepareStatement(query);
            pSt.setInt(1, userId);

            rs = pSt.executeQuery();
            if (rs.next()) {
                UserEntity entity = new UserEntity();
                entity.setEmail(rs.getString("email"));
                entity.setAccType(rs.getString("accType"));
                entity.setFirstName(rs.getString("firstName"));
                entity.setLastName(rs.getString("lastName"));
                entity.setpNum(rs.getString("phoneNumber"));
                entity.setBio(rs.getString("bio"));
                String base64;
                if (rs.getString("pictureURL") != null) {
                    base64 = FileHelper.getFileAsString(rs.getString("pictureURL"));
                } else {
                    base64 = FileHelper.getFileAsString(Constants.DIR + "/img/users/profile_default.jpg");
                }
                entity.setPicture(base64);
                entity.setCountry(rs.getString("country"));
                entity.setApproved(rs.getBoolean("approved"));
                entity.setDateOfBirth(DateHelper.dateToString(rs.getDate("dateOfBirth")));

                Gson gson = new Gson();
                String json = gson.toJson(entity);
                return Response.ok(json).build();


            } else {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
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
        }

    }


    @Path("/getapprovallist")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getApprovalList(String token) {
        Authenticator auth = new Authenticator(token, Constants.TYPE_ADMIN);
        try {
            auth.authenticate();
        } catch (Exception e) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        Connection con = null;
        ResultSet rs = null;
        try {
            con = DataSource.getInstance().getConnection();
            String query = "SELECT * FROM users WHERE approved = 0";
            Statement st = con.createStatement();
            rs = st.executeQuery(query);
            ArrayList<UserMinEntity> entities = new ArrayList<>();
            while (rs.next()) {
                UserMinEntity user = new UserMinEntity();
                user.setUserId(rs.getInt("userID"));
                user.setAccType(rs.getString("accType"));
                user.setApproved(false);
                user.setEmail(rs.getString("email"));
                user.setFirstName(rs.getString("firstName"));
                user.setLastName(rs.getString("lastName"));
                String path = rs.getString("pictureURL");
                if (path == null) {
                    path = Constants.DIR + "/img/users/profile_default.jpg";
                }
                String base64 = FileHelper.getFileAsString(path);
                user.setPicture(base64);
                entities.add(user);
            }

            Gson gson = new Gson();
            String json = gson.toJson(entities);
            return Response.ok(json).build();
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
        }
    }

    @Path("/getxml")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public Response getXml(@QueryParam("t") String t) {
        System.out.println("Token = " + t);
        Authenticator auth = new Authenticator(t);
        try {
            auth.authenticateExport();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        File file = new File(Constants.DIR + "/exp.xml");
        Response.ResponseBuilder responseBuilder = Response.ok((Object) file);
        responseBuilder.header("Content-Disposition", "attachment; filename=\"exported.xml\"");
        return responseBuilder.build();
    }


    @Path("/rawexport")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response exportRaw(String token) {
        Authenticator auth = new Authenticator(token, Constants.TYPE_ADMIN);
        try {
            auth.authenticate();
        } catch (Exception e) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        Connection con = null;
        ResultSet users = null,
                houses = null,
                comments = null,
                bookings = null,
                messages = null;
        try {
            con = DataSource.getInstance().getConnection();
            String query = "SELECT * FROM users";
            Statement userSt = con.createStatement();
            users = userSt.executeQuery(query);


            query = "SELECT * FROM houses";
            Statement houseSt = con.createStatement();
            houses = houseSt.executeQuery(query);


            query = "SELECT * FROM comments";
            Statement commentSt = con.createStatement();
            comments = commentSt.executeQuery(query);


            query = "SELECT * FROM messages";
            Statement mesSt = con.createStatement();
            messages = mesSt.executeQuery(query);


            query = "SELECT * FROM bookings";
            Statement bookSt = con.createStatement();
            bookings = bookSt.executeQuery(query);


            XMLBuilder xmlBuilder = XmlBuilder.getXml(users, houses, comments, bookings, messages);
            if (xmlBuilder == null) {
                throw new NullPointerException("XMLBuilder is null");
            }

            Properties outputProperties = new Properties();
            outputProperties.put(javax.xml.transform.OutputKeys.METHOD, "xml");
            PrintWriter writer = new PrintWriter(new FileOutputStream(Constants.DIR + "/exp.xml"));
            xmlBuilder.toWriter(writer, outputProperties);
            JwtBuilder builder = Jwts.builder()
                    .setExpiration(new Date(System.currentTimeMillis() + 180000)); // Give the browser 1 second to
                                                                                    // initiate the download
            return Response.ok(builder.signWith(SignatureAlgorithm.HS256, Constants.key).compact()).build();
        } catch (Exception e) {
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

            if (users != null) {
                try {
                    users.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            if (houses != null) {
                try {
                    houses.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            if (comments != null) {
                try {
                    comments.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            if (bookings != null) {
                try {
                    bookings.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            if (messages != null) {
                try {
                    messages.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

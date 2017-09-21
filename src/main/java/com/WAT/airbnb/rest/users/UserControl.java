package com.WAT.airbnb.rest.users;

import com.WAT.airbnb.db.DataSource;
import com.WAT.airbnb.etc.*;
import com.WAT.airbnb.rest.Authenticator;
import com.WAT.airbnb.rest.entities.*;
import com.WAT.airbnb.rest.jacksonClasses.authentication.LoginBean;
import com.WAT.airbnb.util.XmlParser;
import com.WAT.airbnb.util.blacklist.BlackList;
import com.WAT.airbnb.util.helpers.ConnectionCloser;
import com.WAT.airbnb.util.helpers.DateHelper;
import com.WAT.airbnb.util.helpers.FileHelper;
import com.google.gson.Gson;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.sql.*;
import java.util.Base64;

/**
 * Handles all user functionality
 * Paths:
 *  /signup: Consumes a JSON and inserts the data
 */
@Path("/user")
public class UserControl {

    private class UserInfo {
        int id;
        int accountType;
    }

    @Path("/signup")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response signUp(SignUpBean bean) {
        Password pass;
        try {
            pass = new Password(bean.getPasswd(), false);
            pass.hash();
        } catch (RuntimeException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        try {
            Connection con = DataSource.getInstance().getConnection();
            String query = "insert into users (" +
                    "email, passwd, accType, firstName, lastName, phoneNumber, dateOfBirth, approved" +
                    ") values (?, ?, ?, ?, ?, ?, ?, false)";
            System.out.println("Email: " + bean.getEmail());
            PreparedStatement pStatement = con.prepareStatement(query);
            pStatement.setString(1, bean.getEmail());
            pStatement.setString(2, pass.getHash());
            pStatement.setString(3, bean.getAccountType());
            pStatement.setString(4, bean.getFirstName());
            pStatement.setString(5, bean.getLastName());
            pStatement.setString(6, bean.getPhoneNumber());
            Date sqlDate = DateHelper.stringToDate(bean.getDateOfBirth());
            pStatement.setDate(7, sqlDate);
            pStatement.execute();
            return Response.ok().build();
        } catch (SQLException sqle) {
            sqle.printStackTrace();
            if (sqle.getErrorCode() == 1062) {
                XmlParser parser = new XmlParser(Constants.DIR + "/Constants.xml");
                Integer code = parser.getCode("signup-codes", "mail-exists");
                if (code == null) {
                    System.out.println("Code is null");
                } else {
                    int tmp = code;
                    System.out.println("Code = " + code);
                    JsonObject object = Json.createObjectBuilder()
                            .add("err-code", tmp)
                            .build();
                    System.out.println("Returning response " + object);
                    return Response.ok(object.toString()).build();
                }

            }
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    @Path("/login")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response authenticate(LoginBean object) {
        try {
            UserInfo info = authenticateAgainstDB(object.getEmail(), object.getPasswd());
            if (info == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }

            String token = issueToken(info);

            String response = Json.createObjectBuilder()
                    .add("token", token).build().toString();
            return Response.ok(response).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
    }

    @Path("/passreset")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response resetPass(String json) {
        Gson gson = new Gson();
        PassResetEntity resetEntity = gson.fromJson(json, PassResetEntity.class);

        Password pass;
        try {
            pass = new Password(resetEntity.getPassword(), false);
            pass.hash();
        } catch(RuntimeException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        Connection con = null;
        try {
            con = DataSource.getInstance().getConnection();
            String update = "UPDATE users SET passwd = ? WHERE email = ?";
            PreparedStatement pSt = con.prepareStatement(update);
            pSt.setString(1, pass.getHash());
            pSt.setString(2, resetEntity.getEmail());
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

    @Path("/getuser/{userId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUser(@PathParam("userId") int userId) {
        Connection con = null;
        PreparedStatement pSt = null;
        ResultSet rs = null;
        try {
            con = DataSource.getInstance().getConnection();
            String query = "SELECT email, firstName, lastName, country, bio, pictureURL FROM users WHERE userID = ? LIMIT 1";
            pSt = con.prepareStatement(query);
            pSt.setInt(1, userId);
            rs = pSt.executeQuery();
            UserEntity entity = new UserEntity();
            if (rs.next()) {
                entity.setFirstName(rs.getString("firstName"));
                entity.setLastName(rs.getString("lastName"));
                entity.setEmail(rs.getString("email"));
                entity.setCountry(rs.getString("country"));
                entity.setBio(rs.getString("bio"));
                entity.setPicture(FileHelper.getFileAsString(rs.getString("pictureURL")));
            }
            Gson gson = new Gson();
            return Response.ok(gson.toJson(entity)).build();
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            ConnectionCloser.closeAll(con, pSt, rs);
        }
    }

    @Path("getuser")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUser(String token) {
        Authenticator auth = new Authenticator(token, Constants.TYPE_USER);

        try {
            auth.authenticate();
        } catch (Exception e) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        int id = auth.getId();
        Connection con = null;

        boolean enoughData;
        Connection dCon = null;
        PreparedStatement dPSt = null;
        ResultSet dRs = null;
        try {
            dCon = DataSource.getInstance().getConnection();
            String query = "SELECT COUNT(*) AS c FROM comments WHERE userID = ?";
            dPSt = dCon.prepareStatement(query);
            dPSt.setInt(1, id);
            dRs = dPSt.executeQuery();
            if (dRs.next()) {
                if (dRs.getInt("c") > 5) {
                    enoughData = true;
                } else {
                    enoughData = false;
                }
            } else {
                enoughData = false;
            }
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            ConnectionCloser.closeAll(dCon, dPSt, dRs);
        }
        try {
            con = DataSource.getInstance().getConnection();
            String query = "SELECT accType, approved, firstName, lastName, phoneNumber, bio, pictureURL FROM users WHERE userID = ?";
            PreparedStatement pStatement = con.prepareStatement(query);
            pStatement.setInt(1, id);
            ResultSet rs = pStatement.executeQuery();
            if (!rs.next()) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }

            String type = rs.getString("accType");
            boolean approved = rs.getBoolean("approved");
            String first = rs.getString("firstName");
            String last = rs.getString("lastName");
            String num = rs.getString("phoneNumber");
            String localUrl = rs.getString("pictureURL");
            String bio = rs.getString("bio");

            if (localUrl == null) {
                localUrl = Constants.DIR + "/img/users/profile_default.jpg";
            }

            File file = new File(localUrl);
            String encoded = Base64.getEncoder().withoutPadding().encodeToString(Files.readAllBytes(file.toPath()));

            UserEntity user = new UserEntity();
            user.setAccType(type);
            user.setApproved(approved);
            user.setFirstName(first);
            user.setLastName(last);
            user.setPicture(encoded);
            user.setpNum(num);
            user.setBio(bio);
            user.setEnoughData(enoughData);
            return Response.ok(user).build();
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

    @Path("/updateuser")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateUser(String json) {
        System.out.println(json);
        Gson gson = new Gson();
        UserUpdateEntity entity = gson.fromJson(json, UserUpdateEntity.class);
        Authenticator auth = new Authenticator(entity.getToken(), Constants.TYPE_USER);
        if (!auth.authenticate()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        int userId = auth.getId();

        Connection con = null;
        PreparedStatement pSt = null;
        try {
            con = DataSource.getInstance().getConnection();
            String update = "UPDATE users SET phoneNumber = ?, bio = ? WHERE userID = ?";
            pSt = con.prepareStatement(update);
            pSt.setString(1, entity.getpNum());
            pSt.setString(2, entity.getBio());
            pSt.setInt(3, userId);
            pSt.execute();
            return Response.ok().build();
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            ConnectionCloser.closeAll(con, pSt, null);
        }
    }

    @Path("/verifytoken")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response verifyToken(String token) {
        Authenticator auth = new Authenticator(token, Constants.TYPE_USER);
        if (!auth.authenticate()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        return Response.ok().build();
    }

    @Path("/changemail")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changeEmail(String json) {
        Gson gson = new Gson();
        ChangeMailEntity entity = gson.fromJson(json, ChangeMailEntity.class);
        Authenticator auth = new Authenticator(entity.getToken(), Constants.TYPE_USER);
        if (!auth.authenticate()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        int userId = auth.getId();

        Connection con = null;
        try {
            con = DataSource.getInstance().getConnection();
            String update = "UPDATE users SET email = ? WHERE userID = ?";
            PreparedStatement pSt = con.prepareStatement(update);
            pSt.setString(1, entity.getNewMail());
            pSt.setInt(2, userId);
            pSt.execute();
            return Response.ok().build();
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

    @Path("/updateprofile")
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response updateProfile(@FormDataParam("file") InputStream uploadedInputStream,
                                  @FormDataParam("file") FormDataContentDisposition fileDetails,
                                  @FormDataParam("token") String token) {
        Authenticator auth = new Authenticator(token, Constants.TYPE_USER);
        if (!auth.authenticate()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        int userId = auth.getId();
        Connection con = null;
        PreparedStatement pSt = null;
        try {
            String localUrl = FileHelper.saveFile(uploadedInputStream, userId, fileDetails, true);
            con = DataSource.getInstance().getConnection();
            String update = "UPDATE users SET pictureURL = ? WHERE userID = ?";
            pSt = con.prepareStatement(update);
            pSt.setString(1, localUrl);
            pSt.setInt(2, userId);
            pSt.execute();
            return Response.ok().build();
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            ConnectionCloser.closeAll(con, pSt, null);
        }
    }

    @Path("/invalidate")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response invalidateToken(String token) {
        BlackList blackList = BlackList.getInstance();
        try {
            blackList.addToList(token, Constants.TYPE_USER);
        } catch (IOException | SQLException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.ok().build();
    }

    private UserInfo authenticateAgainstDB(String email, String passwd) throws Exception {
        Connection con = null;
        ResultSet rs = null;
        System.out.println("Authenticating user");
        UserInfo info = new UserInfo();
        try {
            con = DataSource.getInstance().getConnection();

            String queryString = "SELECT * FROM users WHERE email = ? LIMIT 1";
            PreparedStatement query = con.prepareStatement(queryString);

            query.setString(1, email);
            rs = query.executeQuery();
            if (!rs.next()) {
                System.out.println("No results");
                return null;
            } else {
                System.out.println(rs.getString("passwd"));
                Password pass = new Password(rs.getString("passwd"), true);
                try {
                    if (pass.verify(passwd)) {
                        System.out.println("Verified");
                        info.id = rs.getInt("userID");
                        String accType = rs.getString("accType");
                        if (accType.charAt(Constants.ADMIN_OFFS) == '1') {
                            info.accountType = Constants.TYPE_ADMIN;
                        } else if (accType.charAt(Constants.RENTER_OFFS) == '1') {
                            info.accountType = Constants.TYPE_RENTER;
                        } else if (accType.charAt(Constants.USER_OFFS) == '1') {
                            info.accountType = Constants.TYPE_USER;
                        }
                        return info;
                    } else {
                        System.out.println("Not verified");
                        return null;
                    }
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    return null;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }finally {
            if (con != null) {
                try {
                    DataSource.getInstance().printCon();
                    con.close();
                    DataSource.getInstance().printCon();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String issueToken(UserInfo info) {
        System.out.println("Building token");

        JwtBuilder builder = Jwts.builder()
                .setSubject("users/" + info.id)
                .claim("id", String.valueOf(info.id));
        long expMillis;
        switch (info.accountType) {
            case Constants.TYPE_ADMIN:
                builder.claim("scope", Constants.SCOPE_ADMINS);
                // For admins, we set a 5 minute reset on the token
                expMillis = System.currentTimeMillis() + Constants.ADMIN_EXPIRATION_TIME;
                break;
            case Constants.TYPE_RENTER:
                builder.claim("scope", Constants.SCOPE_RENTERS);
                expMillis = System.currentTimeMillis() + Constants.EXPIRATION_TIME_ALL;
                break;
            case Constants.TYPE_USER:
                builder.claim("scope", Constants.SCOPE_USERS);
                expMillis = System.currentTimeMillis() + Constants.EXPIRATION_TIME_ALL;
                break;
            default:
                System.err.println("Unknown user type");
                return null;
        }
        builder.setExpiration(new Date(expMillis));

        return builder.signWith(SignatureAlgorithm.HS256, Constants.key).compact();
    }



}

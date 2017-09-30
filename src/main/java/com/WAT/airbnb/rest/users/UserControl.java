package com.WAT.airbnb.rest.users;

import com.WAT.airbnb.db.DataSource;
import com.WAT.airbnb.etc.*;
import com.WAT.airbnb.rest.Authenticator;
import com.WAT.airbnb.rest.entities.UserCredentialBean;
import com.WAT.airbnb.rest.entities.SignUpBean;
import com.WAT.airbnb.rest.entities.PassResetBean;
import com.WAT.airbnb.rest.entities.UserBean;
import com.WAT.airbnb.rest.entities.UserUpdateBean;
import com.WAT.airbnb.rest.entities.ChangeMailBean;
import com.WAT.airbnb.util.XmlParser;
import com.WAT.airbnb.util.blacklist.BlackList;
import com.WAT.airbnb.util.helpers.ConnectionCloser;
import com.WAT.airbnb.util.helpers.DateHelper;
import com.WAT.airbnb.util.helpers.FileHelper;
import com.WAT.airbnb.util.passwordverifier.PasswordVerifier;
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
 *  /signup
 *  /login
 *  /passreset
 *  /getuser/{userId}
 *  /getuser
 *  /updateuser
 *  /verifytoken
 *  /changemail
 *  /updateprofilepicture
 */
@Path("/user")
public class UserControl {

    /**
     * Simple inner class that passes around a small part of a user's information
     */
    private class UserInfo {
        int id;
        int accountType;
    }

    /**
     * Inserts the user into the database
     */
    @Path("/signup")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response signUp(SignUpBean bean) {
        PasswordVerifier pass;
        // Hash the provided password and store the hash into the database
        try {
            pass = new PasswordVerifier(bean.getPasswd(), false);
            pass.hash();
        } catch (RuntimeException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        Connection con = null;
        PreparedStatement pSt = null;
        try {
            con = DataSource.getInstance().getConnection();
            String query = "insert into users " +
                    "(email, passwd, accType, firstName, lastName, " +
                    "phoneNumber, dateOfBirth, approved) " +
                    "values (?, ?, ?, ?, ?, ?, ?, false)";
            pSt = con.prepareStatement(query);
            pSt.setString(1, bean.getEmail());
            pSt.setString(2, pass.getHash());
            pSt.setString(3, bean.getAccountType());
            pSt.setString(4, bean.getFirstName());
            pSt.setString(5, bean.getLastName());
            pSt.setString(6, bean.getPhoneNumber());
            Date sqlDate = DateHelper.stringToDate(bean.getDateOfBirth());
            pSt.setDate(7, sqlDate);
            pSt.execute();
            return Response.ok().build();
        } catch (SQLException sqle) {
            sqle.printStackTrace();
            if (sqle.getErrorCode() == 1062) {
                XmlParser parser = new XmlParser(Constants.DIR + "/Constants.xml");
                Integer code = parser.getCode("signup-codes", "mail-exists");
                if (code != null) {
                    JsonObject object = Json.createObjectBuilder()
                            .add("err-code", code)
                            .build();
                    return Response.ok(object.toString()).build();
                }

            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            ConnectionCloser.getCloser()
                    .closeConnection(con)
                    .closeStatement(pSt);
        }
    }

    /**
     * Tries to match the given credentials against the database and returns according results
     */
    @Path("/login")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response authenticate(UserCredentialBean bean) {
        System.out.println("Got mail: " + bean.getEmail() + " and password: " + bean.getPasswd());
        try {
            UserInfo info = authenticateAgainstDB(bean.getEmail(), bean.getPasswd());
            if (info == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }

            String token = issueToken(info);

            return Response.ok(token).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     *
     * @param email Email to be checked
     * @param passwd User provided password to be checked
     * @return A UserInfo object on successful authentication null on failure
     * @throws Exception At any error
     */
    private UserInfo authenticateAgainstDB(String email, String passwd) throws Exception {
        Connection con = null;
        PreparedStatement pSt = null;
        ResultSet rs = null;
        UserInfo info = new UserInfo();
        try {
            con = DataSource.getInstance().getConnection();

            String queryString = "SELECT userID, passwd, accType " +
                    "FROM users " +
                    "WHERE email = ? " +
                    "LIMIT 1";
            pSt = con.prepareStatement(queryString);

            pSt.setString(1, email);
            rs = pSt.executeQuery();
            if (!rs.next()) {
                return null;
            } else {
                // Tries to verify the hashed password
                PasswordVerifier pass = new PasswordVerifier(rs.getString("passwd"), true);
                try {
                    if (pass.verify(passwd)) {
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
                        return null;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }finally {
            ConnectionCloser.getCloser()
                    .closeConnection(con)
                    .closeResultSet(rs);
        }
    }

    /**
     * Issues a JWT token to the provided userID
     * @return The JWT token
     */
    private String issueToken(UserInfo info) {

        JwtBuilder builder = Jwts.builder()
                .setSubject("users/" + info.id)
                .claim("id", String.valueOf(info.id));
        long expMillis;
        switch (info.accountType) {
            case Constants.TYPE_ADMIN:
                builder.claim("scope", Constants.SCOPE_ADMINS);
                // For admins, we set a 3 minute reset on the token
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



    /**
     * Resets the user's current password
     */
    @Path("/passreset")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response resetPass(String json) {
        Gson gson = new Gson();
        PassResetBean resetEntity = gson.fromJson(json, PassResetBean.class);

        PasswordVerifier pass;
        try {
            pass = new PasswordVerifier(resetEntity.getPassword(), false);
            pass.hash();
        } catch(RuntimeException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        Connection con = null;
        PreparedStatement pSt = null;
        try {
            con = DataSource.getInstance().getConnection();
            String update = "UPDATE users SET passwd = ? WHERE email = ?";
            pSt = con.prepareStatement(update);
            pSt.setString(1, pass.getHash());
            pSt.setString(2, resetEntity.getEmail());
            pSt.execute();
            return Response.ok().build();
        } catch (IOException | SQLException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            ConnectionCloser.getCloser()
                    .closeConnection(con)
                    .closeStatement(pSt);
        }
    }

    /**
     * @return The specified user's public information
     */
    @Path("/getuser/{userId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUser(@PathParam("userId") int userId) {
        Connection con = null;
        PreparedStatement pSt = null;
        ResultSet rs = null;
        try {
            con = DataSource.getInstance().getConnection();
            String query = "SELECT email, firstName, lastName, country, bio, pictureURL " +
                    "FROM users " +
                    "WHERE userID = ? " +
                    "LIMIT 1";
            pSt = con.prepareStatement(query);
            pSt.setInt(1, userId);
            rs = pSt.executeQuery();
            UserBean entity = new UserBean();
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
            ConnectionCloser.getCloser()
                    .closeAll(con, pSt, rs);
        }
    }

    /**
     * @return The current user's information
     */
    @Path("getuser")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUser(String token) {
        Authenticator auth = new Authenticator(token, Constants.TYPE_USER);

        if (!auth.authenticate()) {
            return Response
                    .status(Response.Status.UNAUTHORIZED)
                    .build();
        }

        int id = auth.getId();

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

            enoughData = dRs.next() && (dRs.getInt("c") > 5);
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            ConnectionCloser.getCloser()
                    .closeAll(dCon, dPSt, dRs);
        }
        Connection con = null;
        PreparedStatement pSt = null;
        ResultSet rs = null;
        try {
            con = DataSource.getInstance().getConnection();
            String query = "SELECT accType, approved, firstName, lastName, phoneNumber, bio, pictureURL FROM users WHERE userID = ?";
            pSt = con.prepareStatement(query);
            pSt.setInt(1, id);
            rs = pSt.executeQuery();
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

            UserBean user = new UserBean();
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
            ConnectionCloser.getCloser()
                    .closeAll(con, pSt, rs);
        }
    }


    @Path("/updateuser")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateUser(String json) {
        Gson gson = new Gson();
        UserUpdateBean entity = gson.fromJson(json, UserUpdateBean.class);
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
            ConnectionCloser.getCloser()
                    .closeAll(con, pSt, null);
        }
    }

    /**
     * Verifies the provided token and returns accordingly
     */
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

    /**
     * Changes the user's email to the provided one
     */
    @Path("/changemail")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changeEmail(String json) {
        Gson gson = new Gson();
        ChangeMailBean entity = gson.fromJson(json, ChangeMailBean.class);
        Authenticator auth = new Authenticator(entity.getToken(), Constants.TYPE_USER);
        if (!auth.authenticate()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        int userId = auth.getId();

        Connection con = null;
        PreparedStatement pSt = null;
        try {
            con = DataSource.getInstance().getConnection();
            String update = "UPDATE users SET email = ? WHERE userID = ?";
            pSt = con.prepareStatement(update);
            pSt.setString(1, entity.getNewMail());
            pSt.setInt(2, userId);
            pSt.execute();
            return Response.ok().build();
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            ConnectionCloser.getCloser()
                    .closeConnection(con)
                    .closeStatement(pSt);
        }
    }

    /**
     * Updates the user's profile picture
     */
    @Path("/updateprofilepicture")
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response updateProfile(@FormDataParam("file") InputStream uploadedInputStream,
                                  @FormDataParam("file") FormDataContentDisposition fileDetails,
                                  @FormDataParam("token") String token) {
        Authenticator auth = new Authenticator(token, Constants.TYPE_USER);
        if (!auth.authenticate()) {
            return Response
                    .status(Response.Status.UNAUTHORIZED)
                    .build();
        }

        int userId = auth.getId();
        Connection con = null;
        PreparedStatement pSt = null;
        try {
            String localUrl = FileHelper.saveFile(uploadedInputStream, userId, fileDetails,
                    true);
            con = DataSource.getInstance().getConnection();
            String update = "UPDATE users SET pictureURL = ? WHERE userID = ?";
            pSt = con.prepareStatement(update);
            pSt.setString(1, localUrl);
            pSt.setInt(2, userId);
            pSt.execute();
            return Response.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            ConnectionCloser.getCloser()
                    .closeAll(con, pSt, null);
        }
    }

    /**
     * Invalidates the provided token
     */
    @Path("/invalidate")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response invalidateToken(String token) {
        // Adds the token to the blacklist
        BlackList blackList = BlackList.getInstance();
        try {
            blackList.addToList(token, Constants.TYPE_USER);
        } catch (IOException | SQLException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.ok().build();
    }
}

package com.WAT.airbnb.image_handler;

import com.WAT.airbnb.db.DataSource;
import com.WAT.airbnb.etc.Constants;
import com.WAT.airbnb.etc.Helpers;
import com.WAT.airbnb.rest.Authenticator;
import com.WAT.airbnb.rest.entities.AuthenticationEntity;
import io.jsonwebtoken.SignatureException;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.naming.AuthenticationException;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.List;

@Path("/profilecontrol")
public class ProfilePictureHandler {
    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadPicture(
            @FormDataParam("file") InputStream uploadedInputStream,
            @FormDataParam("file") FormDataContentDisposition fileDetails,
            @FormDataParam("token") String token) {
        List<String> scopes = Helpers.ScopeFiller.fillScope(Constants.TYPE_USER);
        Authenticator auth = new Authenticator(token, scopes);
        try {
            auth.authenticate();
        } catch (Exception e) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        int id = auth.getId();
        Connection con = null;
        try {
            System.out.println("Filename: " + fileDetails.getFileName());
            String fileUrl = Helpers.FileHelper.saveFile(uploadedInputStream, id, fileDetails,true);

            con = DataSource.getInstance().getConnection();
            String query = "UPDATE users SET pictureURL = ? WHERE userID = ? LIMIT 1";
            PreparedStatement pStatement = con.prepareStatement(query);
            pStatement.setString(1, fileUrl);
            pStatement.setInt(2, id);
            pStatement.executeUpdate();
            return Response.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @POST
    @Path("/download")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces("image/jpg")
    public Response downloadPicture(AuthenticationEntity authEnt) {
        List<String> scopes = Helpers.ScopeFiller.fillScope(Constants.TYPE_USER);
        Authenticator auth = new Authenticator(authEnt.getToken(), scopes);
        try {
            auth.authenticate();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        int id = auth.getId();
        Connection con = null;
        try {
            con = DataSource.getInstance().getConnection();
            String query = "SELECT pictureURL FROM users WHERE userID = ?";
            PreparedStatement pStatement = con.prepareStatement(query);
            pStatement.setInt(1, id);
            ResultSet rs = pStatement.executeQuery();
            if (rs.next()) {
                String localUrl = rs.getString("pictureURL");
                if (localUrl == null) {
                    localUrl = Constants.DIR + "/img/users/profile_default.jpg";
                }

                String encoded = Helpers.FileHelper.getFileAsString(localUrl);

                return Response.ok(encoded).build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
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

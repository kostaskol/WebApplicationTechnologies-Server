package com.WAT.airbnb.rest.houses.comments;

import com.WAT.airbnb.db.DataSource;
import com.WAT.airbnb.etc.Constants;
import com.WAT.airbnb.rest.Authenticator;
import com.WAT.airbnb.rest.entities.CommentEntity;
import com.WAT.airbnb.util.helpers.ScopeFiller;
import com.google.gson.Gson;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Path("/comment")
public class CommentControl {
    @Path("/new/{houseId}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response newComment(@PathParam("houseId") int houseId,
                               String json) {
        Gson gson = new Gson();
        CommentEntity comment = gson.fromJson(json, CommentEntity.class);
        List<String> scopes = ScopeFiller.fillScope(Constants.TYPE_USER);
        Authenticator auth = new Authenticator(comment.getToken(), scopes);

        if (!auth.authenticate()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        int userId = auth.getId();
        Connection con = null;

        try {
            con = DataSource.getInstance().getConnection();
            String query = "SELECT userID FROM comments WHERE userID = ? and houseID = ? LIMIT 1";
            PreparedStatement pSt = con.prepareStatement(query);
            pSt.setInt(1, userId);
            pSt.setInt(2, houseId);
            ResultSet rs = pSt.executeQuery();
            if (rs.next()) {
                return Response.ok("{\"status\": \"commented\"").build();
            }
            con = DataSource.getInstance().getConnection();
            String insert = "INSERT INTO comments (userID, houseID, comm, rating) VALUES (?, ?, ?, ?)";
            pSt = con.prepareStatement(insert);
            pSt.setInt(1, userId);
            pSt.setInt(2, houseId);
            pSt.setString(3, comment.getComment());
            pSt.setFloat(4, comment.getRating());
            pSt.executeUpdate();
            return Response.ok().build();
        } catch (IOException | SQLException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Path("/edit/{commentId}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response edit(@PathParam("commentId") int commentId,
                         String json) {
        Gson gson = new Gson();
        CommentEntity comment = gson.fromJson(json, CommentEntity.class);
        List<String> scopes = ScopeFiller.fillScope(Constants.TYPE_USER);
        Authenticator auth = new Authenticator(comment.getToken(), scopes);

        if (!auth.authenticate()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        Connection con = null;
        try {
            con = DataSource.getInstance().getConnection();
            String update = "UPDATE comments SET comm = ?, rating = ? WHERE commentId = ?";
            PreparedStatement pSt = con.prepareStatement(update);
            pSt.setString(1, comment.getComment());
            pSt.setFloat(2, comment.getRating());
            pSt.setInt(3, commentId);
            pSt.executeUpdate();
            return Response.ok().build();
        } catch (IOException | SQLException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Path("/get/{houseId}")
    @GET
    public Response getAll(@PathParam("houseId") int houseId) {
        Connection con = null;
        ResultSet rs = null;
        List<CommentEntity> entities = new ArrayList<>();
        try {
            con = DataSource.getInstance().getConnection();
            String query = "SELECT * FROM comments WHERE houseID = ?";
            PreparedStatement pSt = con.prepareStatement(query);
            pSt.setInt(1, houseId);
            rs = pSt.executeQuery();
            while (rs.next()) {
                CommentEntity comment = new CommentEntity();
                comment.setCommentId(rs.getInt("commentID"));
                comment.setUserId(rs.getInt("userID"));
                comment.setHouseId(houseId);
                comment.setComment(rs.getString("comm"));
                comment.setRating(rs.getFloat("rating"));

                Connection nameCon = DataSource.getInstance().getConnection();
                query = "SELECT firstName, lastName FROM users WHERE userID = ? LIMIT 1";
                pSt = nameCon.prepareStatement(query);
                pSt.setInt(1, rs.getInt("userID"));
                ResultSet nameRs = pSt.executeQuery();
                if (nameRs.next()) {
                    comment.setUserFName(nameRs.getString("firstName"));
                    comment.setUserLName(nameRs.getString("lastName"));
                }
                try {
                    nameCon.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                try {
                    nameRs.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                entities.add(comment);
            }

            Gson gson = new Gson();
            String response = gson.toJson(entities);

            return Response.ok(response).build();
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
}

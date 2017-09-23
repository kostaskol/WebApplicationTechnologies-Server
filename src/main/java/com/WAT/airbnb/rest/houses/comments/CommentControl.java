package com.WAT.airbnb.rest.houses.comments;

import com.WAT.airbnb.db.DataSource;
import com.WAT.airbnb.etc.Constants;
import com.WAT.airbnb.rest.Authenticator;
import com.WAT.airbnb.rest.entities.CommentBean;
import com.WAT.airbnb.util.helpers.ConnectionCloser;
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

/**
 * Handles all comment operations
 * Paths:
 * /new/{houseId}
 * /edit/{commentId} -- Not implemented client side
 * /get/{houseId}
 * @author Kostas Kolivas
 * @version 1.0
 */
@Path("/comment")
public class CommentControl {

    /**
     *  Creates a new comment for the specified house
     */
    @Path("/new/{houseId}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response newComment(@PathParam("houseId") int houseId,
                               String json) {
        Gson gson = new Gson();
        CommentBean comment = gson.fromJson(json, CommentBean.class);
        Authenticator auth = new Authenticator(comment.getToken(), Constants.TYPE_USER);

        if (!auth.authenticate()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        int userId = auth.getId();
        Connection con = null;
        PreparedStatement pSt = null;
        ResultSet rs = null;
        try {
            con = DataSource.getInstance().getConnection();
            String query = "SELECT userID FROM comments WHERE userID = ? and houseID = ? LIMIT 1";
            pSt = con.prepareStatement(query);
            pSt.setInt(1, userId);
            pSt.setInt(2, houseId);
            rs = pSt.executeQuery();
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
        } finally {
            ConnectionCloser.getCloser().closeAll(con, pSt, rs);
        }
    }


    /**
     * ::- Not Implemented client - side -::
     * Allowed the user to edit their comment
     */
    @Path("/edit/{commentId}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response edit(@PathParam("commentId") int commentId,
                         String json) {
        Gson gson = new Gson();
        CommentBean comment = gson.fromJson(json, CommentBean.class);
        Authenticator auth = new Authenticator(comment.getToken(), Constants.TYPE_USER);

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

    /**
     * Returns a list of the house's comments
     */
    @Path("/get/{houseId}")
    @GET
    public Response getAll(@PathParam("houseId") int houseId) {
        Connection con = null;
        PreparedStatement pSt = null;
        ResultSet rs = null;
        List<CommentBean> entities = new ArrayList<>();
        try {
            con = DataSource.getInstance().getConnection();
            String query = "SELECT * FROM comments WHERE houseID = ?";
            pSt = con.prepareStatement(query);
            pSt.setInt(1, houseId);
            rs = pSt.executeQuery();
            while (rs.next()) {
                CommentBean comment = new CommentBean();
                comment.setCommentId(rs.getInt("commentID"));
                comment.setUserId(rs.getInt("userID"));
                comment.setHouseId(houseId);
                comment.setComment(rs.getString("comm"));
                comment.setRating(rs.getFloat("rating"));

                Connection nameCon = null;
                PreparedStatement nameSt = null;
                ResultSet nameRs = null;
                query = "SELECT firstName, lastName FROM users WHERE userID = ? LIMIT 1";
                try {
                    nameCon = DataSource.getInstance().getConnection();
                    nameSt = nameCon.prepareStatement(query);
                    nameSt.setInt(1, rs.getInt("userID"));
                    nameRs = nameSt.executeQuery();
                    if (nameRs.next()) {
                        comment.setUserFName(nameRs.getString("firstName"));
                        comment.setUserLName(nameRs.getString("lastName"));
                    }
                } catch (SQLException | IOException e) {
                    e.printStackTrace();
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
                } finally {
                    ConnectionCloser.getCloser().closeAll(nameCon, nameSt, nameRs);
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
            ConnectionCloser.getCloser().closeAll(con, pSt, rs);
        }
    }
}

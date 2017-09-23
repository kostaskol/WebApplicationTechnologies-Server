package com.WAT.airbnb.rest.users.messages;

import com.WAT.airbnb.db.DataSource;
import com.WAT.airbnb.etc.Constants;
import com.WAT.airbnb.rest.Authenticator;
import com.WAT.airbnb.rest.entities.MessageBean;
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
 * Handles all messaging operations
 * Paths:
 * /send/{receiverId}
 * /delete/{type}/{messageId}
 * /receive/{received}
 */
@Path("/message")
public class MessageControl {
    /**
     * Creates a new message in the database
     */
    @Path("/send/{receiverId}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response sendMessage(@PathParam("receiverId") int recId,
                                String json) {
        Gson gson = new Gson();
        MessageBean message = gson.fromJson(json, MessageBean.class);
        Authenticator auth = new Authenticator(message.getToken(), Constants.TYPE_USER);
        if (!auth.authenticate()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        int senderId = auth.getId();
        Connection con = null;
        try {
            con = DataSource.getInstance().getConnection();
            String insert = "INSERT INTO messages " +
                    "(senderId, receiverID, subject, message, deleted)" +
                    "VALUES (?, ?, ?, ?, ?)";
            PreparedStatement pSt = con.prepareStatement(insert);
            pSt.setInt(1, senderId);
            pSt.setInt(2, recId);
            pSt.setString(3, message.getSubject());
            pSt.setString(4, message.getMessage());
            pSt.setString(5, "00");
            pSt.executeUpdate();
            return Response.ok().build();
        } catch (SQLException | IOException e) {
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

    /**
     * In the messages table in the DB, there exists a column named deleted of type char(2)
     * which holds 3 possible combinations:
     * 00 - Neither the sender or receiver of the message have deleted this message
     * 01 - The sender has deleted the message (and thus should not receive it in the message
     *      list anymore)
     * 10 - The receiver has deleted the message (and thus should not receive it in the message
     *      list anymore)
     * @param type The type of the user that deletes the message (saves us an access to the databse
     *             since the client knows if it was deleted from the received or sent messages list)
     */
    @Path("/delete/{type}/{messageId}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response deleteMessage(
            @PathParam("type") int type,
            @PathParam("messageId") int messageId,
            String token) {
        Authenticator auth = new Authenticator(token, Constants.TYPE_USER);

        if (!auth.authenticate()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        Connection con = null;
        PreparedStatement pSt = null;
        ResultSet rs = null;

        try {
            con = DataSource.getInstance().getConnection();
            String query = "SELECT deleted FROM messages WHERE messageId = ? LIMIT 1";
            pSt = con.prepareStatement(query);
            pSt.setInt(1, messageId);
            rs = pSt.executeQuery();
            if (rs.next()) {
                String savedType = rs.getString("deleted");
                String statement;
                PreparedStatement pStatement;
                switch(type) {
                    case Constants.MESSAGE_RECEIVER:
                        if (savedType.charAt(Constants.MESSAGE_SENDER) == '1') {
                            // Both the sender and the receiver have deleted this message,
                            // so it is safe to delete it
                            statement = "DELETE FROM messages WHERE messageID = ? LIMIT 1";
                            pStatement = con.prepareStatement(statement);
                            pStatement.setInt(1, messageId);
                        } else {
                            // Only the receiver has deleted the message, so we it will no be
                            // returned in the message list for them alone
                            // (we still show the sender)
                            statement = "UPDATE messages SET deleted = ? WHERE messageID = ? LIMIT 1";
                            pStatement = con.prepareStatement(statement);
                            pStatement.setString(1, "10");
                            pStatement.setInt(2, messageId);
                        }
                        break;
                    case Constants.MESSAGE_SENDER:
                        // Same logic as above
                        if (savedType.charAt(Constants.MESSAGE_RECEIVER) == '1') {
                            statement = "DELETE FROM messages WHERE messageID = ? LIMIT 1";
                            pStatement = con.prepareStatement(statement);
                            pStatement.setInt(1, messageId);
                        } else {
                            statement = "UPDATE messages SET deleted = ? WHERE messageID = ? LIMIT 1";
                            pStatement = con.prepareStatement(statement);
                            pStatement.setString(1, "01");
                            pStatement.setInt(2, messageId);
                        }
                        break;
                    default:
                        return Response.status(Response.Status.BAD_REQUEST).build();
                }

                pStatement.execute();
                return Response.ok().build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch (IOException | SQLException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            ConnectionCloser.getCloser()
                    .closeAll(con, pSt, rs);
        }
    }

    /**
     * @param received Takes the values 0/1 depending on whether the client has requested their
     *                 received/sent messages respectively
     * @return A list of non-deleted messages sent/received by the user
     */
    @Path("/receive/{received}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response getMessages(
            @PathParam("received") int received,
            String token) {

        Authenticator auth = new Authenticator(token, Constants.TYPE_USER);
        if (!auth.authenticate()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        int userId = auth.getId();
        Connection con = null;
        PreparedStatement pSt = null;
        ResultSet rs = null;
        try {
            con = DataSource.getInstance().getConnection();
            String query;
            if (received == Constants.MESSAGE_RECEIVER) {
                query = "SELECT * FROM messages WHERE receiverID = ? AND deleted LIKE ?";
            } else {
                query = "SELECT * FROM messages WHERE senderID = ? and deleted LIKE ?";
            }
            pSt = con.prepareStatement(query);
            pSt.setInt(1, userId);

            String show;
            switch (received) {
                case Constants.MESSAGE_RECEIVER:
                    show = "0%";
                    break;
                case Constants.MESSAGE_SENDER:
                    show = "%0";
                    break;
                default:
                    return Response.status(Response.Status.BAD_REQUEST).build();
            }

            pSt.setString(2, show);

            rs = pSt.executeQuery();
            List<MessageBean> entities = new ArrayList<>();
            while (rs.next()) {
                MessageBean message = new MessageBean();
                message.setMessage(rs.getString("message"));
                message.setReceiverId(rs.getInt("receiverId"));
                message.setSenderId(rs.getInt("senderId"));
                message.setSubject(rs.getString("subject"));
                message.setMessageId(rs.getInt("messageID"));

                Connection nameCon = null;
                PreparedStatement namepSt = null;
                ResultSet nameRs = null;
                String name;
                try {
                    nameCon = DataSource.getInstance().getConnection();
                    query = "SELECT firstName, lastName FROM users WHERE userID = ? LIMIT 1";
                    namepSt = nameCon.prepareStatement(query);

                    switch (received) {
                        case Constants.MESSAGE_RECEIVER:
                            namepSt.setInt(1, rs.getInt("receiverId"));
                            break;
                        case Constants.MESSAGE_SENDER:
                            namepSt.setInt(1, rs.getInt("senderId"));
                            break;
                    }

                    nameRs = namepSt.executeQuery();

                    if (nameRs.next()) {
                        name = nameRs.getString("firstName") + " "
                                + nameRs.getString("lastName");
                    } else {
                        throw new SQLException("empty result set");
                    }
                } catch (IOException | SQLException e) {
                    e.printStackTrace();
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
                } finally {
                    ConnectionCloser.getCloser()
                            .closeAll(nameCon, namepSt, nameRs);
                }

                message.setName(name);
                entities.add(message);
            }

            Gson gson = new Gson();
            String response = gson.toJson(entities);
            return Response.ok(response).build();
        } catch (IOException | SQLException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            ConnectionCloser.getCloser()
                    .closeAll(con, pSt, rs);
        }
    }
}

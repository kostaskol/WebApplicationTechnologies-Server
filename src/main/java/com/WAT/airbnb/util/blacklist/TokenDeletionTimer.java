package com.WAT.airbnb.util.blacklist;

import com.WAT.airbnb.db.DataSource;
import com.WAT.airbnb.etc.Constants;
import com.WAT.airbnb.rest.Authenticator;
import com.WAT.airbnb.util.helpers.ConnectionCloser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;

import java.io.IOException;
import java.sql.*;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 *  Simple timed task that runs every 30 minutes and removes
 *  the blacklisted tokens from the database (since by that time they are already invalid)
 *  @author Kostas Kolivas
 *  @version 1.0
 */
public class TokenDeletionTimer extends TimerTask {
    private Timer timer;
    private boolean stopped;

    TokenDeletionTimer() {}

    TokenDeletionTimer(Timer timer) {
        this.timer = timer;
        stopped = false;
    }

    private void checkTokens() throws IOException, SQLException {
        Connection con = null;
        ResultSet rs = null;
        try {
            con = DataSource.getInstance().getConnection();
            String query = "SELECT * FROM invalidTokens";
            Statement st = con.createStatement();
            rs = st.executeQuery(query);
            if (rs.next()) {
                rs.previous();
            } else {
                throw new SQLException("Empty result set");
            }
            while (rs.next()) {
                String token = rs.getString("token");
                // Try to validate token.
                // If it fails, we remove it from the database
                try {
                    Jwts.parser()
                            .setSigningKey(Constants.key)
                            .parseClaimsJws(token);
                } catch (Exception e) {
                    Connection deleteCon = null;
                    try {
                        deleteCon = DataSource.getInstance().getConnection();
                        String delete = "DELETE FROM invalidTokens WHERE tokenID = ?";
                        PreparedStatement pSt = deleteCon.prepareStatement(delete);
                        pSt.setInt(1, rs.getInt("tokenID"));
                        pSt.execute();
                        try {
                            pSt.close();
                        } catch (SQLException e3) {
                            e3.printStackTrace();
                        }

                    } catch (SQLException | IOException e2) {
                        throw e2;
                    } finally {
                        ConnectionCloser.closeAll(deleteCon, null, null);
                    }
                }
            }
        } catch (SQLException | IOException e1) {
            throw e1;
        } finally {
            ConnectionCloser.closeAll(con, null, rs);
        }

    }


    boolean hasStopped() { return this.stopped; }

    @Override
    public void run() {
        try {
            checkTokens();
        } catch (Exception e) {
            e.printStackTrace();
            timer.cancel();
        }
    }
}

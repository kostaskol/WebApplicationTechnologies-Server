package com.WAT.airbnb.etc;

import com.WAT.airbnb.db.DataSource;
import com.WAT.airbnb.rest.Authenticator;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;

import java.io.IOException;
import java.sql.*;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

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
                try {
                    Jws<Claims> claims = Jwts.parser()
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
                        if (deleteCon != null) {
                            try {
                                deleteCon.close();
                            } catch (SQLException e4) {
                                e4.printStackTrace();
                            }
                        }
                    }
                }
            }
        } catch (SQLException | IOException e1) {
            throw e1;
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

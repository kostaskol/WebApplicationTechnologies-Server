package com.WAT.airbnb.util.blacklist;

import com.WAT.airbnb.db.DataSource;
import com.WAT.airbnb.etc.Constants;
import com.WAT.airbnb.util.helpers.ConnectionCloser;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Timer;

/**
 * Singleton class that "blacklists" signed out tokens
 * @author Kostas Kolivas
 * @version 1.0
 */
public class BlackList {
    private static BlackList instance = null;
    private Timer timer = null;
    private TokenDeletionTimer deletionTimer = null;

    private BlackList() {}

    public static BlackList getInstance() {
        if (instance == null) {
            instance = new BlackList();
        }
        return instance;
    }

    public void addToList(String token, int scope) throws SQLException, IOException {
        Connection con = null;
        PreparedStatement pSt = null;
        Connection checkCon = null;
        PreparedStatement checkPst = null;
        ResultSet checkRs = null;
        // Check whether the same token exists in the database
        try {
            checkCon = DataSource.getInstance().getConnection();
            String query = "SELECT NULL FROM invalidTokens WHERE token = ? LIMIT 1";
            checkPst = checkCon.prepareStatement(query);
            checkPst.setString(1, token);
            checkRs = checkPst.executeQuery();
            if (checkRs.next()) return;
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        } finally {
            ConnectionCloser.closeAll(checkCon, checkPst, checkRs);
        }
        try {
            con = DataSource.getInstance().getConnection();
            String insert = "INSERT INTO invalidTokens (token, scope)" +
                    "VALUES (?, ?)";
            pSt = con.prepareStatement(insert);
            pSt.setString(1, token);
            pSt.setInt(2, scope);
            pSt.execute();

            if (deletionTimer != null) {
                System.out.println("The timer was already running");
                if (deletionTimer.hasStopped()) {
                    // The deletion should run every TOKEN_EXPIRATION milliseconds
                    timer.schedule(deletionTimer, 0, Constants.EXPIRATION_TIME_ALL);
                } else {
                    System.out.println(" and was not stopped");
                }
            } else {
                System.out.println("Created a new timer");
                timer = new Timer();
                deletionTimer = new TokenDeletionTimer(timer);
                timer.schedule(deletionTimer, 0, Constants.EXPIRATION_TIME_ALL);
            }

        } finally {
            ConnectionCloser.closeAll(con, pSt, null);
        }
    }

    public boolean in(String token) throws SQLException, IOException {
        Connection con = null;
        ResultSet rs = null;
        PreparedStatement pSt = null;
        try {
            con = DataSource.getInstance().getConnection();
            String query = "SELECT * FROM invalidTokens WHERE token = ? LIMIT 1";
            pSt = con.prepareStatement(query);
            pSt.setString(1, token);
            rs = pSt.executeQuery();
            return rs.next();
        } finally {
            ConnectionCloser.closeAll(con, pSt, rs);
        }
    }
}

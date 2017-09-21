package com.WAT.airbnb.util.helpers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 *  Simple static class that closes Connection, Statement and ResultSet objects
 */
public class ConnectionCloser {
    public static void closeAll(Connection con, Statement statement, ResultSet rs) {
        if (con != null) {
            try {
                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        if (statement != null) {
            try {
                statement.close();
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

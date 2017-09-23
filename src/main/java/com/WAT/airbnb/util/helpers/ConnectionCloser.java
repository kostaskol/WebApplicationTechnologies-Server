package com.WAT.airbnb.util.helpers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 *  Simple static class that closes Connection, Statement and ResultSet objects
 *  @author Kostas Kolivas
 *  @version 1.0
 */
public class ConnectionCloser {
    final private static ConnectionCloser con = new ConnectionCloser();

    private ConnectionCloser() {}

    public void closeAll(Connection con, Statement statement, ResultSet rs) {
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

    public static ConnectionCloser getCloser() {
        return con;
    }

    public ConnectionCloser closeConnection(Connection con) {
        if (con != null) {
            try {
                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return this;
    }

    public ConnectionCloser closeStatement(Statement st) {
        if (st != null) {
            try {
                st.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return this;
    }

    public ConnectionCloser closeResultSet(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return this;
    }
}

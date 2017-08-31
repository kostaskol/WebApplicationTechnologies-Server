package com.WAT.airbnb.db;

import com.WAT.airbnb.etc.Constants;
import com.WAT.airbnb.etc.XmlParser;
import org.apache.commons.dbcp.BasicDataSource;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

public class DataSource {
    private static DataSource dataSource;
    private BasicDataSource ds;

    private DataSource() throws IOException, SQLException {
        ds = new BasicDataSource();
        XmlParser parser = new XmlParser(Constants.DIR + "/config.xml");
        ds.setDriverClassName(parser.getConf("driver-class"));
        ds.setUsername(parser.getConf("username"));
        ds.setPassword(parser.getConf("passwd"));
        ds.setUrl(parser.getConf("jdbc-url"));

        ds.setMinIdle(5);
        ds.setMaxIdle(20);
        ds.setMaxOpenPreparedStatements(180);
    }

    public static DataSource getInstance() throws IOException, SQLException {
        if (dataSource == null) {
            dataSource = new DataSource();
        }

        return dataSource;
    }

    public Connection getConnection() throws SQLException {
        return this.ds.getConnection();
    }

    public void printCon() {
        System.out.println("IDLE CONNECTIONS: " + this.ds.getNumIdle());
        System.out.println("ACTIVE CONNECTIONS: " + this.ds.getNumActive());
    }
}

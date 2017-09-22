package com.WAT.airbnb.util.helpers;

import com.WAT.airbnb.db.DataSource;
import com.WAT.airbnb.rest.entities.HouseMinBean;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 *  Simple static class that takes a ResultSet object as input
 *  and returns an ArrayList<HouseMinBean> object
 *  @author Kostas Kolivas
 *  @version 1.0
 */
public class HouseGetter {
    public static ArrayList<HouseMinBean> getHouseMinList(ResultSet minHouseRs) throws SQLException, IOException {
        ArrayList<HouseMinBean> entities = new ArrayList<>();

        while (minHouseRs.next()) {
            HouseMinBean entity = new HouseMinBean();
            entity.setHouseId(minHouseRs.getInt("houseID"));
            entity.setCity(minHouseRs.getString("city"));
            entity.setCountry(minHouseRs.getString("country"));
            entity.setNumRatings(minHouseRs.getInt("numRatings"));
            entity.setMinCost(minHouseRs.getFloat("minCost"));

            Connection picCon = DataSource.getInstance().getConnection();
            String query = "SELECT thumbURL FROM photographs WHERE houseID = ? AND main = 1";
            PreparedStatement picSt = picCon.prepareStatement(query);
            picSt.setInt(1, minHouseRs.getInt("houseID"));
            ResultSet picRs = picSt.executeQuery();
            if (picRs.next()) {
                entity.setPicture(FileHelper.getFileAsString(picRs.getString("thumbURL")));
            }
            ConnectionCloser.closeAll(picCon, picSt, null);
            entities.add(entity);
        }
        return entities;
    }
}

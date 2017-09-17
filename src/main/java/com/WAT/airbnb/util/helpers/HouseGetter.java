package com.WAT.airbnb.util.helpers;

import com.WAT.airbnb.db.DataSource;
import com.WAT.airbnb.etc.Constants;
import com.WAT.airbnb.rest.entities.House;
import com.WAT.airbnb.rest.entities.HouseMinEntity;
import com.WAT.airbnb.rest.entities.HousePageBundle;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class HouseGetter {
    public static HousePageBundle getHouseMinList(ResultSet minHouseRs) throws SQLException, IOException {
        ArrayList<House> entities = new ArrayList<>();

        while (minHouseRs.next()) {
            HouseMinEntity entity = new HouseMinEntity();
            entity.setHouseId(minHouseRs.getInt("houseID"));
            entity.setCity(minHouseRs.getString("city"));
            entity.setCountry(minHouseRs.getString("country"));
            entity.setRating(minHouseRs.getFloat("rating"));
            entity.setNumRatings(minHouseRs.getInt("numRatings"));
            entity.setMinCost(minHouseRs.getFloat("minCost"));

            Connection picCon = DataSource.getInstance().getConnection();
            String query = "SELECT pictureURL FROM photographs WHERE houseID = ? AND main = 1";
            PreparedStatement picSt = picCon.prepareStatement(query);
            picSt.setInt(1, minHouseRs.getInt("houseID"));
            ResultSet picRs = picSt.executeQuery();
            if (picRs.next()) {
                entity.setPicture(FileHelper.getFileAsString(picRs.getString("pictureURL")));
            }
            entities.add(entity);
            try {
                picCon.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }

            try {
                picRs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        HousePageBundle bundle = new HousePageBundle();
        bundle.setHouses(entities);
        bundle.setNumPages(entities.size() / Constants.PAGE_SIZE + 1);
        return bundle;
    }
}
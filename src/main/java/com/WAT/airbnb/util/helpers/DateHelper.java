package com.WAT.airbnb.util.helpers;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 *  Static class that converts java.lang.String objects to java.sql.Date objects
 *  and vice versa
 */
public class DateHelper {
    static public java.sql.Date stringToDate(String strDate) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        java.util.Date date = sdf.parse(strDate);
        return new java.sql.Date(date.getTime());
    }

    static public String dateToString(java.util.Date date) {
        if (date != null) {
            java.util.Date utilDate = new java.util.Date(date.getTime());
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            return dateFormat.format(utilDate);
        }
        return null;
    }

    static public Timestamp stringToDateTime(String strDate) throws ParseException {
        if (strDate == null || strDate.equals("null")) return null;
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        java.util.Date date = sdf.parse(strDate);
        return new java.sql.Timestamp(date.getTime());
    }
}

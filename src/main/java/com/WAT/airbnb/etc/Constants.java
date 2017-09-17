package com.WAT.airbnb.etc;

import io.jsonwebtoken.impl.crypto.MacProvider;

import java.security.Key;

public class Constants {
    // Token signing key (generated once on the REST service execution)
    public static final Key key = MacProvider.generateKey();

    public static final String DIR = /* Windows  "C:/Users/WKostas/workspace/Java/EE/wat" */
                                     /* Linux */ "/home/kostas/workspace/Java/EE/wat" ;

    public static final int MESSAGE_RECEIVER = 0;
    public static final int MESSAGE_SENDER = 1;

    public static final int ADMIN_OFFS = 0;
    public static final int RENTER_OFFS = 1;
    public static final int USER_OFFS = 2;

    public static final int TYPE_ADMIN = 100;
    public static final int TYPE_RENTER = 101;
    public static final int TYPE_USER = 102;

    public static final String SCOPE_ADMINS = "groups/admins";
    public static final String SCOPE_RENTERS = "groups/renters";
    public static final String SCOPE_USERS = "groups/users";

    public static final long ADMIN_EXPIRATION_TIME = 180000; // Admin tokens expire after 3 minutes
    public static final long EXPIRATION_TIME_ALL = 3600000; // User tokens expire after one hour

    public static final int BOOKING_NOT_EXPIRED = 205;
    public static final int BOOKING_NOT_OWNED = 206;

    public static final int ADDR_OFFS = 0;
    public static final int CITY_OFFS = 1;
    public static final int COUNTRY_OFFS = 2;

    public static final int PAGE_SIZE = 8;

    public static final int STATUS_BAD_DATE_RANGE = 205;
    public static final int STATUS_DATE_OOB = 206;
    public static final int STATUS_DATE_BOOKED = 207;

    public static final int K = 8;
    public static final int MAX_NEIGHBOURS = 16;
}

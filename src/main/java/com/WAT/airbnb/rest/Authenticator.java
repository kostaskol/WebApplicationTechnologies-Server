package com.WAT.airbnb.rest;

import com.WAT.airbnb.util.blacklist.BlackList;
import com.WAT.airbnb.etc.Constants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;

import java.util.ArrayList;
import java.util.List;

/**
 * Authenticates the provided JWT token
 */
public class Authenticator {
    private String token;
    private Integer id;
    private List<String> type;

    /**
     * @param token The provided token
     * @param type The minimum scope that is allowed
     *             User - Renter - Administrator
     */
    public Authenticator(String token, int type) {
        this.token = token;
        this.type = new ArrayList<String>();
        switch (type) {
            case Constants.TYPE_USER:
                this.type.add(Constants.SCOPE_ADMINS);
                this.type.add(Constants.SCOPE_RENTERS);
                this.type.add(Constants.SCOPE_USERS);
                break;
            case Constants.TYPE_RENTER:
                this.type.add(Constants.SCOPE_ADMINS);
                this.type.add(Constants.SCOPE_RENTERS);
                break;
            case Constants.TYPE_ADMIN:
                this.type.add(Constants.SCOPE_ADMINS);
                break;
        }
        id = null;
    }

    public Authenticator(String token) {
        this.token = token;
    }

    /**
     * When we authenticate for the XML export functionality of the administrator
     * we check the usage claim
     * @return True on successful authentication
     */
    public boolean authenticateExport() {
        try {
            Jws<Claims> claims = Jwts.parser()
                    .setSigningKey(Constants.key)
                    .parseClaimsJws(this.token);

            return claims.getBody().get("usage").equals("xml");
        } catch (Exception e) {
            return false;
        }

    }

    public boolean authenticate() {
        try {
            BlackList blackList = BlackList.getInstance();
            Jws<Claims> claims = Jwts.parser()
                    .setSigningKey(Constants.key)
                    .parseClaimsJws(this.token);

            String scope = (String) claims.getBody().get("scope");
            boolean verified = false;
            for (String s : this.type) {
                if (scope.equals(s)) {
                    verified = true;
                    break;
                }
            }
            if (!verified) {
                return false;
            }

            // Check if token has been verified but is in the blacklist
            try {
                if (blackList.in(token)) {
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }

            this.id = Integer.parseInt((String) claims.getBody().get("id"));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public int getId() {
        return this.id;
    }
}

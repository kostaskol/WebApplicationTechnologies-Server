package com.WAT.airbnb.rest;

import com.WAT.airbnb.util.blacklist.BlackList;
import com.WAT.airbnb.etc.Constants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;

import java.util.ArrayList;
import java.util.List;

public class Authenticator {
    private String token;
    private Integer id;
    private List<String> type;

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

    public void authenticateExport() throws Exception {
        Jws<Claims> claims = Jwts.parser()
                .setSigningKey(Constants.key)
                .parseClaimsJws(this.token);

    }

    public boolean authenticate() {
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
            System.out.println("Not verified");
            return false;
        }

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
    }

    public int getId() {
        return this.id;
    }
}

package com.WAT.airbnb.rest;

import com.WAT.airbnb.util.blacklist.BlackList;
import com.WAT.airbnb.etc.Constants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;

import java.util.List;

public class Authenticator {
    private String token;
    private Integer id;
    private List<String> type;

    public Authenticator(String token, List<String> accType) {
        this.token = token;
        this.type = accType;
        id = null;
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

package com.WAT.airbnb.util.helpers;

import com.WAT.airbnb.etc.Constants;

import java.util.ArrayList;
import java.util.List;

public class ScopeFiller {
    static public List<String> fillScope(int minScope) {
        List<String> scopes = new ArrayList<>();
        switch (minScope) {
            case Constants.TYPE_USER:
                scopes.add(Constants.SCOPE_ADMINS);
                scopes.add(Constants.SCOPE_RENTERS);
                scopes.add(Constants.SCOPE_USERS);
                break;
            case Constants.TYPE_RENTER:
                scopes.add(Constants.SCOPE_ADMINS);
                scopes.add(Constants.SCOPE_RENTERS);
                break;
            case Constants.TYPE_ADMIN:
                scopes.add(Constants.SCOPE_ADMINS);
                break;
        }

        return scopes;
    }
}

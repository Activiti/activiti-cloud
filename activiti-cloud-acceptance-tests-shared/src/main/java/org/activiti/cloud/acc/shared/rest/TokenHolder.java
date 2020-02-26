package org.activiti.cloud.acc.shared.rest;

import org.activiti.cloud.acc.shared.model.AuthToken;

public class TokenHolder {

    private static AuthToken authToken = null;
    private static String userName = null;

    public static AuthToken getAuthToken() {
        return authToken;
    }

    public static void setAuthToken(AuthToken authToken) {
        TokenHolder.authToken = authToken;
    }

    public static String getUserName(){
        return userName;
    }

    public static void setUserName(String userName) {
        TokenHolder.userName = userName;
    }
}

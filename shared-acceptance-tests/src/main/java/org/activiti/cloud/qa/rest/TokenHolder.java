package org.activiti.cloud.qa.rest;

import org.activiti.cloud.qa.model.AuthToken;

public class TokenHolder {

    private static AuthToken authToken = null;

    public static AuthToken getAuthToken() {
        return authToken;
    }

    public static void setAuthToken(AuthToken authToken) {
        TokenHolder.authToken = authToken;
    }
}

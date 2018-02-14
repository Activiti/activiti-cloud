package org.activiti.cloud.services.security;

import org.activiti.engine.impl.identity.Authentication;
import org.springframework.stereotype.Component;

/**
 * Wrap Authentication.java so as to be able to mock static methods. May later want to move this to engine level but not necessary now.
 */
@Component
public class AuthenticationWrapper {

    public void setAuthenticatedUserId(String user) {
        Authentication.setAuthenticatedUserId(user);
    }

    public String getAuthenticatedUserId() {
        return Authentication.getAuthenticatedUserId();
    }
}

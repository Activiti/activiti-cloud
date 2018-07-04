package org.activiti.cloud.services.common.security;

import org.springframework.security.core.Authentication;

public interface SpringSecurityAuthenticationWrapper {

    Authentication getAuthentication();

    String getAuthenticatedUserId();
}

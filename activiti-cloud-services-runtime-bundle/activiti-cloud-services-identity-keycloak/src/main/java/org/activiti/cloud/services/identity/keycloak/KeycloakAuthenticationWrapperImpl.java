package org.activiti.cloud.services.identity.keycloak;

import org.activiti.cloud.services.common.security.SpringSecurityAuthenticationWrapperImpl;

public class KeycloakAuthenticationWrapperImpl extends SpringSecurityAuthenticationWrapperImpl {

    @Override
    public String getAuthenticatedUserId() {
        return super.getAuthenticatedUserId();
    }
}

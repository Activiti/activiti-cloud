package org.activiti.cloud.services.common.security.keycloak;

import org.activiti.cloud.services.common.security.SpringSecurityAuthenticationWrapperImpl;
import org.keycloak.KeycloakPrincipal;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class KeycloakSecurityAuthenticationWrapperImpl extends SpringSecurityAuthenticationWrapperImpl {



    @Override
    public String getAuthenticatedUserId() {
        Object principal = getAuthentication().getPrincipal();
        if (principal instanceof KeycloakPrincipal) {
            KeycloakPrincipal keycloakPrincipal = (KeycloakPrincipal) principal;
            return keycloakPrincipal.getKeycloakSecurityContext().getToken().getPreferredUsername();
        }
        return getAuthentication().getName();
    }
}

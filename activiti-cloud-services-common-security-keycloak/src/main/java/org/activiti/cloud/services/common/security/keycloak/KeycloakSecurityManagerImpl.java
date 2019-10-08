package org.activiti.cloud.services.common.security.keycloak;

import org.activiti.api.runtime.shared.security.SecurityManager;
import org.keycloak.KeycloakPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;

public class KeycloakSecurityManagerImpl implements SecurityManager {

    @Override
    public String getAuthenticatedUserId() {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof KeycloakPrincipal) {
                KeycloakPrincipal keycloakPrincipal = (KeycloakPrincipal) principal;
                return keycloakPrincipal.getKeycloakSecurityContext().getToken().getPreferredUsername();
            }
            return SecurityContextHolder.getContext().getAuthentication().getName();
        }
        return "";
    }
}

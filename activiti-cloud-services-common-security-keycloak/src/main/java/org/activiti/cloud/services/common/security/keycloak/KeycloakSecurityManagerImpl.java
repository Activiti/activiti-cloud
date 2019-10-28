package org.activiti.cloud.services.common.security.keycloak;

import org.activiti.api.runtime.shared.security.AbstractSecurityManager;
import org.activiti.api.runtime.shared.security.PrincipalGroupsProvider;
import org.activiti.api.runtime.shared.security.PrincipalIdentityProvider;
import org.activiti.api.runtime.shared.security.PrincipalRolesProvider;
import org.activiti.api.runtime.shared.security.SecurityContextPrincipalProvider;
import org.springframework.lang.NonNull;

public class KeycloakSecurityManagerImpl extends AbstractSecurityManager {
    
    public KeycloakSecurityManagerImpl(@NonNull SecurityContextPrincipalProvider securityContextPrincipalProvider,
                                       @NonNull PrincipalIdentityProvider principalIdentityProvider,
                                       @NonNull PrincipalGroupsProvider principalGroupsProvider,
                                       @NonNull PrincipalRolesProvider principalRolesProvider) {
        super(securityContextPrincipalProvider, 
              principalIdentityProvider, 
              principalGroupsProvider,
              principalRolesProvider);
    }


}

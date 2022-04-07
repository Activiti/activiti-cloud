package org.activiti.cloud.services.common.security.oidc;

import java.util.List;
import org.activiti.api.runtime.shared.security.AbstractSecurityManager;
import org.activiti.api.runtime.shared.security.PrincipalGroupsProvider;
import org.activiti.api.runtime.shared.security.PrincipalIdentityProvider;
import org.activiti.api.runtime.shared.security.PrincipalRolesProvider;
import org.activiti.api.runtime.shared.security.SecurityContextPrincipalProvider;

public class OAuth2SecurityManagerImpl extends AbstractSecurityManager {

    public OAuth2SecurityManagerImpl(SecurityContextPrincipalProvider securityContextPrincipalProvider,
        PrincipalIdentityProvider principalIdentityProvider,
        PrincipalGroupsProvider principalGroupsProvider,
        PrincipalRolesProvider principalRolesProvider) {
        super(securityContextPrincipalProvider,
            principalIdentityProvider,
            principalGroupsProvider,
            principalRolesProvider);
    }

//    @Override
//    public String getAuthenticatedUserId() {
//        return null;
//    }

    @Override
    public List<String> getAuthenticatedUserGroups() {
        return null;
    }

//    @Override
//    public List<String> getAuthenticatedUserRoles() {
//        return null;
//    }



}

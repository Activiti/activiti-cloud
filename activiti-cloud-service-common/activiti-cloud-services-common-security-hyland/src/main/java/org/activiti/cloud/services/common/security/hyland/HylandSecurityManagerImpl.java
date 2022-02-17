package org.activiti.cloud.services.common.security.hyland;

import org.activiti.api.runtime.shared.security.AbstractSecurityManager;
import org.activiti.api.runtime.shared.security.PrincipalGroupsProvider;
import org.activiti.api.runtime.shared.security.PrincipalIdentityProvider;
import org.activiti.api.runtime.shared.security.PrincipalRolesProvider;
import org.activiti.api.runtime.shared.security.SecurityContextPrincipalProvider;

public class HylandSecurityManagerImpl extends AbstractSecurityManager {

    public HylandSecurityManagerImpl(SecurityContextPrincipalProvider securityContextPrincipalProvider,
        PrincipalIdentityProvider principalIdentityProvider,
        PrincipalGroupsProvider principalGroupsProvider,
        PrincipalRolesProvider principalRolesProvider) {
        super(securityContextPrincipalProvider,
            principalIdentityProvider,
            principalGroupsProvider,
            principalRolesProvider);
    }
}

package org.activiti.cloud.services.common.security.oidc;

import java.security.Principal;
import java.util.List;
import org.activiti.api.runtime.shared.security.PrincipalRolesProvider;

public class OAuth2PrincipalRolesProvider implements PrincipalRolesProvider {

    @Override
    public List<String> getRoles(Principal principal) {
        return null;
    }
}

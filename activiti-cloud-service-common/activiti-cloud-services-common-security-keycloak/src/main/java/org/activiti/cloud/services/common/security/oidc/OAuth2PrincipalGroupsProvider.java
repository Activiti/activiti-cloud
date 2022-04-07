package org.activiti.cloud.services.common.security.oidc;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import org.activiti.api.runtime.shared.security.PrincipalGroupsProvider;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

public class OAuth2PrincipalGroupsProvider  implements PrincipalGroupsProvider {

    @Override
    public List<String> getGroups(Principal principal) {
         ((OAuth2AuthenticationToken)principal)
            .getPrincipal()
            .getAttributes()
            .get("groups");

         return Collections.emptyList();
    }
}

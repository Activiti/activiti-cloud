package org.activiti.cloud.services.common.security.hyland;

import java.security.Principal;
import java.util.Optional;
import org.activiti.api.runtime.shared.security.SecurityContextPrincipalProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

public class HylandSecurityContextPrincipalProvider implements SecurityContextPrincipalProvider {

    @Override
    public Optional<Principal> getCurrentPrincipal() {
        return Optional.ofNullable(SecurityContextHolder.getContext())
            .map(SecurityContext::getAuthentication)
            .map(Authentication::getPrincipal)
            .filter(OidcUser.class::isInstance)
            .map(OidcUser.class::cast);
    }

    public void getPrincipalClaims() {
        Optional.ofNullable(SecurityContextHolder.getContext())
            .map(SecurityContext::getAuthentication)
            .map(Authentication::getPrincipal)
            .filter(OidcUser.class::isInstance)
            .map(OidcUser.class::cast)
            .map(OidcUser::getUserInfo)
            .map(OidcUserInfo::getClaims);
    }

}

package org.activiti.cloud.services.common.security.hyland;

import java.util.Optional;
import org.activiti.api.runtime.shared.security.SecurityContextTokenProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * Hyland IAM implementation for {@link SecurityContextTokenProvider}
 */
public class HylandIAMSecurityContextTokenProvider implements SecurityContextTokenProvider {

    @Override
    public Optional<String> getCurrentToken() {
        return Optional.ofNullable(SecurityContextHolder.getContext())
            .map(SecurityContext::getAuthentication)
            .map(Authentication::getPrincipal)
            .filter(OidcUser.class::isInstance)
            .map(OidcUser.class::cast)
            .map(OidcUser::getIdToken)
            .map(OidcIdToken::getTokenValue);
    }
}

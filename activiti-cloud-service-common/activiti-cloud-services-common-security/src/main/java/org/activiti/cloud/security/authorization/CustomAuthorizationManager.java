package org.activiti.cloud.security.authorization;

import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

public class CustomAuthorizationManager<RequestAuthorizationContext>
    implements AuthorizationManager<RequestAuthorizationContext> {

    public static final String ROLE_PREFIX = "ROLE_";
    public static final String PERMISSION_PREFIX = "PERMISSION_";

    private final Set<String> authoritiesWithAccess;

    public CustomAuthorizationManager(String[] roles, String[] permissions) {
        this.authoritiesWithAccess =
            Stream
                .concat(
                    Stream.of(roles).map(role -> ROLE_PREFIX + role),
                    Stream.of(permissions).map(permission -> PERMISSION_PREFIX + permission)
                )
                .collect(Collectors.toSet());
    }

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext object) {
        return new AuthorizationDecision(
            authentication
                .get()
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authoritiesWithAccess::contains)
        );
    }

    public Set<String> getAuthoritiesWithAccess() {
        return authoritiesWithAccess;
    }
}

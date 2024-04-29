/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

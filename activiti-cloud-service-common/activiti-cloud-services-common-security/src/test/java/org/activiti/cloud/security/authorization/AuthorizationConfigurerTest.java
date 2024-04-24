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

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.HEAD;
import static org.springframework.http.HttpMethod.OPTIONS;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.TRACE;

import java.util.List;
import org.activiti.cloud.security.authorization.AuthorizationProperties.SecurityCollection;
import org.activiti.cloud.security.authorization.AuthorizationProperties.SecurityConstraint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

@ExtendWith(MockitoExtension.class)
class AuthorizationConfigurerTest {

    @Mock
    private HttpSecurity http;

    @Mock
    private AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry authorizeRequests;

    @Mock
    private AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizedUrl authorizedUrl;

    @Captor
    private ArgumentCaptor<Customizer<AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry>> authorizeHttpRequestsCustomizer;

    @Captor
    private ArgumentCaptor<String[]> requestMatchers;

    @Test
    public void should_configureAuth_when_everythingIsAuthenticated() throws Exception {
        AuthorizationProperties authorizationProperties = new AuthorizationProperties();
        List<SecurityConstraint> securityConstraints = asList(
            createSecurityConstraintWithRolesAndPatterns(
                new String[] { "ROLE_1", "ROLE_2" },
                new String[] { "/a", "/b" }
            ),
            createSecurityConstraintWithRolesAndPatterns(new String[] { "ROLE_3" }, new String[] { "/c" })
        );
        authorizationProperties.setSecurityConstraints(securityConstraints);
        AuthorizationConfigurer authorizationConfigurer = new AuthorizationConfigurer(authorizationProperties, null);

        when(http.authorizeHttpRequests(authorizeHttpRequestsCustomizer.capture())).thenReturn(http);
        when(authorizeRequests.requestMatchers(any(HttpMethod.class), any(String.class))).thenReturn(authorizedUrl);

        authorizationConfigurer.configure(http);

        assertThat(authorizeHttpRequestsCustomizer.getAllValues()).hasSize(2);
        authorizeHttpRequestsCustomizer.getAllValues().forEach($ -> $.customize(authorizeRequests));

        InOrder inOrder = inOrder(authorizeRequests, authorizedUrl);

        for (HttpMethod method : HttpMethod.values()) {
            inOrder.verify(authorizeRequests).requestMatchers(eq(method), eq("/c"));
            inOrder.verify(authorizedUrl).hasAnyRole(eq("ROLE_3"));

            inOrder.verify(authorizeRequests).requestMatchers(eq(method), eq("/a"));
            inOrder.verify(authorizedUrl).hasAnyRole(eq("ROLE_1"), eq("ROLE_2"));

            inOrder.verify(authorizeRequests).requestMatchers(eq(method), eq("/b"));
            inOrder.verify(authorizedUrl).hasAnyRole(eq("ROLE_1"), eq("ROLE_2"));
        }
    }

    @Test
    public void should_configureAuth_usingPermissions_when_everythingIsAuthenticated() throws Exception {
        AuthorizationProperties authorizationProperties = new AuthorizationProperties();
        List<SecurityConstraint> securityConstraints = asList(
            createSecurityConstraintWithPermissionsAndPatterns(
                new String[] { "PERMISSION_1", "PERMISSION_2" },
                new String[] { "/a", "/b" }
            ),
            createSecurityConstraintWithPermissionsAndPatterns(new String[] { "PERMISSION_3" }, new String[] { "/c" })
        );
        authorizationProperties.setSecurityConstraints(securityConstraints);
        AuthorizationConfigurer authorizationConfigurer = new AuthorizationConfigurer(authorizationProperties, null);

        when(http.authorizeHttpRequests(authorizeHttpRequestsCustomizer.capture())).thenReturn(http);
        when(authorizeRequests.requestMatchers(requestMatchers.capture())).thenReturn(authorizedUrl);

        authorizationConfigurer.configure(http);

        assertThat(authorizeHttpRequestsCustomizer.getAllValues())
            .hasSize(HttpMethod.values().length * securityConstraints.size());
        authorizeHttpRequestsCustomizer.getAllValues().forEach($ -> $.customize(authorizeRequests));

        InOrder inOrder = inOrder(authorizeRequests, authorizedUrl);

        inOrder.verify(authorizeRequests).requestMatchers(eq("/c"));
        inOrder.verify(authorizedUrl).hasAnyRole(eq("PERMISSION_3"));

        inOrder.verify(authorizeRequests).requestMatchers(eq("/a"), eq("/b"));
        inOrder.verify(authorizedUrl).hasAnyRole(eq("PERMISSION_1"), eq("PERMISSION_2"));
    }

    @Test
    public void should_configureAuth_when_aURLiSPublic() throws Exception {
        AuthorizationProperties authorizationProperties = new AuthorizationProperties();
        authorizationProperties.setSecurityConstraints(
            asList(
                createSecurityConstraintWithRolesAndPatterns(new String[] { "ROLE_3" }, new String[] { "/c" }),
                createSecurityConstraintWithRolesAndPatterns(new String[] {}, new String[] { "/d" })
            )
        );
        AuthorizationConfigurer authorizationConfigurer = new AuthorizationConfigurer(authorizationProperties, null);

        when(http.authorizeHttpRequests(authorizeHttpRequestsCustomizer.capture())).thenReturn(http);
        when(authorizeRequests.requestMatchers(requestMatchers.capture())).thenReturn(authorizedUrl);

        authorizationConfigurer.configure(http);

        assertThat(authorizeHttpRequestsCustomizer.getAllValues()).hasSize(2);
        authorizeHttpRequestsCustomizer.getAllValues().forEach($ -> $.customize(authorizeRequests));

        InOrder inOrder = inOrder(authorizeRequests, authorizedUrl);

        //URLs with permitAll must be defined first in order to avoid being overridden
        inOrder.verify(authorizeRequests).requestMatchers(eq("/d"));
        inOrder.verify(authorizedUrl).permitAll();

        inOrder.verify(authorizeRequests).requestMatchers(eq("/c"));
        inOrder.verify(authorizedUrl).hasAnyRole(eq("ROLE_3"));
    }

    @Test
    public void should_configureAuth_when_everythingIsAuthenticatedMethods() throws Exception {
        AuthorizationProperties authorizationProperties = new AuthorizationProperties();
        authorizationProperties.setSecurityConstraints(
            asList(
                createSecurityConstraint(
                    new String[] { "ROLE_1" },
                    new String[] {},
                    new String[] { "/c" },
                    new String[] { "POST", "DELETE", "PUT" }
                )
            )
        );
        AuthorizationConfigurer authorizationConfigurer = new AuthorizationConfigurer(authorizationProperties, null);

        when(http.authorizeHttpRequests(authorizeHttpRequestsCustomizer.capture())).thenReturn(http);
        when(authorizeRequests.requestMatchers(any(HttpMethod.class), any(String.class))).thenReturn(authorizedUrl);

        authorizationConfigurer.configure(http);

        assertThat(authorizeHttpRequestsCustomizer.getAllValues()).hasSize(5);
        authorizeHttpRequestsCustomizer.getAllValues().forEach($ -> $.customize(authorizeRequests));

        InOrder inOrder = inOrder(authorizeRequests, authorizedUrl);

        inOrder.verify(authorizeRequests).requestMatchers(eq(GET), eq("/c"));
        inOrder.verify(authorizedUrl).hasAnyRole(eq("ROLE_1"));
        inOrder.verify(authorizeRequests).requestMatchers(eq(HEAD), eq("/c"));
        inOrder.verify(authorizedUrl).hasAnyRole(eq("ROLE_1"));
        inOrder.verify(authorizeRequests).requestMatchers(eq(PATCH), eq("/c"));
        inOrder.verify(authorizedUrl).hasAnyRole(eq("ROLE_1"));
        inOrder.verify(authorizeRequests).requestMatchers(eq(OPTIONS), eq("/c"));
        inOrder.verify(authorizedUrl).hasAnyRole(eq("ROLE_1"));
        inOrder.verify(authorizeRequests).requestMatchers(eq(TRACE), eq("/c"));
        inOrder.verify(authorizedUrl).hasAnyRole(eq("ROLE_1"));
    }

    private SecurityConstraint createSecurityConstraintWithRolesAndPatterns(String[] roles, String[] patterns) {
        return createSecurityConstraint(roles, new String[] {}, patterns, new String[] {});
    }

    private SecurityConstraint createSecurityConstraintWithPermissionsAndPatterns(
        String[] permissions,
        String[] patterns
    ) {
        return createSecurityConstraint(new String[] {}, permissions, patterns, new String[] {});
    }

    private SecurityConstraint createSecurityConstraint(
        String[] roles,
        String[] permissions,
        String[] patterns,
        String[] omittedMethods
    ) {
        SecurityConstraint securityConstraint = new SecurityConstraint();
        securityConstraint.setAuthRoles(roles);
        securityConstraint.setAuthPermissions(permissions);
        SecurityCollection securityCollection = new SecurityCollection();
        securityCollection.setPatterns(patterns);
        securityCollection.setOmittedMethods(omittedMethods);
        securityConstraint.setSecurityCollections(new SecurityCollection[] { securityCollection });
        return securityConstraint;
    }
}

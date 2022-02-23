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
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

import org.activiti.cloud.security.authorization.AuthorizationProperties.SecurityCollection;
import org.activiti.cloud.security.authorization.AuthorizationProperties.SecurityConstraint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer.AuthorizedUrl;

@ExtendWith(MockitoExtension.class)
class AuthorizationConfigurerTest {

    @Mock
    private HttpSecurity http;

    @Mock
    private ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry authorizeRequests;

    @Mock
    private AuthorizedUrl authorizedUrl;

    @Test
    public void should_configureAuth_when_everythingIsAuthenticated() throws Exception {
        AuthorizationProperties authorizationProperties = new AuthorizationProperties();
        authorizationProperties.setSecurityConstraints(asList(
            createSecurityConstraint(new String[]{"ROLE_1", "ROLE_2"}, new String[]{"/a", "/b"}),
            createSecurityConstraint(new String[]{"ROLE_3"}, new String[]{"/c"})));
        AuthorizationConfigurer authorizationConfigurer = new AuthorizationConfigurer(authorizationProperties);

        when(http.authorizeRequests()).thenReturn(authorizeRequests);
        when(authorizeRequests.antMatchers(any(String.class))).thenReturn(authorizedUrl);

        authorizationConfigurer.configure(http);

        InOrder inOrder = inOrder(authorizeRequests, authorizedUrl);

        inOrder.verify(authorizeRequests).antMatchers(eq("/a"), eq("/b"));
        inOrder.verify(authorizedUrl).hasAnyRole(eq("ROLE_1"), eq("ROLE_2"));

        inOrder.verify(authorizeRequests).antMatchers(eq("/c"));
        inOrder.verify(authorizedUrl).hasAnyRole(eq("ROLE_3"));
    }

    @Test
    public void should_configureAuth_when_aURLiSPublic() throws Exception {
        AuthorizationProperties authorizationProperties = new AuthorizationProperties();
        authorizationProperties.setSecurityConstraints(asList(
            createSecurityConstraint(new String[]{"ROLE_3"}, new String[]{"/c"}),
            createSecurityConstraint(new String[]{}, new String[]{"/d"})));
        AuthorizationConfigurer authorizationConfigurer = new AuthorizationConfigurer(authorizationProperties);

        when(http.authorizeRequests()).thenReturn(authorizeRequests);
        when(authorizeRequests.antMatchers(any(String.class))).thenReturn(authorizedUrl);

        authorizationConfigurer.configure(http);

        InOrder inOrder = inOrder(authorizeRequests, authorizedUrl);

        //URLs with permitAll must be defined first in order to avoid being overridden
        inOrder.verify(authorizeRequests).antMatchers(eq("/d"));
        inOrder.verify(authorizedUrl).permitAll();

        inOrder.verify(authorizeRequests).antMatchers(eq("/c"));
        inOrder.verify(authorizedUrl).hasAnyRole(eq("ROLE_3"));
    }

    private SecurityConstraint createSecurityConstraint(String[] roles, String[] patterns) {
        SecurityConstraint securityConstraint = new SecurityConstraint();
        securityConstraint.setAuthRoles(roles);
        SecurityCollection securityCollection = new SecurityCollection();
        securityCollection.setPatterns(patterns);
        securityConstraint.setSecurityCollections(new SecurityCollection[]{securityCollection});
        return securityConstraint;
    }

}

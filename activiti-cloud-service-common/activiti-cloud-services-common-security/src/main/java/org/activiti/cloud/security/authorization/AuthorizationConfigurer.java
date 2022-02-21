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

import java.util.stream.Stream;
import org.activiti.cloud.security.authorization.AuthorizationProperties.SecurityCollection;
import org.activiti.cloud.security.authorization.AuthorizationProperties.SecurityConstraint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.stereotype.Component;

/**
 * This class aims to define authorizations on a REST API using a configuration like below:
 *
 * authorizations.security-constraints[0].authRoles[0]=ACTIVITI_USER
 * authorizations.security-constraints[0].securityCollections[0].patterns[0]=/v1/*
 * authorizations.security-constraints[1].authRoles[0]=ACTIVITI_ADMIN
 * authorizations.security-constraints[1].securityCollections[0].patterns[0]=/admin/*
 *
 * This configuration schema is similar to the security constraint configurations used by other systems like Keycloak.
 *
 */
@Component
public class AuthorizationConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationConfigurer.class);

    private AuthorizationProperties authorizationProperties;

    @Autowired
    public AuthorizationConfigurer(AuthorizationProperties authorizationProperties) {
        this.authorizationProperties = authorizationProperties;
    }

    public void configure(HttpSecurity http) throws Exception {
        for (SecurityConstraint securityConstraint : authorizationProperties.getSecurityConstraints()) {
            String[] patterns = getPatterns(securityConstraint);
            String[] roles = securityConstraint.getAuthRoles();
            http.authorizeRequests().antMatchers(patterns).hasAnyRole(roles);
            if(LOGGER.isDebugEnabled()) {
                LOGGER.debug("Setting access to patterns {} for roles {}", patterns, roles);
            }
        }
    }

    private String[] getPatterns(SecurityConstraint securityConstraint) {
        return Stream.of(securityConstraint.getSecurityCollections())
            .map(SecurityCollection::getPatterns)
            .flatMap(Stream::of)
            .map(pattern -> pattern.endsWith("/*") ? pattern + "*" : pattern)
            .toArray(String[]::new);
    }

}

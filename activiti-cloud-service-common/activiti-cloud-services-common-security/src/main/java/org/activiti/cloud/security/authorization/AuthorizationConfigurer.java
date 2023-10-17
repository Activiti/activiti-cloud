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

import static java.util.function.Predicate.not;
import static org.springframework.security.config.Customizer.withDefaults;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.activiti.cloud.security.authorization.AuthorizationProperties.SecurityCollection;
import org.activiti.cloud.security.authorization.AuthorizationProperties.SecurityConstraint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

/**
 * This class aims to define authorizations on a REST API using a configuration like below:
 * <p>
 * authorizations.security-constraints[0].authRoles[0]=ACTIVITI_USER authorizations.security-constraints[0].securityCollections[0].patterns[0]=/v1/* authorizations.security-constraints[1].authRoles[0]=ACTIVITI_ADMIN
 * authorizations.security-constraints[1].securityCollections[0].patterns[0]=/admin/*
 * <p>
 * This configuration schema is similar to the security constraint configurations used by other systems like Keycloak.
 */
@Component
public class AuthorizationConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationConfigurer.class);

    private final AuthorizationProperties authorizationProperties;
    private final Environment environment;

    @Autowired
    public AuthorizationConfigurer(AuthorizationProperties authorizationProperties, Environment environment) {
        this.authorizationProperties = authorizationProperties;
        this.environment = environment;
    }

    @PostConstruct
    public void checkKeycloakConfig() {
        //if there is a Keycloak security constraint defined it could be configuration issue
        String securityConstraintProperty = environment.getProperty(
            "keycloak.security-constraints[0].securityCollections[0].patterns[0]"
        );
        if (securityConstraintProperty != null) {
            LOGGER.warn(
                "A Keycloak security configuration was found, it could override Spring Security configuration, please check if we have properties starting with \"keycloak.security-constraints\"."
            );
        }
    }

    public void configure(HttpSecurity http) throws Exception {
        List<SecurityConstraint> orderedSecurityConstraints = getOrderedList(
            authorizationProperties.getSecurityConstraints()
        );
        for (SecurityConstraint securityConstraint : orderedSecurityConstraints) {
            String[] roles = securityConstraint.getAuthRoles();
            configureAuthorization(http, roles, securityConstraint.getSecurityCollections());
        }
        http.anonymous(withDefaults());
    }

    private void configureAuthorization(HttpSecurity http, String[] roles, SecurityCollection[] securityCollection)
        throws Exception {
        boolean rolesNotEmpty = isNotEmpty(roles);
        Consumer<AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizedUrl> authorizedUrlConsumer;
        if (rolesNotEmpty) {
            authorizedUrlConsumer = a -> a.hasAnyRole(roles);
        } else {
            authorizedUrlConsumer = a -> a.permitAll();
        }
        buildAntMatchers(http, securityCollection, authorizedUrlConsumer);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Setting access {} to {}", securityCollection, rolesNotEmpty ? roles : "anonymous");
        }
    }

    private void buildAntMatchers(
        HttpSecurity http,
        SecurityCollection[] securityCollections,
        Consumer<AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizedUrl> f
    ) throws Exception {
        for (SecurityCollection securityCollection : securityCollections) {
            String[] patterns = getPatterns(securityCollection.getPatterns());
            if (isNotEmpty(securityCollection.getOmittedMethods())) {
                List<HttpMethod> methods = getAllowedMethods(securityCollection.getOmittedMethods());
                for (HttpMethod method : methods) {
                    f.accept(http.authorizeHttpRequests().requestMatchers(method, patterns));
                }
            } else {
                f.accept(http.authorizeHttpRequests().requestMatchers(patterns));
            }
        }
    }

    /**
     * If a security constraint hasn't any roles it means that it can accessible from anyone. It must be the first one
     * in order to avoid being overridden by other rules. The order is reversed because in order
     * to mimic the security-constraint behaviour.
     *
     * @param securityConstraints
     * @return
     */
    private List<SecurityConstraint> getOrderedList(List<SecurityConstraint> securityConstraints) {
        List<SecurityConstraint> reversed = new ArrayList<>(securityConstraints);
        Collections.reverse(reversed);
        List<SecurityConstraint> result = new ArrayList<>();
        reversed.forEach(securityConstraint -> {
            if (isNotEmpty(securityConstraint.getAuthRoles())) {
                result.add(securityConstraint);
            } else {
                result.add(0, securityConstraint);
            }
        });
        return result;
    }

    private String[] getPatterns(String[] patterns) {
        return Stream
            .of(patterns)
            .map(pattern -> pattern.endsWith("/*") ? pattern + "*" : pattern)
            .toArray(String[]::new);
    }

    private List<HttpMethod> getAllowedMethods(String[] omittedMethods) {
        List<HttpMethod> httpMethods = Stream.of(omittedMethods).map(HttpMethod::valueOf).toList();
        return Stream.of(HttpMethod.values()).filter(not(httpMethods::contains)).collect(Collectors.toList());
    }

    private boolean isNotEmpty(String[] array) {
        return array != null && array.length > 0;
    }
}

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

package org.activiti.cloud.services.notifications.graphql.jpa.query;

import graphql.schema.visibility.BlockedFields;
import graphql.schema.visibility.GraphqlFieldVisibility;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

public class ActivitiGraphQlFieldVisibilityProvider implements Supplier<GraphqlFieldVisibility> {

    private static final BlockedFields blockAllFields = BlockedFields
        .newBlock()
        .addCompiledPattern(Pattern.compile(".*"))
        .build();

    private static final GraphqlFieldVisibility allowAllFields = VisibleFields
        .newFieldsVisibility()
        .addCompiledPattern(Pattern.compile(".*"))
        .build();

    private static final Predicate<Authentication> isAnonymous = AnonymousAuthenticationToken.class::isInstance;

    private String rolePrefix = "ROLE_";

    private final ActivitiGraphQlJPASchemaProperties.FieldsVisibilityProperties properties;

    public ActivitiGraphQlFieldVisibilityProvider(ActivitiGraphQlJPASchemaProperties properties) {
        this.properties = properties.getFieldsVisibility();

        Optional
            .ofNullable(properties.getRestrictedKeysProvider())
            .map(ActivitiGraphQlJPASchemaProperties.RestrictedKeysProviderProperties::getRolePrefix)
            .ifPresent(rolePrefix -> this.rolePrefix = rolePrefix);
    }

    @Override
    public GraphqlFieldVisibility get() {
        var authenticationToken = Optional
            .ofNullable(SecurityContextHolder.getContext())
            .map(SecurityContext::getAuthentication);

        if (authenticationToken.filter(isAnonymous).isPresent()) {
            return blockAllFields;
        }

        return authenticationToken
            .filter(Authentication::isAuthenticated)
            .map(Authentication::getAuthorities)
            .map(authoritiesToVisibilityPatterns())
            .filter(Predicate.not(Collection::isEmpty))
            .map(toGraphQlFieldVisibility())
            .orElse(blockAllFields);
    }

    private Function<Collection<? extends GrantedAuthority>, Set<Pattern>> authoritiesToVisibilityPatterns() {
        return authorities ->
            authorities
                .stream()
                .map(GrantedAuthority::getAuthority)
                .map(it -> it.replaceFirst(rolePrefix, ""))
                .flatMap(it -> properties.getPatterns().getOrDefault(it, Set.of()).stream())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Function<Set<Pattern>, GraphqlFieldVisibility> toGraphQlFieldVisibility() {
        return patterns ->
            VisibleFields
                .newFieldsVisibility()
                .addCompiledPatterns(patterns)
                .addCompiledPattern(Pattern.compile("(?!JPA\\.).*"))
                .build();
    }
}

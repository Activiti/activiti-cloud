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

import static graphql.schema.GraphQLScalarType.newScalar;

import com.introproventures.graphql.jpa.query.autoconfigure.EnableGraphQLJpaQuerySchema;
import com.introproventures.graphql.jpa.query.autoconfigure.GraphQLJPASchemaBuilderCustomizer;
import com.introproventures.graphql.jpa.query.schema.JavaScalars;
import com.introproventures.graphql.jpa.query.schema.RestrictedKeysProvider;
import graphql.GraphQL;
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
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.VariableValue;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Spring Boot auto configuration of Activiti GraphQL Query Service components
 */
@AutoConfiguration
@ConditionalOnClass({ GraphQL.class, ProcessInstanceEntity.class })
@ConditionalOnProperty(
    name = "spring.activiti.cloud.services.notifications.graphql.jpa-query.enabled",
    matchIfMissing = true
)
@EnableGraphQLJpaQuerySchema(basePackageClasses = ProcessInstanceEntity.class)
@EnableConfigurationProperties(ActivitiGraphQlJPASchemaProperties.class)
@PropertySource("classpath:config/jpa-query.properties")
public class ActivitiGraphQLSchemaAutoConfiguration {

    private final ActivitiGraphQlJPASchemaProperties properties;

    public ActivitiGraphQLSchemaAutoConfiguration(ActivitiGraphQlJPASchemaProperties properties) {
        this.properties = properties;
    }

    @Bean
    Supplier<GraphqlFieldVisibility> graphqlFieldVisibility() {
        final var blockAllFields = BlockedFields.newBlock().addCompiledPattern(Pattern.compile(".*")).build();
        final var allowAllFields = VisibleFields
            .newFieldsVisibility()
            .addCompiledPattern(Pattern.compile(".*"))
            .build();
        final var rolePrefix = Optional
            .ofNullable(properties.getRestrictedKeysProvider())
            .map(ActivitiGraphQlJPASchemaProperties.RestrictedKeysProviderProperties::getRolePrefix)
            .orElse("ROLE_");

        return () -> {
            var authenticationToken = Optional
                .ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication);

            Predicate<Authentication> isAnonymous = AnonymousAuthenticationToken.class::isInstance;
            Predicate<Authentication> hasUnrestrictedRoles = authentication ->
                authentication
                    .getAuthorities()
                    .stream()
                    .map(GrantedAuthority::getAuthority)
                    .map(it -> it.replaceFirst(rolePrefix, ""))
                    .anyMatch(authority ->
                        properties.getRestrictedKeysProvider().getUnrestrictedRoles().contains(authority)
                    );

            Function<Collection<? extends GrantedAuthority>, Set<Pattern>> authoritiesToVisibilityPatterns = authorities ->
                authorities
                    .stream()
                    .map(GrantedAuthority::getAuthority)
                    .map(it -> it.replaceFirst(rolePrefix, ""))
                    .map(it -> properties.getFieldsVisibility().get(it))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            Function<Set<Pattern>, GraphqlFieldVisibility> toGraphQlFieldVisibility = patterns ->
                VisibleFields
                    .newFieldsVisibility()
                    .addCompiledPatterns(patterns)
                    .addCompiledPattern(Pattern.compile("(?!JPA\\.).*"))
                    .build();

            if (authenticationToken.filter(isAnonymous).isPresent()) {
                return blockAllFields;
            } else if (authenticationToken.filter(hasUnrestrictedRoles).isPresent()) {
                return allowAllFields;
            }

            return authenticationToken
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getAuthorities)
                .map(authoritiesToVisibilityPatterns)
                .filter(Predicate.not(Collection::isEmpty))
                .map(toGraphQlFieldVisibility)
                .orElse(blockAllFields);
        };
    }

    @Bean
    GraphQLJPASchemaBuilderCustomizer graphQLJPASchemaBuilderCustomizer(
        ObjectProvider<RestrictedKeysProvider> restrictedKeysProvider
    ) {
        return builder -> {
            builder
                .name("Query")
                .description("Activiti Cloud Query Schema")
                .enableAggregate(properties.getAggregate().isEnabled())
                .scalar(
                    VariableValue.class,
                    newScalar()
                        .name("VariableValue")
                        .coercing(
                            new JavaScalars.GraphQLObjectCoercing() {
                                public Object serialize(final Object input) {
                                    return Optional
                                        .ofNullable(input)
                                        .filter(VariableValue.class::isInstance)
                                        .map(VariableValue.class::cast)
                                        .map(it -> Optional.ofNullable(it.getValue()).orElse(Optional.empty()))
                                        .orElseGet(() -> super.serialize(input));
                                }
                            }
                        )
                        .build()
                );

            restrictedKeysProvider.ifAvailable(builder::restrictedKeysProvider);

            Optional
                .ofNullable(properties.getEntities())
                .ifPresent(entities -> entities.forEach(entity -> builder.entityPath(entity.getName())));
        };
    }
}

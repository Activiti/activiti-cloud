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
package org.activiti.cloud.common.swagger;

import static springfox.documentation.schema.AlternateTypeRules.newRule;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;

import org.activiti.cloud.alfresco.rest.model.EntryResponseContent;
import org.activiti.cloud.alfresco.rest.model.ListResponseContent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;

import springfox.documentation.RequestHandler;
import springfox.documentation.builders.AlternateTypeBuilder;
import springfox.documentation.builders.AlternateTypePropertyBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.schema.WildcardType;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.OAuth2Scheme;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.service.SecurityScheme;
import springfox.documentation.service.StringVendorExtension;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class SwaggerDocketBuilder {

    private final TypeResolver typeResolver;
    private final List<DocketCustomizer> docketCustomizers;
    private final BaseAPIInfoBuilder baseAPIInfoBuilder;
    private static final String OAUTH_NAME = "oauth";
    public static final String SERVICE_URL_PREFIX = "service-url-prefix";

    @Value("${keycloak.auth-server-url:}")
    private String authServer;

    @Value("${keycloak.realm:activiti}")
    private String realm;

    public SwaggerDocketBuilder(
            BaseAPIInfoBuilder baseAPIInfoBuilder,
            TypeResolver typeResolver,
            List<DocketCustomizer> docketCustomizers) {
        this.typeResolver = typeResolver;
        this.docketCustomizers = docketCustomizers;
        this.baseAPIInfoBuilder = baseAPIInfoBuilder;
    }

    private Docket baseDocket(
            String title,
            String groupName,
            String serviceURLPrefix,
            Predicate<RequestHandler> apiSelector) {
        Docket baseDocket =
                new Docket(DocumentationType.OAS_30)
                        .groupName(groupName)
                        .apiInfo(baseAPIInfoBuilder.baseApiInfoBuilder(title).build())
                        .extensions(
                                Collections.singletonList(
                                        new StringVendorExtension(
                                                SERVICE_URL_PREFIX, serviceURLPrefix)))
                        .select()
                        .apis(apiSelector)
                        .paths(PathSelectors.any())
                        .build();

        baseDocket
                .forCodeGeneration(true)
                .securitySchemes(Arrays.asList(securitySchema()))
                .securityContexts(Arrays.asList(securityContext()));
        return applyCustomizations(baseDocket);
    }

    private SecurityScheme securitySchema() {
        return new OAuth2Scheme(
                OAUTH_NAME,
                "implicit",
                "Authorizing with SSO",
                authServer + "/realms/" + realm + "/protocol/openid-connect/auth",
                null,
                null,
                Arrays.asList(),
                Arrays.asList());
    }

    private SecurityContext securityContext() {
        return SecurityContext.builder()
                .securityReferences(
                        Arrays.asList(
                                new SecurityReference(OAUTH_NAME, new AuthorizationScope[] {})))
                .build();
    }

    private Docket applyCustomizations(Docket docket) {
        Docket customizedDocket = docket;
        if (docketCustomizers != null) {
            for (DocketCustomizer docketCustomizer : docketCustomizers) {
                customizedDocket = docketCustomizer.customize(customizedDocket);
            }
        }
        return customizedDocket;
    }

    public Docket buildApiDocket(
            String title,
            String groupName,
            String serviceURLPrefix,
            Predicate<RequestHandler> apiSelector) {
        ResolvedType resourceTypeWithWildCard =
                typeResolver.resolve(EntityModel.class, WildcardType.class);
        return baseDocket(title, groupName, serviceURLPrefix, apiSelector)
                .alternateTypeRules(
                        newRule(
                                typeResolver.resolve(
                                        CollectionModel.class, resourceTypeWithWildCard),
                                typeResolver.resolve(
                                        ListResponseContent.class, WildcardType.class)))
                .alternateTypeRules(
                        newRule(
                                typeResolver.resolve(PagedModel.class, resourceTypeWithWildCard),
                                typeResolver.resolve(
                                        ListResponseContent.class, WildcardType.class)))
                .alternateTypeRules(
                        newRule(
                                resourceTypeWithWildCard,
                                typeResolver.resolve(
                                        EntryResponseContent.class, WildcardType.class)))
                .alternateTypeRules(
                        newRule(
                                typeResolver.resolve(Pageable.class),
                                pageableMixin(),
                                Ordered.HIGHEST_PRECEDENCE));
    }

    public Docket buildApiDocket(
            String title, String groupName, String serviceURLPrefix, String basePackage) {
        return buildApiDocket(
                title,
                groupName,
                serviceURLPrefix,
                RequestHandlerSelectors.basePackage(basePackage));
    }

    private Type pageableMixin() {
        return new AlternateTypeBuilder()
                .fullyQualifiedClassName(
                        String.format(
                                "%s.generated.%s",
                                Pageable.class.getPackage().getName(),
                                Pageable.class.getSimpleName()))
                .withProperties(
                        Arrays.asList(
                                property(Integer.class, "skipCount"),
                                property(Integer.class, "maxItems"),
                                property(String.class, "sort")))
                .build();
    }

    private AlternateTypePropertyBuilder property(Class<?> type, String name) {
        return new AlternateTypePropertyBuilder()
                .withName(name)
                .withType(type)
                .withCanRead(true)
                .withCanWrite(true);
    }
}

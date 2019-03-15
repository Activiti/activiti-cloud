/*
 * Copyright 2019 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.common.swagger;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import org.activiti.cloud.alfresco.rest.model.EntryResponseContent;
import org.activiti.cloud.alfresco.rest.model.ListResponseContent;
import org.springframework.core.Ordered;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import springfox.documentation.RequestHandler;
import springfox.documentation.builders.AlternateTypeBuilder;
import springfox.documentation.builders.AlternateTypePropertyBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.schema.WildcardType;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import static springfox.documentation.schema.AlternateTypeRules.newRule;

public class SwaggerDocketBuilder {

    private final TypeResolver typeResolver;
    private final List<DocketCustomizer> docketCustomizers;

    public SwaggerDocketBuilder(TypeResolver typeResolver,
                                List<DocketCustomizer> docketCustomizers) {
        this.typeResolver = typeResolver;
        this.docketCustomizers = docketCustomizers;
    }

    public Docket buildHalAPIDocket(Predicate<RequestHandler> apiSelector) {
        Docket docket = new Docket(DocumentationType.SWAGGER_2)
                .groupName("hal")
                .select()
                .apis(apiSelector::test)
                .paths(PathSelectors.any())
                .build();
        return applyCustomizations(docket);
    }

    private Docket applyCustomizations(Docket docket) {
        Docket customizedDocket = docket;
        if (docketCustomizers != null){
            for (DocketCustomizer docketCustomizer : docketCustomizers) {
                customizedDocket = docketCustomizer.customize(customizedDocket);
            }
        }
        return customizedDocket;
    }

    public Docket buildAlfrescoAPIDocket(Predicate<RequestHandler> apiSelector) {
        ResolvedType resourceTypeWithWildCard = typeResolver.resolve(Resource.class,
                                                                     WildcardType.class);
        Docket docket = new Docket(DocumentationType.SWAGGER_2)
                .alternateTypeRules(newRule(typeResolver.resolve(Resources.class,
                                                                 resourceTypeWithWildCard),
                                            typeResolver.resolve(ListResponseContent.class,
                                                                 WildcardType.class)))
                .alternateTypeRules(newRule(typeResolver.resolve(PagedResources.class,
                                                                 resourceTypeWithWildCard),
                                            typeResolver.resolve(ListResponseContent.class,
                                                                 WildcardType.class)))
                .alternateTypeRules(newRule(resourceTypeWithWildCard,
                                            typeResolver.resolve(EntryResponseContent.class,
                                                                 WildcardType.class)))
                .alternateTypeRules(newRule(typeResolver.resolve(Pageable.class),
                                            pageableMixin(),
                                            Ordered.HIGHEST_PRECEDENCE))
                .select()
                .apis(apiSelector::test)
                .paths(PathSelectors.any())
                .build();
        return applyCustomizations(docket);
    }

    private Type pageableMixin() {
        return new AlternateTypeBuilder()
                .fullyQualifiedClassName(
                        String.format("%s.generated.%s",
                                      Pageable.class.getPackage().getName(),
                                      Pageable.class.getSimpleName()))
                .withProperties(Arrays.asList(
                        property(Integer.class,
                                 "skipCount"),
                        property(Integer.class,
                                 "maxItems"),
                        property(String.class,
                                 "sort")
                ))
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

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

package org.activiti.cloud.common.swagger.springdoc;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.activiti.cloud.common.swagger.springdoc.modelconverter.*;
import org.springframework.boot.info.BuildProperties;
import org.springframework.data.domain.Pageable;

public class BaseOpenApiBuilder {

    public static final String SERVICE_URL_PREFIX = "x-service-url-prefix";

    private final BuildProperties buildProperties;

    private final OAuthFlow swaggerOAuthFlow;

    public BaseOpenApiBuilder(BuildProperties buildProperties, OAuthFlow swaggerOAuthFlow) {
        this.buildProperties = buildProperties;
        this.swaggerOAuthFlow = swaggerOAuthFlow;
        ModelConverters.getInstance().addConverter(new EntityModelConverter());
        ModelConverters.getInstance().addConverter(new CollectionModelConverter());
        ModelConverters.getInstance().addConverter(new PagedModelConverter());
        ModelConverters.getInstance().addConverter(new IgnoredTypesModelConverter());
        SwaggerDocUtils.replaceParameterObjectWithClass(Pageable.class, PageableMixin.class);
    }

    public OpenAPI build(String title, String serviceURLPrefix) {
        OpenAPI openAPI = new OpenAPI()
            .info(
                new Info()
                    .title(title)
                    .version(buildProperties.getVersion())
                    .license(
                        new License()
                            .name(
                                String.format(
                                    "© %s-%s %s. All rights reserved",
                                    buildProperties.get("inceptionYear"),
                                    buildProperties.get("year"),
                                    buildProperties.get("organization.name")
                                )
                            )
                    )
                    .termsOfService(buildProperties.get("organization.url"))
            )
            .components(new Components().addSecuritySchemes("oauth", securityScheme()));
        openAPI.addExtension(SERVICE_URL_PREFIX, serviceURLPrefix);
        return openAPI;
    }

    private SecurityScheme securityScheme() {
        return new SecurityScheme()
            .type(SecurityScheme.Type.OAUTH2)
            .scheme("bearer")
            .bearerFormat("jwt")
            .in(SecurityScheme.In.HEADER)
            .description("Authorizing with SSO")
            .flows(new OAuthFlows().authorizationCode(swaggerOAuthFlow));
    }
}

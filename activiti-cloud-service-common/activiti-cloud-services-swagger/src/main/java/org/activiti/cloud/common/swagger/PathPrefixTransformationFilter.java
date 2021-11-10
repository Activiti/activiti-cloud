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

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;
import springfox.documentation.oas.web.OpenApiTransformationContext;
import springfox.documentation.oas.web.WebMvcOpenApiTransformationFilter;
import springfox.documentation.spi.DocumentationType;

public class PathPrefixTransformationFilter implements WebMvcOpenApiTransformationFilter {

    private final String swaggerBasePath;

    public PathPrefixTransformationFilter(String swaggerBasePath) {
        this.swaggerBasePath = swaggerBasePath;
    }

    @Override
    public OpenAPI transform(OpenApiTransformationContext<HttpServletRequest> context) {
        final OpenAPI openApi = context.getSpecification();
        String servicePrefix = getServicePrefix(openApi);
        final String url = (swaggerBasePath + servicePrefix).replaceAll("//", "/");
        replaceServer(openApi, url);
        return openApi;
    }

    private void replaceServer(OpenAPI openApi, String url) {
        final ArrayList<Server> servers = new ArrayList<>();
        final Server server = new Server();
        server.setUrl(url);
        servers.add(server);
        openApi.setServers(servers);
    }

    private String getServicePrefix(OpenAPI openApi) {
        String servicePrefix = "";
        final String configuredPrefix = (String) openApi.getExtensions().get(SwaggerDocketBuilder.SERVICE_URL_PREFIX);
        if (configuredPrefix != null) {
            servicePrefix = configuredPrefix;
        }
        return servicePrefix;
    }

    @Override
    public boolean supports(DocumentationType documentationType) {
        return DocumentationType.OAS_30.equals(documentationType);
    }
}

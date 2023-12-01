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

package org.activiti.cloud.common.swagger.springdoc.customizer;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import java.util.ArrayList;
import java.util.Optional;
import org.activiti.cloud.common.swagger.springdoc.BaseOpenApiBuilder;

public class PathPrefixOpenApiCustomizer implements DefaultOpenApiCustomizer {

    private final String swaggerBasePath;

    public PathPrefixOpenApiCustomizer(String swaggerBasePath) {
        this.swaggerBasePath = swaggerBasePath;
    }

    @Override
    public void customise(OpenAPI openApi) {
        String servicePrefix = getServicePrefix(openApi);
        final String url = (swaggerBasePath + servicePrefix).replaceAll("//", "/");
        replaceServer(openApi, url);
    }

    private String getServicePrefix(OpenAPI openApi) {
        String servicePrefix = "";
        final String configuredPrefix = (String) Optional
            .ofNullable(openApi.getExtensions())
            .map(extensions -> extensions.get(BaseOpenApiBuilder.SERVICE_URL_PREFIX))
            .orElse(null);
        if (configuredPrefix != null) {
            servicePrefix = configuredPrefix;
        }
        return servicePrefix;
    }

    private void replaceServer(OpenAPI openApi, String url) {
        final ArrayList<Server> servers = new ArrayList<>();
        final Server server = new Server();
        server.setUrl(url);
        servers.add(server);
        openApi.setServers(servers);
    }
}

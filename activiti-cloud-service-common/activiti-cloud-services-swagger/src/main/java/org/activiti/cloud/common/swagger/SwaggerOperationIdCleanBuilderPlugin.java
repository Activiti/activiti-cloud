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

import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.annotation.Order;
import springfox.documentation.service.Operation;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.OperationBuilderPlugin;
import springfox.documentation.spi.service.contexts.OperationContext;
import springfox.documentation.swagger.common.SwaggerPluginSupport;

@Order(SwaggerPluginSupport.SWAGGER_PLUGIN_ORDER + 1000)
public class SwaggerOperationIdCleanBuilderPlugin implements OperationBuilderPlugin {

    private static final String ADMIN_TAG_NAME = "Admin";

    @Override
    public void apply(OperationContext context) {
        Operation operationBuilder = context.operationBuilder().build();
        String uniqueId = operationBuilder.getUniqueId().replaceAll("Using(GET|POST|PUT|DELETE)(_[0-9])?", "");

        uniqueId = checkIfOperationIdBelongsToAdminClass(operationBuilder, uniqueId);

        context.operationBuilder();
        context.operationBuilder().uniqueId(uniqueId);
        context.operationBuilder().codegenMethodNameStem(uniqueId);
    }

    @Override
    public boolean supports(DocumentationType delimiter) {
        return SwaggerPluginSupport.pluginDoesApply(delimiter);
    }

    private String checkIfOperationIdBelongsToAdminClass(Operation operationBuilder, String uniqueId) {
        Set<String> filteredTags = operationBuilder.getTags()
            .stream()
            .filter(string -> string.contains("admin"))
            .collect(Collectors.toSet());

        if(!filteredTags.isEmpty()){
            uniqueId += ADMIN_TAG_NAME;
        }
        return uniqueId;
    }


}

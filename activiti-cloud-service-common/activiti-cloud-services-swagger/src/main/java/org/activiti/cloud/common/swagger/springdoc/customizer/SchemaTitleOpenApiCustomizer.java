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
import org.apache.commons.lang3.StringUtils;
import org.springdoc.core.customizers.OpenApiCustomiser;

public class SchemaTitleOpenApiCustomizer implements OpenApiCustomiser {

    @Override
    public void customise(OpenAPI openApi) {
        openApi.getComponents().getSchemas().forEach((schemaName, schema) -> {
            if (StringUtils.isBlank(schema.getTitle())) {
                schema.setTitle(schemaName);
            }
        });
    }
}

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
package org.activiti.cloud.services.organization.validation;

import java.io.IOException;
import java.io.InputStream;

import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
public class JsonSchemaModelValidatorConfiguration {

    @Value("${activiti.validation.connector-schema:schema/connector-schema.json}")
    private String connectorSchema;

    @Bean(name = "connectorSchemaLoader")
    public SchemaLoader getConnectorSchemaLoader() throws IOException {
        return buildSchemaLoaderFromClasspath(connectorSchema);
    }

    private SchemaLoader buildSchemaLoaderFromClasspath(String schemaFileName) throws IOException {
        try (InputStream schemaInputStream = new ClassPathResource(schemaFileName).getInputStream()) {
            JSONObject jsonSchema = new JSONObject(new JSONTokener(schemaInputStream));
            return SchemaLoader
                    .builder()
                    .schemaJson(jsonSchema)
                    .draftV7Support()
                    .build();
        }
    }
}

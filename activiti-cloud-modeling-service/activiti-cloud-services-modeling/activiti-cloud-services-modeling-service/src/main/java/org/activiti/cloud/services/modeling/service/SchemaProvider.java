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
package org.activiti.cloud.services.modeling.service;

import java.io.IOException;
import java.io.InputStream;
import org.activiti.cloud.services.modeling.validation.JsonSchemaFlattener;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

public class SchemaProvider {

    private static final Logger logger = LoggerFactory.getLogger(
        SchemaProvider.class
    );

    private String modelType;
    private String schemaFileName;

    public SchemaProvider(String modelType, String schemaFileName) {
        this.modelType = modelType;
        this.schemaFileName = schemaFileName;
    }

    public String getModelType() {
        return modelType;
    }

    public String getSchemaFileName() {
        return schemaFileName;
    }

    public JSONObject getJsonSchemaForType() {
        JSONObject schema = null;
        if (schemaFileName != null) {
            try (
                InputStream inputStream = new ClassPathResource(schemaFileName)
                    .getInputStream()
            ) {
                schema =
                    new JsonSchemaFlattener()
                        .flatten(new JSONObject(new JSONTokener(inputStream)));
            } catch (IOException e) {
                logger.error(
                    "Unable to read schema for model type {} in file {}: {}",
                    modelType,
                    schemaFileName,
                    e
                );
            }
        }
        return schema;
    }
}

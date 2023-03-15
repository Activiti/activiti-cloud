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

import java.util.List;
import java.util.Optional;
import org.json.JSONObject;

public class SchemaService {

    public static final String PROCESS_EXTENSION = "PROCESS-EXTENSION";

    private List<SchemaProvider> schemaProviders;

    public SchemaService(List<SchemaProvider> schemaProviders) {
        this.schemaProviders = schemaProviders;
    }

    public Optional<JSONObject> getJsonSchemaFromType(String schemaType) {
        return schemaProviders
            .stream()
            .filter(schema -> schema.getModelType().equalsIgnoreCase(schemaType))
            .findFirst()
            .map(SchemaProvider::getJsonSchemaForType);
    }
}

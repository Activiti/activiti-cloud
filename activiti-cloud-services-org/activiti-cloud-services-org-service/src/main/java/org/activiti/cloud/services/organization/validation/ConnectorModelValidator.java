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

import org.activiti.cloud.organization.api.ConnectorModelType;
import org.activiti.cloud.organization.api.ModelType;
import org.activiti.cloud.organization.api.ModelValidator;
import org.everit.json.schema.loader.SchemaLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * {@link ModelValidator} implementation of connector models
 */
@Component
public class ConnectorModelValidator extends JsonSchemaModelValidator {

    private final SchemaLoader connectorSchemaLoader;

    private final ConnectorModelType connectorModelType;

    @Autowired
    public ConnectorModelValidator(SchemaLoader connectorSchemaLoader,
                                   ConnectorModelType connectorModelType) {
        this.connectorSchemaLoader = connectorSchemaLoader;
        this.connectorModelType = connectorModelType;
    }

    @Override
    public ModelType getHandledModelType() {
        return connectorModelType;
    }

    @Override
    public SchemaLoader schemaLoader() {
        return connectorSchemaLoader;
    }
}

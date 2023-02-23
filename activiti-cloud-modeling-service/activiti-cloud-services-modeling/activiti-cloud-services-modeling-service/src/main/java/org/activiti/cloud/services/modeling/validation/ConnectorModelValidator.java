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
package org.activiti.cloud.services.modeling.validation;

import static org.activiti.cloud.services.common.util.ContentTypeUtils.CONTENT_TYPE_JSON;

import java.util.Collection;
import org.activiti.cloud.api.error.ModelingException;
import org.activiti.cloud.modeling.api.ConnectorModelType;
import org.activiti.cloud.modeling.api.ModelContentValidator;
import org.activiti.cloud.modeling.api.ModelType;
import org.activiti.cloud.modeling.api.ModelValidator;
import org.activiti.cloud.modeling.api.ValidationContext;
import org.everit.json.schema.loader.SchemaLoader;

/**
 * {@link ModelValidator} implementation of connector models
 */
public class ConnectorModelValidator extends JsonSchemaModelValidator implements ModelContentValidator {

    private final SchemaLoader connectorSchemaLoader;

    private final ConnectorModelType connectorModelType;

    public ConnectorModelValidator(SchemaLoader connectorSchemaLoader, ConnectorModelType connectorModelType) {
        this.connectorSchemaLoader = connectorSchemaLoader;
        this.connectorModelType = connectorModelType;
    }

    @Override
    public Collection<ModelingException> validateAndReturnErrors(byte[] modelContent,
                                                                 ValidationContext validationContext) {
        return null;
    }

    @Override
    public ModelType getHandledModelType() {
        return connectorModelType;
    }

    @Override
    public String getHandledContentType() {
        return CONTENT_TYPE_JSON;
    }

    @Override
    public SchemaLoader schemaLoader() {
        return connectorSchemaLoader;
    }
}

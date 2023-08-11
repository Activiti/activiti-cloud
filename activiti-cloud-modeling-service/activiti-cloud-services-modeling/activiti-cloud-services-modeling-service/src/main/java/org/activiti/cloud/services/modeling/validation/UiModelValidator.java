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

import java.util.ArrayList;
import java.util.Collection;
import org.activiti.cloud.modeling.api.ModelContentValidator;
import org.activiti.cloud.modeling.api.ModelType;
import org.activiti.cloud.modeling.api.ModelValidationError;
import org.activiti.cloud.modeling.api.UIModelType;
import org.activiti.cloud.modeling.api.ValidationContext;
import org.everit.json.schema.Schema;

public class UiModelValidator extends JsonSchemaModelValidator implements ModelContentValidator {

    private final Schema uiSchema;

    private final UIModelType uiModelType;

    public UiModelValidator(Schema uiSchema, UIModelType uiModelType) {
        this.uiSchema = uiSchema;
        this.uiModelType = uiModelType;
    }

    @Override
    public ModelType getHandledModelType() {
        return uiModelType;
    }

    @Override
    public String getHandledContentType() {
        return CONTENT_TYPE_JSON;
    }

    @Override
    public Schema schema() {
        return null;
    }

    @Override
    public Collection<ModelValidationError> validate(byte[] modelContent, ValidationContext validationContext) {
        Collection<ModelValidationError> parentErrors = new ArrayList<>(
            super.validate(modelContent, validationContext)
        );

        return parentErrors;
    }
}

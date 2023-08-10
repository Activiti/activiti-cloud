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

package org.activiti.cloud.services.modeling.validation.process;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.cloud.modeling.api.ModelValidationError;
import org.activiti.cloud.modeling.api.ValidationContext;

public class BpmnModelValidator implements BpmnCommonModelValidator {

    public final String ERROR_TYPE = "Missing process category";

    public final String ERROR_DESCRIPTION = "The process category needs to be set";

    @Override
    public Stream<ModelValidationError> validate(BpmnModel bpmnModel,
        ValidationContext validationContext) {
        return validateTargetNamespace(bpmnModel);
    }

    public Stream<ModelValidationError> validateTargetNamespace(BpmnModel bpmnModel) {
        List<ModelValidationError> aggregatedErrors = new ArrayList();
        if(bpmnModel.getTargetNamespace() == null){
            aggregatedErrors.add(new ModelValidationError(ERROR_TYPE,
                ERROR_DESCRIPTION));
        }
        return aggregatedErrors.stream();
    }

}

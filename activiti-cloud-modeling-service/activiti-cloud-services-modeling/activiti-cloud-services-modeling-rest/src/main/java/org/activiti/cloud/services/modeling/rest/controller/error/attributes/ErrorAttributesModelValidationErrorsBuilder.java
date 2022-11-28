package org.activiti.cloud.services.modeling.rest.controller.error.attributes;

import org.activiti.cloud.common.error.attributes.ErrorAttributesCustomizer;
import org.activiti.cloud.modeling.api.ModelValidationError;
import org.activiti.cloud.modeling.core.error.SemanticModelValidationException;
import org.springframework.validation.ObjectError;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ErrorAttributesModelValidationErrorsBuilder implements ErrorAttributesCustomizer {

    public static final String ERRORS = "errors";

    @Override
    public Map<String, Object> customize(Map<String, Object> errorAttributes, Throwable error) {

        Stream<ModelValidationError> bindingErrors = Optional.ofNullable((List<ObjectError>) errorAttributes.get(ERRORS))
            .map(this::transformBindingErrors)
            .orElse(Stream.empty());
        Stream<ModelValidationError> semanticErrors = resolveSemanticErrors(error,
            errorAttributes);
        Stream<ModelValidationError> modelValidationErrorStream = Stream.concat(bindingErrors,
            semanticErrors);
        List<ModelValidationError> collectedErrors = modelValidationErrorStream.collect(Collectors.toList());
        if (!collectedErrors.isEmpty()) {
            errorAttributes.put(ERRORS,
                collectedErrors);
        }

        return errorAttributes;
    }

    private Stream<ModelValidationError> resolveSemanticErrors(Throwable error,
                                                               Map<String, Object> errorAttributes) {
        return Optional.ofNullable(error)
            .filter(SemanticModelValidationException.class::isInstance)
            .map(SemanticModelValidationException.class::cast)
            .map(SemanticModelValidationException::getValidationErrors)
            .map(Collection::stream)
            .orElse(Stream.empty());
    }

    private Stream<ModelValidationError> transformBindingErrors(List<ObjectError> errors) {
        return errors.stream()
            .map(this::createModelValidationError);
    }

    private ModelValidationError createModelValidationError(ObjectError objectError) {
        ModelValidationError modelValidationError = new ModelValidationError();
        modelValidationError.setWarning(false);
        modelValidationError.setProblem(objectError.getCode());
        modelValidationError.setDescription(objectError.getDefaultMessage());
        modelValidationError.setValidatorSetName(objectError.getObjectName());
        return modelValidationError;
    }
}

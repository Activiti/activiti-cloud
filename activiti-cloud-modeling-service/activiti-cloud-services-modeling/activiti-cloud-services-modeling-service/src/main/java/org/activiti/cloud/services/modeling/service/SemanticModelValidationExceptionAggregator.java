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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.activiti.cloud.modeling.api.ModelValidationError;
import org.activiti.cloud.modeling.api.ModelValidator;
import org.activiti.cloud.modeling.core.error.ModelingException;
import org.activiti.cloud.modeling.core.error.SemanticModelValidationException;
import org.springframework.lang.NonNull;

public class SemanticModelValidationExceptionAggregator<V extends ModelValidator> {

    private final static String ERROR_MESSAGE = "Semantic model validation errors encountered: %d schema violations found";

    private final static String WARNING_MESSAGE = "Semantic model validation warnings encountered: %d warnings found";

    private final Collection<V> validators;
    private final ValidationCallback callback;

    public SemanticModelValidationExceptionAggregator(@NonNull Collection<V> validators, @NonNull ValidationCallback<V> callback) {
        this.validators = validators;
        this.callback = callback;
    }

    private List<SemanticModelValidationException> getSemanticModelValidationExceptions() {
        final List<SemanticModelValidationException> semanticModelValidationExceptionList = new LinkedList<>();

        this.validators.stream().forEach(modelValidator -> {
            try {
                callback.accept(modelValidator);
            } catch(SemanticModelValidationException e) {
                semanticModelValidationExceptionList.add(e);
            }
        });

        return semanticModelValidationExceptionList;
    }

    public List<ModelValidationError> getValidationErrors() {
        final List<SemanticModelValidationException> semanticModelValidationExceptionList = getSemanticModelValidationExceptions();

        return getValidationErrors(semanticModelValidationExceptionList);
    }

    protected List<ModelValidationError> getValidationErrors(@NonNull List<SemanticModelValidationException> semanticModelValidationExceptionList) {
        return semanticModelValidationExceptionList.stream().filter(validationException -> validationException.getValidationErrors() != null)
            .flatMap(validationException -> validationException.getValidationErrors().stream()).collect(Collectors.toList());
    }

    public void validate() throws ModelingException {
        final List<SemanticModelValidationException> validationExceptions = getSemanticModelValidationExceptions();
        throwExceptionIfNeeded(validationExceptions);
    }

    private void throwExceptionIfNeeded(@NonNull List<SemanticModelValidationException> validationExceptions) {
        if(!validationExceptions.isEmpty()){
            if (validationExceptions.size() == 1) {
                throw validationExceptions.get(0);
            } else {
                final List<ModelValidationError> modelValidationErrors = getValidationErrors(validationExceptions);

                final long errorCount = modelValidationErrors.stream().filter(modelValidationError -> !modelValidationError.isWarning()).count();
                if(errorCount > 0) {
                    throw new SemanticModelValidationException(String.format(ERROR_MESSAGE, errorCount), modelValidationErrors);
                }

                throw new SemanticModelValidationException(String.format(WARNING_MESSAGE, modelValidationErrors.size()), modelValidationErrors);
            }
        }
    }

    public static interface ValidationCallback<T extends ModelValidator> extends Consumer<T> {
        @Override
        void accept(T validator) throws ModelingException;
    }
}

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
package org.activiti.cloud.services.modeling.service.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.activiti.cloud.api.error.ModelingException;
import org.activiti.cloud.modeling.api.ModelValidationError;
import org.activiti.cloud.modeling.api.ModelValidator;
import org.activiti.cloud.modeling.core.error.SemanticModelValidationException;
import org.springframework.lang.NonNull;

public class AggregateErrorValidationStrategy<V extends ModelValidator> implements ValidationStrategy<V> {

    private static final String ERROR_MESSAGE =
        "Semantic model validation errors encountered: %d schema violations found";

    private static final String WARNING_MESSAGE = "Semantic model validation warnings encountered: %d warnings found";

    protected List<SemanticModelValidationException> getSemanticModelValidationExceptions(
        @NonNull Collection<V> validators,
        @NonNull ValidationCallback<V> callback
    ) {
        final List<SemanticModelValidationException> semanticModelValidationExceptionList = new ArrayList<>();

        validators
            .stream()
            .forEach(modelValidator -> {
                try {
                    callback.accept(modelValidator);
                } catch (SemanticModelValidationException e) {
                    semanticModelValidationExceptionList.add(e);
                }
            });

        return semanticModelValidationExceptionList;
    }

    @Override
    public List<ModelValidationError> getValidationErrors(
        @NonNull Collection<V> validators,
        @NonNull ValidationCallback<V> callback
    ) {
        final List<SemanticModelValidationException> semanticModelValidationExceptionList = getSemanticModelValidationExceptions(
            validators,
            callback
        );

        return getValidationErrors(semanticModelValidationExceptionList);
    }

    @Override
    public void validate(@NonNull Collection<V> validators, @NonNull ValidationCallback<V> callback)
        throws ModelingException {
        final List<SemanticModelValidationException> validationExceptions = getSemanticModelValidationExceptions(
            validators,
            callback
        );
        throwExceptionIfNeeded(validationExceptions);
    }

    protected List<ModelValidationError> getValidationErrors(
        @NonNull List<SemanticModelValidationException> semanticModelValidationExceptionList
    ) {
        return semanticModelValidationExceptionList
            .stream()
            .filter(validationException -> validationException.getValidationErrors() != null)
            .flatMap(validationException -> validationException.getValidationErrors().stream())
            .distinct()
            .collect(Collectors.toList());
    }

    private void throwExceptionIfNeeded(@NonNull List<SemanticModelValidationException> validationExceptions) {
        if (!validationExceptions.isEmpty()) {
            if (validationExceptions.size() == 1) {
                throw validationExceptions.stream().findFirst().get();
            } else {
                final List<ModelValidationError> modelValidationErrors = getValidationErrors(validationExceptions);

                if (
                    modelValidationErrors.stream().anyMatch(modelValidationError -> !modelValidationError.isWarning())
                ) {
                    throw new SemanticModelValidationException(
                        String.format(
                            ERROR_MESSAGE,
                            modelValidationErrors
                                .stream()
                                .filter(modelValidationError -> !modelValidationError.isWarning())
                                .count()
                        ),
                        modelValidationErrors
                    );
                }

                throw new SemanticModelValidationException(
                    String.format(WARNING_MESSAGE, modelValidationErrors.size()),
                    modelValidationErrors
                );
            }
        }
    }
}

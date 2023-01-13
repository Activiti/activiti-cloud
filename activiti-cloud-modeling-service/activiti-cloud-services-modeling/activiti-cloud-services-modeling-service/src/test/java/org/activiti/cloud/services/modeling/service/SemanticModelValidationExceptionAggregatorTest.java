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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.activiti.cloud.modeling.api.Model;
import org.activiti.cloud.modeling.api.ModelContentValidator;
import org.activiti.cloud.modeling.api.ModelValidationError;
import org.activiti.cloud.modeling.api.ValidationContext;
import org.activiti.cloud.modeling.core.error.SemanticModelValidationException;
import org.activiti.cloud.modeling.core.error.SyntacticModelValidationException;
import org.activiti.cloud.services.modeling.service.utils.SemanticModelValidationExceptionAggregator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SemanticModelValidationExceptionAggregatorTest {

    @Mock
    private Model model;

    @Mock
    private ModelContentValidator modelContentValidator;

    @Mock
    private ModelContentValidator otherModelContentValidator;

    @Test
    public void should_returnEmpty_whenValidatorsDoNotThrowAnyException() {

        SemanticModelValidationExceptionAggregator<ModelContentValidator> exceptionAggregator =
            new SemanticModelValidationExceptionAggregator<>(
                List.of(modelContentValidator),
                validator -> {}
            );

        assertDoesNotThrow(() -> exceptionAggregator.validate());
    }

    @Test
    public void should_returnAggregatedException_whenValidatorsThrowSemanticException() {

        final List<ModelValidationError> validatorErrors = List.of(
            new ModelValidationError("Problem 1", "Description 1"),
            new ModelValidationError("Problem 2", "Description 2")
        );

        final List<ModelValidationError> otherValidatorErrors = List.of(
            new ModelValidationError("Problem 3", "Description 3")
        );

        doThrow(new SemanticModelValidationException("Test Syntactic Exception", validatorErrors))
            .when(modelContentValidator)
            .validateModelContent(any(), any());

        doThrow(new SemanticModelValidationException("Test Syntactic Exception", otherValidatorErrors))
            .when(otherModelContentValidator)
            .validateModelContent(any(), any());

        SemanticModelValidationExceptionAggregator<ModelContentValidator> exceptionAggregator =
            new SemanticModelValidationExceptionAggregator<>(
                List.of(modelContentValidator, otherModelContentValidator),
                validator -> validator.validateModelContent(model.getContent(), ValidationContext.EMPTY_CONTEXT)
            );

        SemanticModelValidationException exception = assertThrows(SemanticModelValidationException.class, () -> exceptionAggregator.validate());
        assertThat(exception).hasMessage("Semantic model validation errors encountered: 3 schema violations found");
        assertThat(exception.getValidationErrors()).isNotNull().hasSize(3).containsExactly(Stream.concat(validatorErrors.stream(),
                otherValidatorErrors.stream()).collect(Collectors.toList()).toArray(ModelValidationError[]::new));
    }

    @Test
    public void should_returnException_whenAValidatorDoesNotThrowSemanticException() {

        doThrow(new SyntacticModelValidationException("Test Syntactic Exception"))
            .when(modelContentValidator)
            .validateModelContent(any(), any());

        final List<ModelValidationError> otherValidatorErrors = List.of(
            new ModelValidationError("Problem 3", "Description 3")
        );

        doThrow(new SemanticModelValidationException("Test Syntactic Exception", otherValidatorErrors))
            .when(otherModelContentValidator)
            .validateModelContent(any(), any());

        SemanticModelValidationExceptionAggregator<ModelContentValidator> exceptionAggregator =
            new SemanticModelValidationExceptionAggregator<>(
                List.of(modelContentValidator, otherModelContentValidator),
                validator -> validator.validateModelContent(model.getContent(), ValidationContext.EMPTY_CONTEXT)
                );

        assertThrows(SyntacticModelValidationException.class, () -> exceptionAggregator.validate());
    }
}

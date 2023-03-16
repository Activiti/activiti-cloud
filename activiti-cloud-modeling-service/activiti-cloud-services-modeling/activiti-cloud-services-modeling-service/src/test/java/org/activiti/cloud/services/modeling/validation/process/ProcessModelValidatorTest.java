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
package org.activiti.cloud.services.modeling.validation.process;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.cloud.modeling.api.ModelValidationError;
import org.activiti.cloud.modeling.api.ProcessModelType;
import org.activiti.cloud.modeling.core.error.SemanticModelValidationException;
import org.activiti.cloud.services.modeling.converter.ProcessModelContentConverter;
import org.activiti.cloud.services.modeling.validation.ProjectValidationContext;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessModelValidatorTest {

    @InjectMocks
    private ProcessModelValidator processModelValidator;

    @Mock
    private ProcessModelType processModelType;

    @Mock
    private ProcessModelContentConverter processModelContentConverter;

    @Spy
    private Set<BpmnCommonModelValidator> bpmnCommonModelValidators =
        new HashSet<>(Arrays.asList(new BpmnModelValidator(),
                                    new BpmnModelUniqueIdValidator(
                                        processModelType,
                                        processModelContentConverter)));

    @Test
    void should_validateWithNoErrors_when_categoryIsSet() {
        BpmnModel bpmnModel = new BpmnModel();
        byte[] bytesFromModel = bpmnModel.toString().getBytes();
        bpmnModel.setTargetNamespace("test-category");

        given(processModelContentConverter.convertToBpmnModel(bytesFromModel))
            .willReturn(bpmnModel);

        Throwable exception = catchThrowable(() ->
                                                 processModelValidator.validate(bytesFromModel,
                                                                                new ProjectValidationContext()));

        assertThat(exception).isNull();
    }

    @Test
    void should_validateWithAndReturnNoErrors_when_categoryIsSet() {
        BpmnModel bpmnModel = new BpmnModel();
        byte[] bytesFromModel = bpmnModel.toString().getBytes();
        bpmnModel.setTargetNamespace("test-category");

    @Test
    void should_validateWithAndReturnNoErrors_when_categoryIsSet() {
        BpmnModel bpmnModel = new BpmnModel();
        byte[] bytesFromModel = bpmnModel.toString().getBytes();
        bpmnModel.setTargetNamespace("test-category");

        given(processModelContentConverter.convertToBpmnModel(bytesFromModel))
            .willReturn(bpmnModel);

        Collection<ModelValidationError> modelValidationErrors = processModelValidator.validate(
            bytesFromModel,
            new ProjectValidationContext());

        assertThat(modelValidationErrors).isEmpty();
    }

    @Test
    void should_validateWithNoErrors_when_categoryIsEmpty() {
        BpmnModel bpmnModel = new BpmnModel();
        byte[] bytesFromModel = bpmnModel.toString().getBytes();
        bpmnModel.setTargetNamespace("");

        given(processModelContentConverter.convertToBpmnModel(bytesFromModel))
            .willReturn(bpmnModel);

        Throwable exception = catchThrowable(() ->
                                                 processModelValidator.validate(bytesFromModel,
                                                                                new ProjectValidationContext()));

        assertThat(exception).isNull();
    }

    @Test
    void should_validateAndReturnNoErrors_when_categoryIsEmpty() {
        BpmnModel bpmnModel = new BpmnModel();
        byte[] bytesFromModel = bpmnModel.toString().getBytes();
        bpmnModel.setTargetNamespace("");

        given(processModelContentConverter.convertToBpmnModel(bytesFromModel))
            .willReturn(bpmnModel);

        Collection<ModelValidationError> modelValidationErrors = processModelValidator.validate(
            bytesFromModel,
            new ProjectValidationContext());

        assertThat(modelValidationErrors).isEmpty();
    }

    @Test
    void should_validateWithErrors_when_categoryIsNotSet() {
        BpmnModel bpmnModel = new BpmnModel();
        byte[] bytesFromModel = bpmnModel.toString().getBytes();

        given(processModelContentConverter.convertToBpmnModel(bytesFromModel))
            .willReturn(bpmnModel);

        Collection<ModelValidationError> modelValidationErrors = processModelValidator.validate(bytesFromModel,
                                                                                                new ProjectValidationContext());

        assertThat(modelValidationErrors).hasSize(1);
        ModelValidationError validationError = modelValidationErrors.iterator().next();
        assertThat(validationError.getProblem()).isEqualTo("Missing process category");
        assertThat(validationError.getDescription()).isEqualTo("The process category needs to be set");
    }

    @Test
    void should_validateAndReturnErrors_when_categoryIsNotSet() {
        BpmnModel bpmnModel = new BpmnModel();
        byte[] bytesFromModel = bpmnModel.toString().getBytes();

        given(processModelContentConverter.convertToBpmnModel(bytesFromModel))
            .willReturn(bpmnModel);

        Collection<ModelValidationError> modelValidationErrors = processModelValidator.validate(
            bytesFromModel,
            new ProjectValidationContext());

        assertThat(modelValidationErrors)
            .hasSize(1)
            .element(0)
            .has(new Condition<>(error -> "The process category needs to be set".equals(error.getDescription())
                && "Missing process category".equals(error.getProblem()), "Error assertions"));
    }
}

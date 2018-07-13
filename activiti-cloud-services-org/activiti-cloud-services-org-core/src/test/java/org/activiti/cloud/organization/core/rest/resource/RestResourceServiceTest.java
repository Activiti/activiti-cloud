/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
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

package org.activiti.cloud.organization.core.rest.resource;

import org.activiti.cloud.organization.api.ModelType;
import org.activiti.cloud.organization.core.mock.ModelMock;
import org.activiti.cloud.organization.core.model.ModelReference;
import org.activiti.cloud.organization.core.rest.client.ModelService;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.activiti.cloud.organization.api.ModelType.PROCESS;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link RestResourceService}
 */
@RunWith(MockitoJUnitRunner.class)
public class RestResourceServiceTest {

    @Spy
    private ModelService modelService = mock(ModelService.class);

    @InjectMocks
    private ModelRestResourceService restResourceService;

    @Captor
    private ArgumentCaptor<ModelReference> modelReferenceCaptor;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testLoadRestResourceIntoEntityField() {

        // GIVEN
        ModelMock processModel = new ModelMock();
        processModel.setType(PROCESS);
        processModel.setRefId("process_model_refId");

        ModelReference processModelReference = new ModelReference("process_model_refId",
                                                                  "Process Model");
        doReturn(processModelReference).when(modelService).getResource(eq(PROCESS),
                                                                       eq("process_model_refId"));

        // WHEN
        restResourceService.loadRestResourceIntoEntityField(processModel,
                                                            "data",
                                                            "type",
                                                            "refId");

        // THEN
        assertThat(processModel.getData()).isNotNull();
        assertThat(processModel.getData().getModelId()).isEqualTo("process_model_refId");
        assertThat(processModel.getData().getName()).isEqualTo("Process Model");
    }

    @Test
    public void testLoadRestResourceWhenNoResourceProvided() {

        // GIVEN
        ModelMock processModel = new ModelMock();
        processModel.setType(PROCESS);
        processModel.setRefId("process_model_refId");
        processModel.setData(null);

        doThrow(RuntimeException.class).when(modelService).getResource(eq(PROCESS),
                                                                       eq("process_model_refId"));

        // WHEN
        restResourceService.loadRestResourceIntoEntityField(processModel,
                                                            "data",
                                                            "type",
                                                            "refId");

        // THEN
        verify(modelService,
               times(1))
                .getResource(PROCESS,
                             "process_model_refId");

        assertThat(processModel.getData()).isNull();
    }

    @Test
    public void testLoadRestResourceInvalidResourceKeyFieldName() {

        // THEN
        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage(
                "Cannot access field 'invalid_field_name' of entity type 'class org.activiti.cloud.organization.core.mock.ModelMock'");

        // WHEN
        restResourceService.loadRestResourceIntoEntityField(new ModelMock(),
                                                            "data",
                                                            "invalid_field_name",
                                                            "refId");
    }

    @Test
    public void testLoadRestResourceInvalidResourceIdFieldName() {

        // THEN
        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage(
                "Cannot access field 'invalid_field_name' of entity type 'class org.activiti.cloud.organization.core.mock.ModelMock'");

        // WHEN
        restResourceService.loadRestResourceIntoEntityField(new ModelMock(),
                                                            "data",
                                                            "type",
                                                            "invalid_field_name");
    }

    @Test
    public void testLoadRestResourceInvalidTargetFieldName() {

        // THEN
        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage(
                "Cannot set value to the target field 'invalid_field_name' of entity type 'class org.activiti.cloud.organization.core.mock.ModelMock'");

        // WHEN
        restResourceService.loadRestResourceIntoEntityField(new ModelMock(),
                                                            "invalid_field_name",
                                                            "type",
                                                            "refId");
    }

    @Test
    public void testLoadRestResourceWrongTargetFieldName() {

        // GIVEN
        ModelMock processModel = new ModelMock();
        processModel.setType(PROCESS);
        processModel.setRefId("process_model_refId");

        ModelReference processModelReference = new ModelReference("process_model_refId",
                                                                  "Process Model");
        doReturn(processModelReference).when(modelService).getResource(eq(PROCESS),
                                                                       eq("process_model_refId"));

        // THEN
        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage(//"argument type mismatch"
                                        "Cannot set value to the target field 'id' of entity type 'class org.activiti.cloud.organization.core.mock.ModelMock'");

        // WHEN
        restResourceService.loadRestResourceIntoEntityField(processModel,
                                                            "id",
                                                            "type",
                                                            "refId");
    }

    @Test
    public void testCreateRestResourceFromEntityField() {

        // GIVEN
        ModelMock processModel = new ModelMock("process_model_id",
                                               "Process Model",
                                               PROCESS,
                                               "process_model_refId");

        // WHEN
        restResourceService.saveRestResourceFromEntityField(processModel,
                                                            "data",
                                                            "type",
                                                            "refId",
                                                            false);

        // THEN
        verify(modelService,
               never())
                .updateResource(eq(PROCESS),
                                eq("process_model_refId"),
                                any(ModelReference.class));

        verify(modelService,
               times(1))
                .createResource(any(ModelType.class),
                                modelReferenceCaptor.capture());

        ModelReference createdModelReference = modelReferenceCaptor.getValue();
        assertThat(createdModelReference).isNotNull();
        assertThat(createdModelReference.getModelId()).isEqualTo("process_model_refId");
        assertThat(createdModelReference.getName()).isEqualTo("Process Model");
    }

    @Test
    public void testUpdateRestResourceFromEntityField() {

        // GIVEN
        ModelMock processModel = new ModelMock("process_model_id",
                                               "Process Model",
                                               PROCESS,
                                               "process_model_refId");

        // WHEN
        restResourceService.saveRestResourceFromEntityField(processModel,
                                                            "data",
                                                            "type",
                                                            "refId",
                                                            true);

        // THEN
        verify(modelService,
               never())
                .createResource(any(ModelType.class),
                                any(ModelReference.class));

        verify(modelService,
               times(1))
                .updateResource(eq(PROCESS),
                                eq("process_model_refId"),
                                modelReferenceCaptor.capture());

        ModelReference updatedModelReference = modelReferenceCaptor.getValue();
        assertThat(updatedModelReference).isNotNull();
        assertThat(updatedModelReference.getModelId()).isEqualTo("process_model_refId");
        assertThat(updatedModelReference.getName()).isEqualTo("Process Model");
    }

    @Test
    public void testSaveRestResourceFromEntityFieldWithNoDataToSave() {

        // GIVEN
        ModelMock processModel = new ModelMock();

        // WHEN
        restResourceService.saveRestResourceFromEntityField(processModel,
                                                            "data",
                                                            "type",
                                                            "refId",
                                                            false);

        // THEN
        verify(modelService,
               never())
                .updateResource(any(ModelType.class),
                                anyString(),
                                any(ModelReference.class));

        verify(modelService,
               never())
                .createResource(any(ModelType.class),
                                any(ModelReference.class));
    }
}

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

package org.activiti.cloud.organization.core.rest.client.feign;

import java.util.HashMap;
import java.util.Map;

import org.activiti.cloud.organization.api.ModelType;
import org.activiti.cloud.organization.core.rest.client.model.ModelReference;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.activiti.cloud.organization.api.ModelType.FORM;
import static org.activiti.cloud.organization.api.ModelType.PROCESS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Tests for {@link FeignModelReferenceService}
 */
public class FeignModelServiceTest {

    @Mock
    private ProcessModelReferenceService processModelService;

    @Mock
    private FormModelReferenceService formModelService;

    private FeignModelReferenceService feignModelService = new FeignModelReferenceService();

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        Map<ModelType, BaseModelService<ModelReference>> modelServices = new HashMap<>();
        modelServices.put(PROCESS,
                          processModelService);
        modelServices.put(FORM,
                          formModelService);

        FieldUtils.writeField(feignModelService,
                              "modelServices",
                              modelServices,
                              true);
    }

    @Test
    public void testGetResource() {
        // WHEN
        feignModelService.getResource(PROCESS,
                                      "process_model_id");

        // THEN
        verify(processModelService,
               times(1))
                .getResource("process_model_id");

        // WHEN
        feignModelService.getResource(FORM,
                                      "form_model_id");

        // THEN
        verify(formModelService,
               times(1))
                .getResource("form_model_id");
    }

    @Test
    public void testCreateResource() {
        // WHEN
        feignModelService.createResource(PROCESS,
                                         mock(ModelReference.class));

        // THEN
        verify(processModelService,
               times(1))
                .createResource(any(ModelReference.class));

        // WHEN
        feignModelService.createResource(FORM,
                                         mock(ModelReference.class));

        // THEN
        verify(formModelService,
               times(1))
                .createResource(any(ModelReference.class));
    }

    @Test
    public void testUpdateResource() {
        // WHEN
        feignModelService.updateResource(PROCESS,
                                         "process_model_id",
                                         mock(ModelReference.class));

        // THEN
        verify(processModelService,
               times(1))
                .updateResource(eq("process_model_id"),
                                any(ModelReference.class));

        // WHEN
        feignModelService.updateResource(FORM,
                                         "form_model_id",
                                         mock(ModelReference.class));

        // THEN
        verify(formModelService,
               times(1))
                .updateResource(eq("form_model_id"),
                                any(ModelReference.class));
    }

    @Test
    public void testDeleteResource() {
        // WHEN
        feignModelService.deleteResource(PROCESS,
                                         "process_model_id");

        // THEN
        verify(processModelService,
               times(1))
                .deleteResource(eq("process_model_id"));

        // WHEN
        feignModelService.deleteResource(FORM,
                                         "form_model_id");

        // THEN
        verify(formModelService,
               times(1))
                .deleteResource(eq("form_model_id"));
    }

    @Test
    public void testvalidateResourceContent() {
        // WHEN
        feignModelService.validateResourceContent(PROCESS,
                                                  "processModelConent".getBytes());

        // THEN
        verify(processModelService,
               times(1))
                .validateResourceContent(eq("processModelConent".getBytes()));

        // WHEN
        feignModelService.validateResourceContent(FORM,
                                                  "formModelConent".getBytes());

        // THEN
        verify(formModelService,
               times(1))
                .validateResourceContent(eq("formModelConent".getBytes()));
    }
}

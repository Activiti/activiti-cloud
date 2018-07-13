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

import org.activiti.cloud.organization.core.model.ModelReference;
import org.activiti.cloud.organization.repository.entity.ModelType;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.activiti.cloud.organization.repository.entity.ModelType.FORM;
import static org.activiti.cloud.organization.repository.entity.ModelType.PROCESS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Tests for {@link FeignModelService}
 */
public class FeignModelServiceTest {

    @Mock
    private ProcessModelService processModelService;

    @Mock
    private FormModelService formModelService;

    private FeignModelService feignModelService = new FeignModelService();

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
                                      "process_model_refId");

        // THEN
        verify(processModelService,
               times(1))
                .getResource("process_model_refId");

        // WHEN
        feignModelService.getResource(FORM,
                                      "form_model_refId");

        // THEN
        verify(formModelService,
               times(1))
                .getResource("form_model_refId");
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
                                         "process_model_refId",
                                         mock(ModelReference.class));

        // THEN
        verify(processModelService,
               times(1))
                .updateResource(eq("process_model_refId"),
                                any(ModelReference.class));

        // WHEN
        feignModelService.updateResource(FORM,
                                         "form_model_refId",
                                         mock(ModelReference.class));

        // THEN
        verify(formModelService,
               times(1))
                .updateResource(eq("form_model_refId"),
                                any(ModelReference.class));
    }

    @Test
    public void testDeleteResource() {
        // WHEN
        feignModelService.deleteResource(PROCESS,
                                         "process_model_refId");

        // THEN
        verify(processModelService,
               times(1))
                .deleteResource(eq("process_model_refId"));

        // WHEN
        feignModelService.deleteResource(FORM,
                                         "form_model_refId");

        // THEN
        verify(formModelService,
               times(1))
                .deleteResource(eq("form_model_refId"));
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

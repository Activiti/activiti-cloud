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

import org.activiti.cloud.organization.core.model.Group;
import org.activiti.cloud.organization.core.model.Model;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.hateoas.Resource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link RestResourceProcessor}
 */
@RunWith(MockitoJUnitRunner.class)
public class RestResourceProcessorTest {

    @Mock
    private RestResourceService restResourceService;

    @InjectMocks
    @Spy
    private RestResourceProcessor restResourceProcessor;

    @Test
    public void testProcessWithRestResource() {

        // GIVEN
        Resource<Object> modelResource = new Resource<Object>(new Model());
        doReturn(true).when(restResourceProcessor).isEntityWithRestResource(eq(Model.class));

        // WHEN
        restResourceProcessor.resourceProcessor().process(modelResource);

        // THEN
        verify(restResourceService,
               times(1))
                .loadRestResourceIntoEntityField(any(Model.class),
                                                 eq("data"),
                                                 eq("type"),
                                                 eq("refId"));
    }

    @Test
    public void testProcessWithoutRestResource() {

        // GIVEN
        Resource<Object> modelResource = new Resource<Object>(new Group());
        doReturn(false).when(restResourceProcessor).isEntityWithRestResource(eq(Group.class));

        // WHEN
        restResourceProcessor.resourceProcessor().process(modelResource);

        // THEN
        verify(restResourceService,
               never())
                .loadRestResourceIntoEntityField(any(),
                                                 anyString(),
                                                 anyString(),
                                                 anyString());
    }
}

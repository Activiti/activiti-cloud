/*
 * Copyright 2017-2020 Alfresco.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.services.rest.assemblers;

import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.impl.TaskImpl;
import org.activiti.cloud.api.task.model.CloudTask;
import org.activiti.cloud.api.task.model.impl.CloudTaskImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;

import static org.activiti.api.task.model.Task.TaskStatus.ASSIGNED;
import static org.activiti.api.task.model.Task.TaskStatus.CREATED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.MockitoAnnotations.initMocks;

public class TaskResourceAssemblerTest {

    @InjectMocks
    private TaskResourceAssembler resourceAssembler;

    @Mock
    private ToCloudTaskConverter converter;

    @BeforeEach
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void toResourceShouldReturnResourceWithSelfLinkContainingResourceId() {
        Task model = new TaskImpl("my-identifier", "myTask", CREATED);

        given(converter.from(model)).willReturn(new CloudTaskImpl(model));

        Resource<CloudTask> resource = resourceAssembler.toResource(model);

        Link selfResourceLink = resource.getLink("self");

        assertThat(selfResourceLink).isNotNull();
        assertThat(selfResourceLink.getHref()).contains("my-identifier");
    }

    @Test
    public void toResourceShouldReturnResourceWithReleaseAndCompleteLinksWhenStatusIsAssigned() {
        Task model = new TaskImpl("my-identifier", "myTask", CREATED);

        given(converter.from(model)).willReturn(new CloudTaskImpl(model));

        Resource<CloudTask> resource = resourceAssembler.toResource(model);

        assertThat(resource.getLink("claim")).isNotNull();
        assertThat(resource.getLink("release")).isNull();
        assertThat(resource.getLink("complete")).isNull();
    }

    @Test
    public void toResourceShouldReturnResourceWithClaimLinkWhenStatusIsNotAssigned() {
        Task model = new TaskImpl("my-identifier", "myTask", ASSIGNED);

        given(converter.from(model)).willReturn(new CloudTaskImpl(model));

        Resource<CloudTask> resource = resourceAssembler.toResource(model);

        assertThat(resource.getLink("claim")).isNull();
        assertThat(resource.getLink("release")).isNotNull();
        assertThat(resource.getLink("complete")).isNotNull();
    }

    @Test
    public void toResourceShouldNotReturnResourceWithProcessInstanceLinkWhenNewTaskIsCreated() {
        Task model = new TaskImpl("my-identifier", "myTask", CREATED);

        given(converter.from(model)).willReturn(new CloudTaskImpl(model));
        Resource<CloudTask> resource = resourceAssembler.toResource(model);

        // a new standalone task doesn't have a bond to a process instance
        // and should not return the rel 'processInstance'
        assertThat(resource.getLink("processInstance")).isNull();
    }

    @Test
    public void toResourceShouldReturnResourceWithProcessInstanceLinkForProcessInstanceTask() {
        // process instance task
        Task model = new TaskImpl("my-identifier", "myTask", CREATED);
        ((TaskImpl) model).setProcessInstanceId("processInstanceId");

        given(converter.from(model)).willReturn(new CloudTaskImpl(model));

        Resource<CloudTask> resource = resourceAssembler.toResource(model);

        assertThat(resource.getLink("processInstance")).isNotNull();
    }

}

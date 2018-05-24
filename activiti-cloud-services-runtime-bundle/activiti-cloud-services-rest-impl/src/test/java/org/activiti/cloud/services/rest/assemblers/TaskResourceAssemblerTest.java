package org.activiti.cloud.services.rest.assemblers;

import org.activiti.cloud.services.rest.api.resources.TaskResource;
import org.activiti.runtime.api.model.Task;
import org.junit.Test;
import org.springframework.hateoas.Link;

import static org.activiti.runtime.api.model.Task.TaskStatus.ASSIGNED;
import static org.activiti.runtime.api.model.Task.TaskStatus.CREATED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TaskResourceAssemblerTest {

    private TaskResourceAssembler resourceAssembler = new TaskResourceAssembler();

    @Test
    public void toResourceShouldReturnResourceWithSelfLinkContainingResourceId() {
        Task model = mock(Task.class);
        when(model.getId()).thenReturn("my-identifier");

        TaskResource resource = resourceAssembler.toResource(model);

        Link selfResourceLink = resource.getLink("self");

        assertThat(selfResourceLink).isNotNull();
        assertThat(selfResourceLink.getHref()).contains("my-identifier");
    }

    @Test
    public void toResourceShouldReturnResourceWithReleaseAndCompleteLinksWhenStatusIsAssigned() {
        Task model = mock(Task.class);
        when(model.getId()).thenReturn("my-identifier");

        TaskResource resource = resourceAssembler.toResource(model);

        assertThat(resource.getLink("claim")).isNotNull();
        assertThat(resource.getLink("release")).isNull();
        assertThat(resource.getLink("complete")).isNull();
    }

    @Test
    public void toResourceShouldReturnResourceWithClaimLinkWhenStatusIsNotAssigned() {
        Task model = mock(Task.class);
        when(model.getId()).thenReturn("my-identifier");
        when(model.getStatus()).thenReturn(ASSIGNED);

        TaskResource resource = resourceAssembler.toResource(model);

        assertThat(resource.getLink("claim")).isNull();
        assertThat(resource.getLink("release")).isNotNull();
        assertThat(resource.getLink("complete")).isNotNull();
    }

    @Test
    public void toResourceShouldNotReturnResourceWithProcessInstanceLinkWhenNewTaskIsCreated() {
        Task model = mock(Task.class);
        when(model.getStatus()).thenReturn(CREATED);
        TaskResource resource = resourceAssembler.toResource(model);

        // a new standalone task doesn't have a bond to a process instance
        // and should not return the rel 'processInstance'
        assertThat(resource.getLink("processInstance")).isNull();
    }

    @Test
    public void toResourceShouldReturnResourceWithProcessInstanceLinkForProcessInstanceTask() {
        // process instance task
        Task model = mock(Task.class);
        when(model.getProcessInstanceId()).thenReturn("processInstanceId");

        TaskResource resource = resourceAssembler.toResource(model);

        assertThat(resource.getLink("processInstance")).isNotNull();
    }

}
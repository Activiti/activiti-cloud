package org.activiti.cloud.services.rest.assemblers;

import org.activiti.cloud.services.api.model.Task;
import org.activiti.cloud.services.rest.api.resources.TaskResource;
import org.junit.Test;
import org.springframework.hateoas.Link;

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
        when(model.getStatus()).thenReturn(Task.TaskStatus.ASSIGNED.name());

        TaskResource resource = resourceAssembler.toResource(model);

        assertThat(resource.getLink("claim")).isNull();
        assertThat(resource.getLink("release")).isNotNull();
        assertThat(resource.getLink("complete")).isNotNull();
    }

}
package org.activiti.cloud.services.rest.assemblers;

import org.activiti.cloud.services.api.model.TaskVariables;
import org.activiti.cloud.services.rest.api.resources.VariablesResource;
import org.junit.Test;
import org.springframework.hateoas.Link;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TaskVariablesResourceAssemblerTest {

    private TaskVariablesResourceAssembler resourceAssembler = new TaskVariablesResourceAssembler();

    @Test
    public void toResourceShouldReturnResourceWithSelfLinkContainingResourceId() {
        TaskVariables model = mock(TaskVariables.class);
        when(model.getTaskId()).thenReturn("my-identifier");

        VariablesResource resource = resourceAssembler.toResource(model);

        Link selfResourceLink = resource.getLink("self");

        assertThat(selfResourceLink).isNotNull();
        assertThat(selfResourceLink.getHref()).contains("my-identifier");
    }
}
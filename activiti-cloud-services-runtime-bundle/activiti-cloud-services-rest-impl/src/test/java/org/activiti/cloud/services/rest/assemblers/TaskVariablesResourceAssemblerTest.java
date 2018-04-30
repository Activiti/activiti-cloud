package org.activiti.cloud.services.rest.assemblers;

import org.activiti.cloud.services.api.model.TaskVariable;
import org.activiti.cloud.services.rest.api.resources.TaskVariableResource;
import org.junit.Test;
import org.springframework.hateoas.Link;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TaskVariablesResourceAssemblerTest {

    private TaskVariableResourceAssembler resourceAssembler = new TaskVariableResourceAssembler();

    @Test
    public void toResourceShouldReturnResourceWithSelfLinkContainingResourceId() {
        TaskVariable model = mock(TaskVariable.class);
        when(model.getTaskId()).thenReturn("my-identifier");

        TaskVariableResource resource = resourceAssembler.toResource(model);

        Link globalVariablesLink = resource.getLink("globalVariables");

        assertThat(globalVariablesLink).isNotNull();
        assertThat(globalVariablesLink.getHref()).contains("my-identifier");
    }
}
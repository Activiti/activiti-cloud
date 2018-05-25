package org.activiti.cloud.services.rest.assemblers;

import org.activiti.cloud.services.rest.api.resources.VariableInstanceResource;
import org.activiti.runtime.api.model.VariableInstance;
import org.junit.Test;
import org.springframework.hateoas.Link;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TaskVariablesResourceAssemblerTest {

    private TaskVariableInstanceResourceAssembler resourceAssembler = new TaskVariableInstanceResourceAssembler();

    @Test
    public void toResourceShouldReturnResourceWithSelfLinkContainingResourceId() {
        VariableInstance model = mock(VariableInstance.class);
        when(model.getTaskId()).thenReturn("my-identifier");

        VariableInstanceResource resource = resourceAssembler.toResource(model);

        Link globalVariablesLink = resource.getLink("globalVariables");

        assertThat(globalVariablesLink).isNotNull();
        assertThat(globalVariablesLink.getHref()).contains("my-identifier");
    }
}
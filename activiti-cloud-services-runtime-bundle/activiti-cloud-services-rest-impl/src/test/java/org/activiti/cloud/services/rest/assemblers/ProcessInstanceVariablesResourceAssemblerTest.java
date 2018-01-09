package org.activiti.cloud.services.rest.assemblers;

import org.activiti.cloud.services.api.model.ProcessInstanceVariables;
import org.activiti.cloud.services.rest.api.resources.VariablesResource;
import org.junit.Test;
import org.springframework.hateoas.Link;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ProcessInstanceVariablesResourceAssemblerTest {

    private ProcessInstanceVariablesResourceAssembler resourceAssembler = new ProcessInstanceVariablesResourceAssembler();

    @Test
    public void toResourceShouldReturnResourceWithSelfLinkContainingResourceId() {
        ProcessInstanceVariables model = mock(ProcessInstanceVariables.class);
        when(model.getProcessInstanceId()).thenReturn("my-identifier");

        VariablesResource resource = resourceAssembler.toResource(model);

        Link selfResourceLink = resource.getLink("self");

        assertThat(selfResourceLink).isNotNull();
        assertThat(selfResourceLink.getHref()).contains("my-identifier");
    }
}
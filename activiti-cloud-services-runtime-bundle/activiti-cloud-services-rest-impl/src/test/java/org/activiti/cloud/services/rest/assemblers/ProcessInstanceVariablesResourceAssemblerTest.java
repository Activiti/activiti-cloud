package org.activiti.cloud.services.rest.assemblers;

import org.activiti.cloud.services.api.model.ProcessInstanceVariable;
import org.activiti.cloud.services.rest.api.resources.ProcessVariableResource;
import org.junit.Test;
import org.springframework.hateoas.Link;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ProcessInstanceVariablesResourceAssemblerTest {

    private ProcessInstanceVariableResourceAssembler resourceAssembler = new ProcessInstanceVariableResourceAssembler();

    @Test
    public void toResourceShouldReturnResourceWithSelfLinkContainingResourceId() {
        ProcessInstanceVariable model = mock(ProcessInstanceVariable.class);
        when(model.getProcessInstanceId()).thenReturn("my-identifier");

        ProcessVariableResource resource = resourceAssembler.toResource(model);

        Link selfResourceLink = resource.getLink("self");

        assertThat(selfResourceLink).isNotNull();
        assertThat(selfResourceLink.getHref()).contains("my-identifier");
    }
}
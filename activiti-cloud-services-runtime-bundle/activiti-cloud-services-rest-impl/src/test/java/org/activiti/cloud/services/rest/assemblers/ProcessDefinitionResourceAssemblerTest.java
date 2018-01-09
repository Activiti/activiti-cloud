package org.activiti.cloud.services.rest.assemblers;

import org.activiti.cloud.services.api.model.ProcessDefinition;
import org.activiti.cloud.services.rest.api.resources.ProcessDefinitionResource;
import org.junit.Test;
import org.springframework.hateoas.Link;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ProcessDefinitionResourceAssemblerTest {

    private ProcessDefinitionResourceAssembler resourceAssembler = new ProcessDefinitionResourceAssembler();

    @Test
    public void toResourceShouldReturnResourceWithSelfLinkContainingResourceId() {
        ProcessDefinition processDefinition = mock(ProcessDefinition.class);
        when(processDefinition.getId()).thenReturn("my-identifier");

        ProcessDefinitionResource processDefinitionResource = resourceAssembler.toResource(processDefinition);

        Link selfResourceLink = processDefinitionResource.getLink("self");

        assertThat(selfResourceLink).isNotNull();
        assertThat(selfResourceLink.getHref()).contains("my-identifier");
    }
}
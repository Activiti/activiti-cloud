package org.activiti.cloud.services.rest.assemblers;

import org.activiti.cloud.services.api.model.ProcessInstance;
import org.activiti.cloud.services.rest.api.resources.ProcessInstanceResource;
import org.junit.Test;
import org.springframework.hateoas.Link;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ProcessInstanceResourceAssemblerTest {

    private ProcessInstanceResourceAssembler resourceAssembler = new ProcessInstanceResourceAssembler();

    @Test
    public void toResourceShouldReturnResourceWithSelfLinkContainingResourceId() {
        ProcessInstance model = mock(ProcessInstance.class);
        when(model.getId()).thenReturn("my-identifier");

        ProcessInstanceResource resource = resourceAssembler.toResource(model);

        Link selfResourceLink = resource.getLink("self");

        assertThat(selfResourceLink).isNotNull();
        assertThat(selfResourceLink.getHref()).contains("my-identifier");
    }
}
package org.activiti.cloud.services.rest.assemblers;

import org.activiti.cloud.services.rest.api.resources.ProcessInstanceResource;
import org.activiti.runtime.api.model.ProcessInstance;
import org.junit.Test;
import org.springframework.hateoas.Link;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
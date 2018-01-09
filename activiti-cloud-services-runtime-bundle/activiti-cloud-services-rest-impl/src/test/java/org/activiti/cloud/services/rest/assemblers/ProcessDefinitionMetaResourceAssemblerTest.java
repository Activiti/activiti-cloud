package org.activiti.cloud.services.rest.assemblers;

import org.activiti.cloud.services.api.model.ProcessDefinitionMeta;
import org.activiti.cloud.services.rest.api.resources.ProcessDefinitionMetaResource;
import org.junit.Test;
import org.springframework.hateoas.Link;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ProcessDefinitionMetaResourceAssemblerTest {

    private ProcessDefinitionMetaResourceAssembler resourceAssembler = new ProcessDefinitionMetaResourceAssembler();

    @Test
    public void toResourceShouldReturnResourceWithSelfLinkContainingResourceId() {
        ProcessDefinitionMeta model = mock(ProcessDefinitionMeta.class);
        when(model.getId()).thenReturn("my-identifier");

        ProcessDefinitionMetaResource resource = resourceAssembler.toResource(model);

        Link selfResourceLink = resource.getLink("self");

        assertThat(selfResourceLink).isNotNull();
        assertThat(selfResourceLink.getHref()).contains("my-identifier");

        Link metaResourceLink = resource.getLink("meta");

        assertThat(metaResourceLink).isNotNull();
        assertThat(metaResourceLink.getHref()).contains("my-identifier");
    }
}
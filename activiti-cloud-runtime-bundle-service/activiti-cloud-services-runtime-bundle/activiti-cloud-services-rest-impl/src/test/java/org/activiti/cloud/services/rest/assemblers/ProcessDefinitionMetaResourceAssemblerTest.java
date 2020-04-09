package org.activiti.cloud.services.rest.assemblers;

import org.activiti.cloud.services.api.model.ProcessDefinitionMeta;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProcessDefinitionMetaResourceAssemblerTest {

    private ProcessDefinitionMetaResourceAssembler resourceAssembler = new ProcessDefinitionMetaResourceAssembler();

    @Test
    public void toResourceShouldReturnResourceWithSelfLinkContainingResourceId() {
        ProcessDefinitionMeta model = mock(ProcessDefinitionMeta.class);
        when(model.getId()).thenReturn("my-identifier");

        Resource<ProcessDefinitionMeta> resource = resourceAssembler.toResource(model);

        Link selfResourceLink = resource.getLink("self");

        assertThat(selfResourceLink).isNotNull();
        assertThat(selfResourceLink.getHref()).contains("my-identifier");

        Link metaResourceLink = resource.getLink("meta");

        assertThat(metaResourceLink).isNotNull();
        assertThat(metaResourceLink.getHref()).contains("my-identifier");
    }
}

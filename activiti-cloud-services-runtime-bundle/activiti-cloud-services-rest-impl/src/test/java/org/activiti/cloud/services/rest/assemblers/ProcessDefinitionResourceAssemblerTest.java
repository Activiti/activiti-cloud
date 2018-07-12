package org.activiti.cloud.services.rest.assemblers;

import org.activiti.cloud.services.rest.api.resources.ProcessDefinitionResource;
import org.activiti.runtime.api.model.impl.CloudProcessDefinitionImpl;
import org.activiti.runtime.api.model.impl.ProcessDefinitionImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.hateoas.Link;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.MockitoAnnotations.initMocks;

public class ProcessDefinitionResourceAssemblerTest {

    @InjectMocks
    private ProcessDefinitionResourceAssembler resourceAssembler;

    @Mock
    private ToCloudProcessDefinitionConverter converter;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void toResourceShouldReturnResourceWithSelfLinkContainingResourceId() {
        ProcessDefinitionImpl processDefinition = new ProcessDefinitionImpl();
        processDefinition.setId("my-identifier");
        given(converter.from(processDefinition)).willReturn(new CloudProcessDefinitionImpl(processDefinition));

        ProcessDefinitionResource processDefinitionResource = resourceAssembler.toResource(processDefinition);

        Link selfResourceLink = processDefinitionResource.getLink("self");

        assertThat(selfResourceLink).isNotNull();
        assertThat(selfResourceLink.getHref()).contains("my-identifier");
    }
}
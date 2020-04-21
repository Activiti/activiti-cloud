package org.activiti.cloud.services.rest.assemblers;

import java.util.Optional;
import org.activiti.api.runtime.model.impl.ProcessDefinitionImpl;
import org.activiti.cloud.api.process.model.CloudProcessDefinition;
import org.activiti.cloud.api.process.model.impl.CloudProcessDefinitionImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.EntityModel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.hateoas.IanaLinkRelations.SELF;

public class ProcessDefinitionRepresentationModelAssemblerTest {

    @InjectMocks
    private ProcessDefinitionRepresentationModelAssembler representationModelAssembler;

    @Mock
    private ToCloudProcessDefinitionConverter converter;

    @BeforeEach
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void toResourceShouldReturnResourceWithSelfLinkContainingResourceId() {
        ProcessDefinitionImpl processDefinition = new ProcessDefinitionImpl();
        processDefinition.setId("my-identifier");
        given(converter.from(processDefinition)).willReturn(new CloudProcessDefinitionImpl(processDefinition));

        EntityModel<CloudProcessDefinition> processDefinitionResource = representationModelAssembler.toModel(processDefinition);

        Optional<Link> selfResourceLink = processDefinitionResource.getLink(SELF);

        assertThat(selfResourceLink).isPresent();
        assertThat(selfResourceLink.get().getHref()).contains("my-identifier");
    }
}

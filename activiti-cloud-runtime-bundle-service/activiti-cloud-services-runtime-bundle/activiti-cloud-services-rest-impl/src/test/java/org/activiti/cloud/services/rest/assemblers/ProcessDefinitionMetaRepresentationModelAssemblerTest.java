package org.activiti.cloud.services.rest.assemblers;

import java.util.Optional;
import org.activiti.cloud.services.api.model.ProcessDefinitionMeta;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.EntityModel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.hateoas.IanaLinkRelations.SELF;

public class ProcessDefinitionMetaRepresentationModelAssemblerTest {

    private ProcessDefinitionMetaRepresentationModelAssembler representationModelAssembler = new ProcessDefinitionMetaRepresentationModelAssembler();

    @Test
    public void toResourceShouldReturnResourceWithSelfLinkContainingResourceId() {
        ProcessDefinitionMeta model = mock(ProcessDefinitionMeta.class);
        when(model.getId()).thenReturn("my-identifier");

        EntityModel<ProcessDefinitionMeta> resource = representationModelAssembler.toModel(model);

        Optional<Link> selfResourceLink = resource.getLink(SELF);

        assertThat(selfResourceLink).isPresent();
        assertThat(selfResourceLink.get().getHref()).contains("my-identifier");

        Optional<Link> metaResourceLink = resource.getLink("meta");

        assertThat(metaResourceLink).isPresent();
        assertThat(metaResourceLink.get().getHref()).contains("my-identifier");
    }
}

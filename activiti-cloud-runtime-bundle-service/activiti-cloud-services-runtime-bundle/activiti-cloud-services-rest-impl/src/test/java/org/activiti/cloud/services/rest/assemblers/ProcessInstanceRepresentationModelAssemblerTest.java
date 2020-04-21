package org.activiti.cloud.services.rest.assemblers;

import java.util.Optional;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.EntityModel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.hateoas.IanaLinkRelations.SELF;

public class ProcessInstanceRepresentationModelAssemblerTest {

    @InjectMocks
    private ProcessInstanceRepresentationModelAssembler representationModelAssembler;

    @Mock
    private ToCloudProcessInstanceConverter toCloudProcessInstanceConverter;

    @BeforeEach
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void toResourceShouldReturnResourceWithSelfLinkContainingResourceId() {
        //given
        CloudProcessInstance cloudModel = mock(CloudProcessInstance.class);
        given(cloudModel.getId()).willReturn("my-identifier");

        ProcessInstance model = mock(ProcessInstance.class);
        given(toCloudProcessInstanceConverter.from(model)).willReturn(cloudModel);

        //when
        EntityModel<CloudProcessInstance> resource = representationModelAssembler.toModel(model);

        //then
        Optional<Link> selfResourceLink = resource.getLink(SELF);
        assertThat(selfResourceLink).isPresent();
        assertThat(selfResourceLink.get().getHref()).contains("my-identifier");
    }
}

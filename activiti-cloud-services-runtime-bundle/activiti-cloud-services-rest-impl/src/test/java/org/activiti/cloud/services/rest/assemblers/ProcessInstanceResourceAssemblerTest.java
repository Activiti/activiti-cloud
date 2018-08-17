package org.activiti.cloud.services.rest.assemblers;

import org.activiti.api.process.model.ProcessInstance;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.services.rest.api.resources.ProcessInstanceResource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.hateoas.Link;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.initMocks;

public class ProcessInstanceResourceAssemblerTest {

    @InjectMocks
    private ProcessInstanceResourceAssembler resourceAssembler;

    @Mock
    private ToCloudProcessInstanceConverter toCloudProcessInstanceConverter;

    @Before
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
        ProcessInstanceResource resource = resourceAssembler.toResource(model);

        //then
        Link selfResourceLink = resource.getLink("self");
        assertThat(selfResourceLink).isNotNull();
        assertThat(selfResourceLink.getHref()).contains("my-identifier");
    }
}
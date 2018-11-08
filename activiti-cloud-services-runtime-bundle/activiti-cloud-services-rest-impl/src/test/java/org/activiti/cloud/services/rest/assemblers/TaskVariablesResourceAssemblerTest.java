package org.activiti.cloud.services.rest.assemblers;

import org.activiti.api.model.shared.model.VariableInstance;
import org.activiti.api.runtime.model.impl.VariableInstanceImpl;
import org.activiti.cloud.api.model.shared.impl.CloudVariableInstanceImpl;
import org.activiti.cloud.services.rest.api.resources.VariableInstanceResource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.hateoas.Link;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.MockitoAnnotations.initMocks;

public class TaskVariablesResourceAssemblerTest {

    @InjectMocks
    private TaskVariableInstanceResourceAssembler resourceAssembler;

    @Mock
    private ToCloudVariableInstanceConverter converter;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void toResourceShouldReturnResourceWithSelfLinkContainingResourceId() {
        //given
        VariableInstance model = new VariableInstanceImpl<>("var", "string", "value", "procInstId");
        ((VariableInstanceImpl) model).setTaskId("my-identifier");

        given(converter.from(model)).willReturn(new CloudVariableInstanceImpl<>(model));

        //when
        VariableInstanceResource resource = resourceAssembler.toResource(model);

        //then
        Link globalVariablesLink = resource.getLink("variables");

        assertThat(globalVariablesLink).isNotNull();
        assertThat(globalVariablesLink.getHref()).contains("my-identifier");
    }
}
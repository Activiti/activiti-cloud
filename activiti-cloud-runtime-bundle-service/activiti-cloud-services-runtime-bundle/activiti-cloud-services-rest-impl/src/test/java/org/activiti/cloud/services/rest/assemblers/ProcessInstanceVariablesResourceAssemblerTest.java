package org.activiti.cloud.services.rest.assemblers;

import org.activiti.api.model.shared.model.VariableInstance;
import org.activiti.api.runtime.model.impl.VariableInstanceImpl;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.activiti.cloud.api.model.shared.impl.CloudVariableInstanceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.MockitoAnnotations.initMocks;

public class ProcessInstanceVariablesResourceAssemblerTest {

    @InjectMocks
    private ProcessInstanceVariableResourceAssembler resourceAssembler;

    @Mock
    private ToCloudVariableInstanceConverter converter;

    @BeforeEach
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void toResourceShouldReturnResourceWithSelfLinkContainingResourceId() {
        //given
        VariableInstance model = new VariableInstanceImpl<>("var", "string", "value", "my-identifier");
        given(converter.from(model)).willReturn(new CloudVariableInstanceImpl<>(model));

        //when
        Resource<CloudVariableInstance> resource = resourceAssembler.toResource(model);

        //then
        Link processVariablesLink = resource.getLink("processVariables");

        assertThat(processVariablesLink).isNotNull();
        assertThat(processVariablesLink.getHref()).contains("my-identifier");
    }
}

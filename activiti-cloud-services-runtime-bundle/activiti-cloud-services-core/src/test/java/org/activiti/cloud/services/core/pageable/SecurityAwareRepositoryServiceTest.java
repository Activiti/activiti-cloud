package org.activiti.cloud.services.core.pageable;

import org.activiti.cloud.services.core.SecurityPoliciesApplicationService;
import org.activiti.cloud.services.security.SecurityPolicy;
import org.activiti.runtime.api.ProcessRuntime;
import org.activiti.runtime.api.model.ProcessDefinition;
import org.activiti.runtime.api.model.payloads.GetProcessDefinitionsPayload;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class SecurityAwareRepositoryServiceTest {

    @InjectMocks
    private SecurityAwareRepositoryService securityAwareRepositoryService;

    @Mock
    private SecurityPoliciesApplicationService securityService;

    @Mock
    private ProcessRuntime processRuntime;

    @Mock
    private SpringPageConverter pageConverter;

    @Mock
    private org.activiti.runtime.api.query.Page<ProcessDefinition> apiPage;

    @Mock
    private Page<ProcessDefinition> springPage;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void getAuthorizedProcessDefinitionsShouldApplySecurity() {
        //given
        Pageable springPageable = mock(Pageable.class);
        GetProcessDefinitionsPayload filter = mock(GetProcessDefinitionsPayload.class);
        given(securityService.restrictProcessDefQuery(SecurityPolicy.READ)).willReturn(filter);

        org.activiti.runtime.api.query.Pageable apiPageable = mock(org.activiti.runtime.api.query.Pageable.class);
        given(pageConverter.toAPIPageable(springPageable)).willReturn(apiPageable);

        given(processRuntime.processDefinitions(apiPageable,
                                                filter)).willReturn(apiPage);
        given(pageConverter.toSpringPage(springPageable,
                                         apiPage)).willReturn(springPage);

        //when
        Page<ProcessDefinition> authorizedProcessDefinitions = securityAwareRepositoryService.getAuthorizedProcessDefinitions(springPageable);

        //then
        assertThat(authorizedProcessDefinitions).isEqualTo(springPage);
    }
}

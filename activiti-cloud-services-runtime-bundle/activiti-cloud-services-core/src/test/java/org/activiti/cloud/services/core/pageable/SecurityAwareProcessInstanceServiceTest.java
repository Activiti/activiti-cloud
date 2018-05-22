package org.activiti.cloud.services.core.pageable;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.activiti.cloud.services.api.commands.ActivateProcessInstanceCmd;
import org.activiti.cloud.services.api.commands.SetProcessVariablesCmd;
import org.activiti.cloud.services.api.commands.SignalProcessInstancesCmd;
import org.activiti.cloud.services.api.commands.StartProcessInstanceCmd;
import org.activiti.cloud.services.api.commands.SuspendProcessInstanceCmd;
import org.activiti.cloud.services.core.ActivitiForbiddenException;
import org.activiti.cloud.services.core.AuthenticationWrapper;
import org.activiti.cloud.services.core.SecurityPoliciesApplicationService;
import org.activiti.cloud.services.security.SecurityPolicy;
import org.activiti.engine.RuntimeService;
import org.activiti.runtime.api.NotFoundException;
import org.activiti.runtime.api.ProcessRuntime;
import org.activiti.runtime.api.model.FluentProcessDefinition;
import org.activiti.runtime.api.model.FluentProcessInstance;
import org.activiti.runtime.api.model.ProcessInstance;
import org.activiti.runtime.api.model.builder.ProcessStarter;
import org.activiti.runtime.api.model.builder.SignalPayload;
import org.activiti.runtime.api.model.impl.FluentProcessInstanceImpl;
import org.activiti.runtime.api.query.ProcessInstanceFilter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import static org.activiti.cloud.services.core.utils.MockUtils.selfReturningMock;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class SecurityAwareProcessInstanceServiceTest {

    @InjectMocks
    private SecurityAwareProcessInstanceService securityAwareProcessInstanceService;

    @Mock
    private ProcessRuntime processRuntime;

    @Mock
    private SecurityPoliciesApplicationService securityService;

    @Mock
    private PageableConverter pageableConverter;

    @Mock
    private SpringPageConverter springPageConverter;

    @Mock
    private org.activiti.runtime.api.query.Page<FluentProcessInstance> apiPage;

    @Mock
    private Page<ProcessInstance> springPage;

    @Mock
    private AuthenticationWrapper authenticationWrapper;

    @Mock
    private RuntimeService runtimeService;

    public SecurityAwareProcessInstanceServiceTest() {
    }

    @Before
    public void setUp() {
        initMocks(this);
        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("bob");
    }

    @Test
    public void getAuthorizedProcessInstancesShouldApplySecurity() {
        //given
        ProcessInstanceFilter filter = mock(ProcessInstanceFilter.class);
        given(securityService.restrictProcessInstQuery(SecurityPolicy.READ)).willReturn(filter);

        Pageable springPageable = mock(Pageable.class);
        org.activiti.runtime.api.query.Pageable apiPageable = mock(org.activiti.runtime.api.query.Pageable.class);
        given(pageableConverter.toAPIPageable(springPageable)).willReturn(apiPageable);

        given(processRuntime.processInstances(apiPageable,
                                              filter)).willReturn(apiPage);
        given(springPageConverter.<ProcessInstance, FluentProcessInstance>toSpringPage(springPageable,
                                                                                       apiPage)).willReturn(springPage);

        //when
        Page<ProcessInstance> authorizedProcessInstances = securityAwareProcessInstanceService.getAuthorizedProcessInstances(springPageable);

        //then
        assertThat(authorizedProcessInstances).isEqualTo(springPage);
    }

    @Test
    public void shouldStartProcessByKeyWithPermission() {
        //given
        String processDefinitionKey = "my-proc";
        FluentProcessDefinition processDefinition = buildProcessDefinition(processDefinitionKey);

        given(processRuntime.processDefinitionByKey(processDefinitionKey)).willReturn(processDefinition);
        given(securityService.canWrite(processDefinitionKey)).willReturn(true);

        ProcessStarter starter = mock(ProcessStarter.class,
                                      Answers.RETURNS_DEEP_STUBS);
        given(processDefinition.startProcessWith()).willReturn(starter);
        given(starter.variables(any())
                      .businessKey(any()))
                .willReturn(starter);
        FluentProcessInstance processInstance = mock(FluentProcessInstance.class);
        given(starter.doIt()).willReturn(processInstance);

        StartProcessInstanceCmd startProcessInstanceCmd = new StartProcessInstanceCmd(processDefinitionKey,
                                                                                      null,
                                                                                      null,
                                                                                      null);

        //when
        ProcessInstance startedInstance = securityAwareProcessInstanceService.startProcess(startProcessInstanceCmd);

        //then
        assertThat(startedInstance).isEqualTo(processInstance);
    }

    private FluentProcessDefinition buildProcessDefinition(String processDefinitionKey) {
        FluentProcessDefinition processDefinition = mock(FluentProcessDefinition.class);
        given(processDefinition.getKey()).willReturn(processDefinitionKey);
        return processDefinition;
    }

    @Test
    public void shouldNotStartWithoutPermission() {
        //given
        String processDefinitionKey = "my-proc";
        FluentProcessDefinition processDefinition = buildProcessDefinition(processDefinitionKey);

        given(processRuntime.processDefinitionByKey(processDefinitionKey)).willReturn(processDefinition);
        given(securityService.canWrite(processDefinitionKey)).willReturn(false);

        //then
        assertThatExceptionOfType(ActivitiForbiddenException.class).isThrownBy(
                //when
                () -> securityAwareProcessInstanceService.startProcess(new StartProcessInstanceCmd(processDefinitionKey,
                                                                                                   null,
                                                                                                   null,
                                                                                                   null))
        );
    }

    @Test
    public void shouldSignal() {
        //given
        SignalPayload signalPayload = selfReturningMock(SignalPayload.class);
        doReturn(null).when(signalPayload).doIt();
        given(processRuntime.sendSignalWith()).willReturn(signalPayload);
        String name = "go";
        Map<String, Object> inputVariables = Collections.singletonMap("var",
                                                                      "value");
        //when
        securityAwareProcessInstanceService.signal(new SignalProcessInstancesCmd(name,
                                                                                 inputVariables));
        verify(signalPayload).name(name);
        verify(signalPayload).variables(inputVariables);
    }

    @Test
    public void shouldNotSuspendWithoutPermission() {
        //given
        ProcessInstance processInstance = aProcessInstanceWithWritePermission(false);

        //then
        assertThatExceptionOfType(ActivitiForbiddenException.class).isThrownBy(
                //when
                () -> securityAwareProcessInstanceService.suspend(new SuspendProcessInstanceCmd(processInstance.getId()))
        );
    }

    private FluentProcessInstance aProcessInstanceWithWritePermission(boolean hasWritePermission) {
        FluentProcessInstance processInstance = buildProcessInstance(UUID.randomUUID().toString(),
                                                                     UUID.randomUUID().toString(),
                                                                     "my-proc");
        given(processRuntime.processInstance(processInstance.getId())).willReturn(processInstance);
        given(securityService.canRead(processInstance.getProcessDefinitionKey())).willReturn(true);
        FluentProcessDefinition def = buildProcessDefinition("my-proc");
        when(processRuntime.processDefinitionById(processInstance.getProcessDefinitionId())).thenReturn(def);

        when(securityService.canWrite(def.getKey())).thenReturn(hasWritePermission);
        return processInstance;
    }

    private FluentProcessInstance buildProcessInstance(String processInstanceId,
                                                       String processDefinitionId,
                                                       String processDefinitionKey) {
        FluentProcessInstanceImpl processInstance = buildProcessInstance(processInstanceId,
                                                                         processDefinitionId);
        processInstance.setProcessDefinitionKey(processDefinitionKey);
        return processInstance;
    }

    private FluentProcessInstanceImpl buildProcessInstance(String processInstanceId,
                                                           String processDefinitionId) {
        FluentProcessInstanceImpl processInstance = new FluentProcessInstanceImpl(runtimeService,
                                                                                  null);
        processInstance.setId(processInstanceId);
        processInstance.setProcessDefinitionId(processDefinitionId);
        return processInstance;
    }

    @Test
    public void shouldNotActivateWithoutPermission() {
        //given
        ProcessInstance processInstance = aProcessInstanceWithWritePermission(false);

        //then
        assertThatExceptionOfType(ActivitiForbiddenException.class).isThrownBy(
                //when
                () -> securityAwareProcessInstanceService.activate(new ActivateProcessInstanceCmd(processInstance.getId()))
        );
    }

    @Test
    public void shouldSetProcessVariables() {
        //given
        FluentProcessInstance processInstance = aProcessInstanceWithWritePermission(true);

        Map<String, Object> variables = Collections.singletonMap("var",
                                                                 "value");
        //when
        securityAwareProcessInstanceService.setProcessVariables(new SetProcessVariablesCmd(processInstance.getId(),
                                                                                           variables));

        //then
        verify(runtimeService).setVariables(processInstance.getId(),
                                            variables);
    }

    @Test
    public void shouldNotSetProcessVariablesWithoutPermission() {
        //given
        ProcessInstance processInstance = aProcessInstanceWithWritePermission(false);

        //then
        assertThatExceptionOfType(ActivitiForbiddenException.class).isThrownBy(
                //when
                () -> securityAwareProcessInstanceService.setProcessVariables(new SetProcessVariablesCmd(processInstance.getId(),
                                                                                                         Collections.emptyMap()))
        );
    }

    @Test
    public void deleteNotExistingProcessInstanceShouldThrowException() {
        //given
        String processInstanceId = UUID.randomUUID().toString();
        given(processRuntime.processInstance(processInstanceId)).willThrow(new NotFoundException("Not found"));

        //then
        assertThatExceptionOfType(NotFoundException.class).isThrownBy(
                //when
                () -> securityAwareProcessInstanceService.deleteProcessInstance(processInstanceId)
        ).withMessageStartingWith("Not found");
    }

    @Test
    public void shouldNotDeleteProcessInstanceWithoutPermission() {
        //given
        ProcessInstance processInstance = aProcessInstanceWithWritePermission(false);

        //then
        assertThatExceptionOfType(ActivitiForbiddenException.class).isThrownBy(
                //when
                () -> securityAwareProcessInstanceService.deleteProcessInstance(processInstance.getId())
        ).withMessageStartingWith("Operation not permitted");
    }
}

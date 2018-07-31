package org.activiti.cloud.services.core.pageable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.activiti.cloud.services.common.security.SpringSecurityAuthenticationWrapper;
import org.activiti.cloud.services.core.ActivitiForbiddenException;
import org.activiti.cloud.services.core.SecurityPoliciesApplicationService;
import org.activiti.cloud.services.security.SecurityPolicy;
import org.activiti.runtime.api.NotFoundException;
import org.activiti.runtime.api.ProcessRuntime;
import org.activiti.runtime.api.model.ProcessDefinition;
import org.activiti.runtime.api.model.ProcessInstance;
import org.activiti.runtime.api.model.builders.ProcessPayloadBuilder;
import org.activiti.runtime.api.model.impl.ProcessInstanceImpl;
import org.activiti.runtime.api.model.payloads.GetProcessInstancesPayload;
import org.activiti.runtime.api.model.payloads.ResumeProcessPayload;
import org.activiti.runtime.api.model.payloads.SetProcessVariablesPayload;
import org.activiti.runtime.api.model.payloads.SignalPayload;
import org.activiti.runtime.api.model.payloads.StartProcessPayload;
import org.activiti.runtime.api.model.payloads.SuspendProcessPayload;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class SecurityAwareProcessInstanceServiceTest {

    @InjectMocks
    private SecurityAwareProcessInstanceService securityAwareProcessInstanceService;

    @Mock
    private ProcessRuntime processRuntime;

    @Mock
    private SecurityPoliciesApplicationService securityService;

    @Mock
    private SpringPageConverter springPageConverter;

    @Mock
    private org.activiti.runtime.api.query.Page<ProcessInstance> apiProcInstPage;

    @Mock
    private org.activiti.runtime.api.query.Page<ProcessDefinition> apiProcDefPage;

    @Mock
    private Page<ProcessInstance> springProcInstPage;

    @Mock
    private SpringSecurityAuthenticationWrapper authenticationWrapper;

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
        GetProcessInstancesPayload filter = mock(GetProcessInstancesPayload.class);
        given(securityService.restrictProcessInstQuery(SecurityPolicy.READ)).willReturn(filter);

        Pageable springPageable = mock(Pageable.class);
        org.activiti.runtime.api.query.Pageable apiPageable = mock(org.activiti.runtime.api.query.Pageable.class);
        given(springPageConverter.toAPIPageable(springPageable)).willReturn(apiPageable);

        given(processRuntime.processInstances(apiPageable,
                                              filter)).willReturn(apiProcInstPage);
        given(springPageConverter.toSpringPage(springPageable,
                                               apiProcInstPage)).willReturn(springProcInstPage);

        //when
        Page<ProcessInstance> authorizedProcessInstances = securityAwareProcessInstanceService.getAuthorizedProcessInstances(springPageable);

        //then
        assertThat(authorizedProcessInstances).isEqualTo(springProcInstPage);
    }

    @Test
    public void shouldStartProcessByKeyWithPermission() {
        //given
        String processDefinitionKey = "my-proc";
        ProcessDefinition processDefinition = buildProcessDefinition(processDefinitionKey);

        List<ProcessDefinition> processDefinitionList = new ArrayList<>();
        processDefinitionList.add(processDefinition);
        given(apiProcDefPage.getContent()).willReturn(processDefinitionList);
        given(processRuntime.processDefinitions(org.activiti.runtime.api.query.Pageable.of(0,
                                                                                           50),
                                                ProcessPayloadBuilder.processDefinitions().withProcessDefinitionKey(processDefinitionKey).build()))
                .willReturn(apiProcDefPage);

        given(processRuntime.processDefinition(processDefinitionKey)).willReturn(processDefinition);
        given(securityService.canWrite(processDefinitionKey)).willReturn(true);

        ProcessInstance processInstance = mock(ProcessInstance.class);
        given(processRuntime.start(any())).willReturn(processInstance);

        StartProcessPayload startProcess = buildStartProcessCmd(processDefinitionKey);

        //when
        ProcessInstance startedInstance = securityAwareProcessInstanceService.startProcess(startProcess);

        //then
        assertThat(startedInstance).isEqualTo(processInstance);
    }

    private StartProcessPayload buildStartProcessCmd(String processDefinitionKey) {
        return new StartProcessPayload(null,
                                       processDefinitionKey,
                                       "",
                                       null);
    }

    private ProcessDefinition buildProcessDefinition(String processDefinitionKey) {
        ProcessDefinition processDefinition = mock(ProcessDefinition.class);
        given(processDefinition.getKey()).willReturn(processDefinitionKey);
        return processDefinition;
    }

    @Test
    public void shouldNotStartWithoutPermission() {
        //given
        String processDefinitionKey = "my-proc";
        ProcessDefinition processDefinition = buildProcessDefinition(processDefinitionKey);

        List<ProcessDefinition> processDefinitionList = new ArrayList<>();
        processDefinitionList.add(processDefinition);
        given(apiProcDefPage.getContent()).willReturn(processDefinitionList);
        given(processRuntime.processDefinitions(org.activiti.runtime.api.query.Pageable.of(0,
                                                                                           50),
                                                ProcessPayloadBuilder.processDefinitions().withProcessDefinitionKey(processDefinitionKey).build()))
                .willReturn(apiProcDefPage);
        given(processRuntime.processDefinition(processDefinitionKey)).willReturn(processDefinition);
        given(securityService.canWrite(processDefinitionKey)).willReturn(false);

        //then
        assertThatExceptionOfType(ActivitiForbiddenException.class).isThrownBy(
                //when
                () -> securityAwareProcessInstanceService.startProcess(buildStartProcessCmd(processDefinitionKey))
        );
    }

    @Test
    public void shouldSignal() {
        String name = "go";
        Map<String, Object> inputVariables = Collections.singletonMap("var",
                                                                      "value");
        //given
        SignalPayload signalPayload = ProcessPayloadBuilder
                .signal()
                .withName(name)
                .withVariables(inputVariables)
                .build();

        //when
        securityAwareProcessInstanceService.signal(signalPayload);

        verify(processRuntime).signal(signalPayload);
    }

    @Test
    public void shouldNotSuspendWithoutPermission() {
        //given
        ProcessInstance processInstance = aProcessInstanceWithWritePermission(false);

        //then
        assertThatExceptionOfType(ActivitiForbiddenException.class).isThrownBy(
                //when
                () -> securityAwareProcessInstanceService.suspend(new SuspendProcessPayload(processInstance.getId()))
        );
    }

    private ProcessInstance aProcessInstanceWithWritePermission(boolean hasWritePermission) {
        ProcessInstance processInstance = buildProcessInstance(UUID.randomUUID().toString(),
                                                               UUID.randomUUID().toString(),
                                                               "my-proc");
        given(processRuntime.processInstance(processInstance.getId())).willReturn(processInstance);
        given(securityService.canRead(processInstance.getProcessDefinitionKey())).willReturn(true);
        ProcessDefinition processDefinition = buildProcessDefinition("my-proc");

        List<ProcessDefinition> processDefinitionList = new ArrayList<>();
        processDefinitionList.add(processDefinition);
        given(apiProcDefPage.getContent()).willReturn(processDefinitionList);
        given(processRuntime.processDefinitions(org.activiti.runtime.api.query.Pageable.of(0,
                                                                                           50),
                                                ProcessPayloadBuilder.processDefinitions().withProcessDefinitionId(processInstance.getProcessDefinitionId()).build()))
                .willReturn(apiProcDefPage);
        when(securityService.canWrite(processDefinition.getKey())).thenReturn(hasWritePermission);
        return processInstance;
    }

    private ProcessInstance buildProcessInstance(String processInstanceId,
                                                 String processDefinitionId,
                                                 String processDefinitionKey) {
        ProcessInstanceImpl processInstance = buildProcessInstance(processInstanceId,
                                                                   processDefinitionId);
        processInstance.setProcessDefinitionKey(processDefinitionKey);
        return processInstance;
    }

    private ProcessInstanceImpl buildProcessInstance(String processInstanceId,
                                                     String processDefinitionId) {
        ProcessInstanceImpl processInstance = new ProcessInstanceImpl();
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
                () -> securityAwareProcessInstanceService.activate(new ResumeProcessPayload(processInstance.getId()))
        );
    }

    @Test
    public void shouldSetProcessVariables() {
        //given
        ProcessInstance processInstance = aProcessInstanceWithWritePermission(true);

        Map<String, Object> variables = Collections.singletonMap("var",
                                                                 "value");
        //when
        SetProcessVariablesPayload setProcessVariablesPayload = ProcessPayloadBuilder
                .setVariables()
                .withProcessInstanceId(processInstance.getId())
                .withVariables(variables)
                .build();
        securityAwareProcessInstanceService.setProcessVariables(setProcessVariablesPayload);

        //then
        verify(processRuntime).setVariables(setProcessVariablesPayload);
    }

    @Test
    public void shouldNotSetProcessVariablesWithoutPermission() {
        //given
        ProcessInstance processInstance = aProcessInstanceWithWritePermission(false);

        //then
        assertThatExceptionOfType(ActivitiForbiddenException.class).isThrownBy(
                //when
                () -> securityAwareProcessInstanceService.setProcessVariables(ProcessPayloadBuilder
                                                                                      .setVariables()
                                                                                      .withProcessInstanceId(processInstance.getId())
                                                                                      .withVariables(Collections.emptyMap())
                                                                                      .build())

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
                () -> securityAwareProcessInstanceService.deleteProcessInstance(ProcessPayloadBuilder.delete(processInstanceId))
        ).withMessageStartingWith("Not found");
    }

    @Test
    public void shouldNotDeleteProcessInstanceWithoutPermission() {
        //given
        ProcessInstance processInstance = aProcessInstanceWithWritePermission(false);

        //then
        assertThatExceptionOfType(ActivitiForbiddenException.class).isThrownBy(
                //when
                () -> securityAwareProcessInstanceService.deleteProcessInstance(ProcessPayloadBuilder.delete(processInstance.getId()))
        ).withMessageStartingWith("Operation not permitted");
    }
}

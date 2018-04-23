/*
 * Copyright 2017 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.services.connectors.behavior;

import org.activiti.bpmn.model.ServiceTask;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextEntityImpl;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextManager;
import org.activiti.services.connectors.model.IntegrationRequestEvent;
import org.assertj.core.api.AssertionsForInterfaceTypes;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.context.ApplicationEventPublisher;

import static org.activiti.services.test.DelegateExecutionBuilder.anExecution;
import static org.activiti.test.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class MQServiceTaskBehaviorTest {

    private static final String CONNECTOR_TYPE = "payment";
    private static final String EXECUTION_ID = "execId";
    private static final String PROC_INST_ID = "procInstId";
    private static final String PROC_DEF_ID = "procDefId";
    private static final String FLOW_NODE_ID = "flowNodeId";
    private static final String INTEGRATION_CONTEXT_ID = "entityId";
    private static final String APP_NAME = "myApp";

    @Spy
    @InjectMocks
    private MQServiceTaskBehavior behavior;

    @Mock
    private IntegrationContextManager integrationContextManager;

    @Mock
    private RuntimeBundleProperties runtimeBundleProperties;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Captor
    private ArgumentCaptor<IntegrationRequestEvent> integrationRequestCaptor;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void executeShouldStoreTheIntegrationContextAndPublishASpringEvent() throws Exception {
        //given
        ServiceTask serviceTask = new ServiceTask();
        serviceTask.setImplementation(CONNECTOR_TYPE);

        DelegateExecution execution = anExecution()
                .withId(EXECUTION_ID)
                .withProcessInstanceId(PROC_INST_ID)
                .withProcessDefinitionId(PROC_DEF_ID)
                .withServiceTask(serviceTask)
                .withFlowNodeId(FLOW_NODE_ID)
                .build();
        given(runtimeBundleProperties.getServiceFullName()).willReturn(APP_NAME);
        IntegrationContextEntityImpl entity = new IntegrationContextEntityImpl();
        entity.setId(INTEGRATION_CONTEXT_ID);
        given(integrationContextManager.create()).willReturn(entity);

        //when
        behavior.execute(execution);

        //then
        assertThat(entity)
                .hasExecutionId(EXECUTION_ID)
                .hasProcessDefinitionId(PROC_DEF_ID)
                .hasProcessInstanceId(PROC_INST_ID);

        verify(eventPublisher).publishEvent(integrationRequestCaptor.capture());
        IntegrationRequestEvent event = integrationRequestCaptor.getValue();
        assertThat(event)
                .hasConnectorType(CONNECTOR_TYPE)
                .hasExecutionId(EXECUTION_ID)
                .hasProcessInstanceId(PROC_INST_ID)
                .hasProcessDefinitionId(PROC_DEF_ID)
                .hasIntegrationContextId(INTEGRATION_CONTEXT_ID)
                .hasFlowNodeId(FLOW_NODE_ID);
        AssertionsForInterfaceTypes.assertThat(event.getServiceFullName()).isEqualTo(APP_NAME);
    }

    @Test
    public void triggerShouldCallLeave() throws Exception {
        //given
        DelegateExecution execution = mock(DelegateExecution.class);
        doNothing().when(behavior).leave(execution);

        //when
        behavior.trigger(execution, null, null);

        //then
        verify(behavior).leave(execution);
    }
}
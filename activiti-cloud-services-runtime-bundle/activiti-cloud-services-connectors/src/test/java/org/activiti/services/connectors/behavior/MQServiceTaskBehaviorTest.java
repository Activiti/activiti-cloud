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

import org.activiti.api.process.model.IntegrationContext;
import org.activiti.bpmn.model.ServiceTask;
import org.activiti.cloud.api.process.model.impl.IntegrationRequestImpl;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextEntityImpl;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextManager;
import org.activiti.runtime.api.connector.DefaultServiceTaskBehavior;
import org.activiti.runtime.api.connector.IntegrationContextBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;

import static org.activiti.services.test.DelegateExecutionBuilder.anExecution;
import static org.activiti.test.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
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

    private MQServiceTaskBehavior behavior;

    @Mock
    private IntegrationContextManager integrationContextManager;

    @Mock
    private RuntimeBundleProperties runtimeBundleProperties;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private IntegrationContextBuilder integrationContextBuilder;

    @Mock
    private RuntimeBundleInfoAppender runtimeBundleInfoAppender;

    @Mock
    private DefaultServiceTaskBehavior defaultServiceTaskBehavior;

    @Captor
    private ArgumentCaptor<IntegrationRequestImpl> integrationRequestCaptor;

    @Before
    public void setUp() {
        initMocks(this);
        behavior = spy(new MQServiceTaskBehavior(integrationContextManager,
                                                 eventPublisher,
                                                 integrationContextBuilder,
                                                 runtimeBundleInfoAppender,
                                                 defaultServiceTaskBehavior));
    }

    @Test
    public void executeShouldDelegateToDefaultBehaviourWhenBeanIsAvailable() {
        //given
        DelegateExecution execution = mock(DelegateExecution.class);
        given(defaultServiceTaskBehavior.hasConnectorBean(execution)).willReturn(true);

        //when
        behavior.execute(execution);

        //then
        verify(defaultServiceTaskBehavior).execute(execution);
    }

    @Test
    public void executeShouldStoreTheIntegrationContextAndPublishASpringEvent() {
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

        given(applicationContext.containsBean(CONNECTOR_TYPE)).willReturn(false);

        IntegrationContext integrationContext = mock(IntegrationContext.class);
        given(integrationContextBuilder.from(entity, execution, null)).willReturn(integrationContext);

        //when
        behavior.execute(execution);

        //then
        assertThat(entity)
                .hasExecutionId(EXECUTION_ID)
                .hasProcessDefinitionId(PROC_DEF_ID)
                .hasProcessInstanceId(PROC_INST_ID);

        verify(eventPublisher).publishEvent(integrationRequestCaptor.capture());
        IntegrationRequestImpl integrationRequest = integrationRequestCaptor.getValue();
        assertThat(integrationRequest.getIntegrationContext())
                .isEqualTo(integrationContext);

        verify(runtimeBundleInfoAppender).appendRuntimeBundleInfoTo(integrationRequest);
    }

    @Test
    public void triggerShouldCallLeave() {
        //given
        DelegateExecution execution = mock(DelegateExecution.class);
        doNothing().when(behavior).leave(execution);

        //when
        behavior.trigger(execution, null, null);

        //then
        verify(behavior).leave(execution);
    }
}
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
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.messaging.Message;

import static org.activiti.services.test.DelegateExecutionBuilder.anExecution;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class MQServiceTaskBehaviorTest {

    private static final String CONNECTOR_TYPE = "payment";
    private static final String EXECUTION_ID = "execId";
    private static final String PROC_INST_ID = "procInstId";
    private static final String PROC_DEF_ID = "procDefId";

    @Spy
    @InjectMocks
    private MQServiceTaskBehavior behavior;

    @Mock
    private IntegrationContextManager integrationContextManager;


    @Mock
    private RuntimeBundleProperties runtimeBundleProperties;

    @Captor
    private ArgumentCaptor<Message<IntegrationRequestEvent>> integrationRequestCaptor;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void executeShouldStoreTheIntegrationContextAndRegisterAMessage() throws Exception {
        //given
        ServiceTask serviceTask = new ServiceTask();
        serviceTask.setImplementation(CONNECTOR_TYPE);

        DelegateExecution execution = anExecution()
                .withId(EXECUTION_ID)
                .withProcessInstanceId(PROC_INST_ID)
                .withProcessDefinitionId(PROC_DEF_ID)
                .withServiceTask(serviceTask)
                .build();
        given(runtimeBundleProperties.getName()).willReturn("myApp");

        IntegrationContextEntityImpl entity = new IntegrationContextEntityImpl();
        entity.setId("entityId");
        given(integrationContextManager.create()).willReturn(entity);
        doNothing().when(behavior).registerTransactionSynchronization(ArgumentMatchers.any());

        //when
        behavior.execute(execution);

        //then
        verify(behavior).registerTransactionSynchronization(ArgumentMatchers.any());
        assertThat(entity.getExecutionId()).isEqualTo(EXECUTION_ID);
        assertThat(entity.getProcessDefinitionId()).isEqualTo(PROC_DEF_ID);
        assertThat(entity.getProcessInstanceId()).isEqualTo(PROC_INST_ID);

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
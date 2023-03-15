/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.services.connectors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.activiti.api.process.model.IntegrationContext;
import org.activiti.bpmn.model.ServiceTask;
import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.activiti.cloud.api.process.model.impl.IntegrationRequestImpl;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.el.ExpressionManager;
import org.activiti.engine.impl.persistence.deploy.DeploymentManager;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextEntity;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.runtime.api.connector.IntegrationContextBuilder;
import org.activiti.runtime.api.impl.ExtensionsVariablesMappingProvider;
import org.activiti.services.connectors.message.IntegrationContextMessageBuilderFactory;
import org.activiti.services.test.DelegateExecutionBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;

@ExtendWith(MockitoExtension.class)
public class IntegrationRequestSenderTest {

    private static final String MY_PARENT_PROC_ID = "my-parent-proc-id";
    private static final String SERVICE_VERSION = "serviceVersion";
    private static final String SERVICE_TYPE = "serviceType";
    private static final String SPRING_APP_NAME = "springAppName";
    private static final String CONNECTOR_TYPE = "payment";
    private static final String ROOT_PROC_INST_ID = "rootProcInstId";
    private static final String PROC_INST_ID = "procInstId";
    private static final String PROC_DEF_ID = "procDefId";
    private static final String BUSINESS_KEY = "my-business-key";
    private static final String INTEGRATION_CONTEXT_ID = "intContextId";
    private static final String APP_NAME = "appName";

    private IntegrationRequestSender integrationRequestSender;

    @Mock
    private StreamBridge streamBridge;

    @Spy
    private RuntimeBundleProperties runtimeBundleProperties = new RuntimeBundleProperties() {
        {
            setAppName(APP_NAME);
            setServiceType(SERVICE_TYPE);
            setServiceVersion(SERVICE_VERSION);
            setRbSpringAppName(SPRING_APP_NAME);
        }
    };

    @Mock
    private RuntimeBundleInfoAppender runtimeBundleInfoAppender;

    @Mock
    private RuntimeBundleProperties.RuntimeBundleEventsProperties eventsProperties;

    private IntegrationContextMessageBuilderFactory messageBuilderFactory;

    @Mock
    private IntegrationContextEntity integrationContextEntity;

    @Mock
    private ExtensionsVariablesMappingProvider inboundVariablesProvider;

    @Mock
    private ExpressionManager expressionManager;

    private DelegateExecution delegateExecution;

    @Captor
    private ArgumentCaptor<Message<IntegrationRequest>> integrationRequestMessageCaptor;

    private IntegrationRequestImpl integrationRequest;

    @BeforeEach
    public void setUp() {
        configureDeploymentManager();
        messageBuilderFactory = new IntegrationContextMessageBuilderFactory(runtimeBundleProperties);

        integrationRequestSender = new IntegrationRequestSender(streamBridge, messageBuilderFactory);

        configureProperties();
        configureExecution();

        when(runtimeBundleProperties.getServiceFullName()).thenReturn(APP_NAME);

        IntegrationContextEntity contextEntity = mock(IntegrationContextEntity.class);
        given(contextEntity.getId()).willReturn(INTEGRATION_CONTEXT_ID);

        IntegrationContext integrationContext = new IntegrationContextBuilder(
            inboundVariablesProvider,
            expressionManager
        )
            .from(contextEntity, delegateExecution);

        integrationRequest = new IntegrationRequestImpl(integrationContext);
        integrationRequest.setServiceFullName(APP_NAME);
    }

    private void configureDeploymentManager() {
        ProcessEngineConfigurationImpl processEngineConfiguration = mock(ProcessEngineConfigurationImpl.class);
        Context.setProcessEngineConfiguration(processEngineConfiguration);

        DeploymentManager deploymentManager = mock(DeploymentManager.class);
        ProcessDefinition processDefinition = mock(ProcessDefinition.class);

        given(processEngineConfiguration.getDeploymentManager()).willReturn(deploymentManager);
        given(deploymentManager.findDeployedProcessDefinitionById(PROC_DEF_ID)).willReturn(processDefinition);
    }

    private void configureExecution() {
        ServiceTask serviceTask = new ServiceTask();
        serviceTask.setName("Service Task");
        serviceTask.setImplementation(CONNECTOR_TYPE);

        delegateExecution =
            DelegateExecutionBuilder
                .anExecution()
                .withServiceTask(serviceTask)
                .withProcessDefinitionId(PROC_DEF_ID)
                .withRootProcessInstanceId(ROOT_PROC_INST_ID)
                .withProcessInstanceId(PROC_INST_ID)
                .withBusinessKey(BUSINESS_KEY)
                .withParentProcessInstanceId(MY_PARENT_PROC_ID)
                .build();

        Expression mockExpression = mock(Expression.class);
        given(mockExpression.getValue(delegateExecution)).willReturn(serviceTask.getName());
        given(expressionManager.createExpression(anyString())).willReturn(mockExpression);
    }

    private void configureProperties() {
        when(runtimeBundleProperties.getServiceFullName()).thenReturn(APP_NAME);
    }

    @Test
    public void shouldSendIntegrationRequestMessage() {
        //when
        integrationRequestSender.sendIntegrationRequest(integrationRequest);

        //then
        verify(streamBridge).send(eq(CONNECTOR_TYPE), integrationRequestMessageCaptor.capture());
        Message<IntegrationRequest> integrationRequestMessage = integrationRequestMessageCaptor.getValue();

        IntegrationRequest sentIntegrationRequestEvent = integrationRequestMessage.getPayload();
        assertThat(sentIntegrationRequestEvent).isEqualTo(integrationRequest);
        assertThat(integrationRequestMessage.getHeaders().get(IntegrationRequestSender.CONNECTOR_TYPE))
            .isEqualTo(CONNECTOR_TYPE);
    }
}

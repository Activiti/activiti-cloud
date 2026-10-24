/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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

import static org.activiti.cloud.common.messaging.config.FunctionRouterConfiguration.FUNCTION_DESTINATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.activiti.api.process.model.IntegrationContext;
import org.activiti.api.runtime.model.impl.IntegrationContextImpl;
import org.activiti.bpmn.model.ServiceTask;
import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.activiti.cloud.api.process.model.impl.IntegrationRequestImpl;
import org.activiti.cloud.common.messaging.config.FunctionBindingConfiguration;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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

    @Mock
    private FunctionBindingConfiguration.BindingResolver bindingResolver;

    @Mock
    private IntegrationRequestReloadService integrationRequestReloadService;

    private DelegateExecution delegateExecution;

    @Captor
    private ArgumentCaptor<Message<IntegrationRequest>> integrationRequestMessageCaptor;

    private IntegrationRequestImpl integrationRequest;

    @BeforeEach
    public void setUp() {
        configureDeploymentManager();
        messageBuilderFactory = new IntegrationContextMessageBuilderFactory(runtimeBundleProperties);

        integrationRequestSender = new IntegrationRequestSender(
            streamBridge,
            messageBuilderFactory,
            bindingResolver,
            integrationRequestReloadService
        );

        configureProperties();
        configureExecution();

        when(bindingResolver.getBindingDestination(CONNECTOR_TYPE)).thenReturn(CONNECTOR_TYPE);
        when(runtimeBundleProperties.getServiceFullName()).thenReturn(APP_NAME);

        IntegrationContextEntity contextEntity = mock(IntegrationContextEntity.class);
        given(contextEntity.getId()).willReturn(INTEGRATION_CONTEXT_ID);

        IntegrationContext integrationContext = new IntegrationContextBuilder(
            inboundVariablesProvider,
            expressionManager
        ).from(contextEntity, delegateExecution);

        integrationRequest = new IntegrationRequestImpl(integrationContext);
        integrationRequest.setServiceFullName(APP_NAME);

        when(integrationRequestReloadService.reload(INTEGRATION_CONTEXT_ID)).thenReturn(
            Optional.of(integrationRequest)
        );
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

        delegateExecution = DelegateExecutionBuilder.anExecution()
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
        // given
        TransactionSynchronizationManager.initSynchronization();

        //when
        integrationRequestSender.sendIntegrationRequest(integrationRequest);

        verify(integrationRequestReloadService, never()).reload(anyString());

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        //then
        verify(streamBridge).send(eq(CONNECTOR_TYPE), integrationRequestMessageCaptor.capture());
        Message<IntegrationRequest> integrationRequestMessage = integrationRequestMessageCaptor.getValue();

        IntegrationRequest sentIntegrationRequestEvent = integrationRequestMessage.getPayload();
        assertThat(sentIntegrationRequestEvent).isSameAs(integrationRequest);
        assertThat(integrationRequestMessage.getHeaders().get(IntegrationRequestSender.CONNECTOR_TYPE)).isEqualTo(
            CONNECTOR_TYPE
        );
        assertThat(integrationRequestMessage.getHeaders().get(FUNCTION_DESTINATION)).isEqualTo(CONNECTOR_TYPE);

        TransactionSynchronizationManager.clear();
    }

    @Test
    public void shouldNotSendMessageWhenReloadReturnsEmpty() {
        TransactionSynchronizationManager.initSynchronization();
        when(integrationRequestReloadService.reload(INTEGRATION_CONTEXT_ID)).thenReturn(Optional.empty());

        integrationRequestSender.sendIntegrationRequest(integrationRequest);

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        verify(streamBridge, never()).send(anyString(), any(Message.class));

        TransactionSynchronizationManager.clear();
    }

    @Test
    public void shouldIgnoreReloadFailures() {
        TransactionSynchronizationManager.initSynchronization();
        doThrow(new RuntimeException("boom")).when(integrationRequestReloadService).reload(INTEGRATION_CONTEXT_ID);

        integrationRequestSender.sendIntegrationRequest(integrationRequest);

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        verify(streamBridge, never()).send(anyString(), any(Message.class));

        TransactionSynchronizationManager.clear();
    }

    @Test
    public void shouldRequireActiveTransactionSynchronization() {
        assertThatThrownBy(() -> integrationRequestSender.sendIntegrationRequest(integrationRequest))
            .isInstanceOf(org.springframework.transaction.IllegalTransactionStateException.class)
            .hasMessage("Transaction synchronization must be active.");
    }

    @Test
    public void shouldNotRetainOriginalIntegrationRequestBeforeAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();

        try {
            String heavyIntegrationContextId = "heavy-context-id";
            IntegrationRequestImpl heavyIntegrationRequest = createHeavyIntegrationRequest(heavyIntegrationContextId);
            IntegrationRequestImpl reloadedIntegrationRequest = createReloadedIntegrationRequest(
                heavyIntegrationContextId
            );
            when(integrationRequestReloadService.reload(heavyIntegrationContextId)).thenReturn(
                Optional.of(reloadedIntegrationRequest)
            );

            ReferenceQueue<IntegrationRequest> referenceQueue = new ReferenceQueue<>();
            WeakReference<IntegrationRequest> reference = new WeakReference<>(heavyIntegrationRequest, referenceQueue);

            integrationRequestSender.sendIntegrationRequest(heavyIntegrationRequest);

            heavyIntegrationRequest = null;

            assertThat(awaitCollection(reference, referenceQueue)).isTrue();
            assertThat(reference.get()).isNull();

            TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

            verify(streamBridge).send(eq(CONNECTOR_TYPE), integrationRequestMessageCaptor.capture());
            Message<IntegrationRequest> integrationRequestMessage = integrationRequestMessageCaptor.getValue();

            assertThat(integrationRequestMessage.getPayload()).isSameAs(reloadedIntegrationRequest);
        } finally {
            TransactionSynchronizationManager.clear();
        }
    }

    private IntegrationRequestImpl createHeavyIntegrationRequest(String integrationContextId) {
        IntegrationContextImpl integrationContext = new IntegrationContextImpl();
        integrationContext.setId(integrationContextId);
        integrationContext.setConnectorType(CONNECTOR_TYPE);
        integrationContext.setProcessInstanceId(PROC_INST_ID);
        integrationContext.setRootProcessInstanceId(ROOT_PROC_INST_ID);
        integrationContext.setProcessDefinitionId(PROC_DEF_ID);
        integrationContext.setBusinessKey(BUSINESS_KEY);
        integrationContext.setParentProcessInstanceId(MY_PARENT_PROC_ID);
        integrationContext.setClientId("clientId");
        integrationContext.setClientName("clientName");
        integrationContext.setClientType("ServiceTask");
        integrationContext.setAppVersion("1");
        integrationContext.addInBoundVariable("payload", createLargeNestedPayload());

        IntegrationRequestImpl heavyIntegrationRequest = new IntegrationRequestImpl(integrationContext);
        heavyIntegrationRequest.setServiceFullName(APP_NAME);
        return heavyIntegrationRequest;
    }

    private IntegrationRequestImpl createReloadedIntegrationRequest(String integrationContextId) {
        IntegrationContextImpl integrationContext = new IntegrationContextImpl();
        integrationContext.setId(integrationContextId);
        integrationContext.setConnectorType(CONNECTOR_TYPE);
        integrationContext.setProcessInstanceId(PROC_INST_ID);
        integrationContext.setRootProcessInstanceId(ROOT_PROC_INST_ID);
        integrationContext.setProcessDefinitionId(PROC_DEF_ID);
        integrationContext.setBusinessKey(BUSINESS_KEY);
        integrationContext.setParentProcessInstanceId(MY_PARENT_PROC_ID);
        integrationContext.setClientId("clientId");
        integrationContext.setClientName("clientName");
        integrationContext.setClientType("ServiceTask");
        integrationContext.setAppVersion("1");
        integrationContext.addInBoundVariable("payload", List.of("reloaded"));

        IntegrationRequestImpl reloadedIntegrationRequest = new IntegrationRequestImpl(integrationContext);
        reloadedIntegrationRequest.setServiceFullName(APP_NAME);
        return reloadedIntegrationRequest;
    }

    private List<List<byte[]>> createLargeNestedPayload() {
        List<List<byte[]>> outer = new ArrayList<>();

        for (int outerIndex = 0; outerIndex < 16; outerIndex++) {
            List<byte[]> inner = new ArrayList<>();
            for (int innerIndex = 0; innerIndex < 16; innerIndex++) {
                inner.add(new byte[64 * 1024]);
            }
            outer.add(inner);
        }

        return outer;
    }

    private boolean awaitCollection(
        WeakReference<IntegrationRequest> reference,
        ReferenceQueue<IntegrationRequest> referenceQueue
    ) {
        for (int attempt = 0; attempt < 20; attempt++) {
            if (reference.get() == null || referenceQueue.poll() != null) {
                return true;
            }

            createGarbagePressure();
            System.gc();
            System.runFinalization();
        }

        return reference.get() == null || referenceQueue.poll() != null;
    }

    private void createGarbagePressure() {
        List<byte[]> pressure = new ArrayList<>();
        for (int index = 0; index < 16; index++) {
            pressure.add(new byte[64 * 1024]);
        }
    }
}

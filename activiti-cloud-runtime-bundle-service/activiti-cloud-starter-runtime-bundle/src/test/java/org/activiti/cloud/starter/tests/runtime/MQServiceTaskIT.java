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
package org.activiti.cloud.starter.tests.runtime;

import static org.activiti.cloud.starter.tests.helper.ProcessInstanceRestTemplate.CONTENT_TYPE_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.activiti.api.model.shared.event.RuntimeEvent;
import org.activiti.api.model.shared.model.VariableInstance;
import org.activiti.api.process.model.IntegrationContext;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.cloud.api.model.shared.events.CloudVariableUpdatedEvent;
import org.activiti.cloud.api.process.model.events.CloudIntegrationRequestedEvent;
import org.activiti.cloud.api.process.model.events.CloudIntegrationResultReceivedEvent;
import org.activiti.cloud.services.rest.api.ReplayServiceTaskRequest;
import org.activiti.cloud.starter.tests.services.audit.AuditConsumerStreamHandler;
import org.activiti.cloud.starter.tests.services.audit.AuditProducerIT;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.services.connectors.conf.ConnectorImplementationsProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.config.BindingProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles(AuditProducerIT.AUDIT_PRODUCER_IT)
@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
public class MQServiceTaskIT extends AbstractMQServiceTaskIT {

    @Autowired
    private CanFailConnector canFailConnector;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private AuditConsumerStreamHandler auditConsumer;

    @Autowired
    protected ConnectorImplementationsProvider connectorImplementationsProvider;

    @Test
    public void shouldConfigureDefaultConnectorBindingProperties() {
        //given

        //when
        Map<String, BindingProperties> bindings = bindingServiceProperties.getBindings();

        //then
        assertThat(bindings)
            .extractingFromEntries(entry ->
                new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue().getDestination())
            )
            .contains(
                entry("mealsConnector", "mealsConnector"),
                entry("rest.GET", "rest.GET"),
                entry("perfromBusinessTask", "perfromBusinessTask"),
                entry("anyImplWithoutHandler", "anyImplWithoutHandler"),
                entry("payment", "payment"),
                entry("Constants Connector.constantsActionName", "Constants Connector.constantsActionName"),
                entry(
                    "Variable Mapping Connector.variableMappingActionName",
                    "Variable Mapping Connector.variableMappingActionName"
                ),
                entry("miCloudConnector", "miCloudConnector")
            );
    }

    @Test
    public void shouldRecoverFromFailure() {
        //given
        Map<String, Object> variables = new HashMap<>();
        variables.put("firstName", "John");
        ProcessInstance procInst = runtimeService.startProcessInstanceByKey(
            "MQServiceTaskErrorRecoverProcess",
            "businessKey",
            variables
        );
        assertThat(procInst).isNotNull();
        await("the service task should fail the execution").untilTrue(canFailConnector.errorSent());

        assertThat(taskService.createTaskQuery().processInstanceId(procInst.getProcessInstanceId()).list()).isEmpty();
        //when
        IntegrationContext integrationContext = canFailConnector
            .getLatestReceivedIntegrationRequest()
            .getIntegrationContext();
        canFailConnector.setShouldSendError(false);
        replayServiceTask(integrationContext);

        //then
        await("the execution should arrive in the human tasks which follows the service task").untilAsserted(() -> {
            List<Task> tasks = taskService.createTaskQuery().processInstanceId(procInst.getProcessInstanceId()).list();
            assertThat(tasks).isNotNull();
            assertThat(tasks).extracting(Task::getName).containsExactly("Schedule meeting after service");
        });
    }

    private void replayServiceTask(IntegrationContext integrationContext) {
        identityTokenProducer.withTestUser("testadmin");
        final ResponseEntity<Void> responseEntity = testRestTemplate.exchange(
            "/admin/v1/executions/{executionId}/replay/service-task",
            HttpMethod.POST,
            new HttpEntity<>(new ReplayServiceTaskRequest(integrationContext.getClientId()), CONTENT_TYPE_HEADER),
            new ParameterizedTypeReference<>() {},
            integrationContext.getExecutionId()
        );
        identityTokenProducer.withTestUser(keycloakTestUser);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldRemoveInboundAndOutBoundVarsFromIntegrationEventsForEphemeralVariables() {
        auditConsumer.clear();
        processInstanceRestTemplate.startProcess(
            ProcessPayloadBuilder.start().withProcessDefinitionKey("ProcessWithRestConnectorEphemeralVars").build()
        );
        await().untilAsserted(() -> {
            List<CloudIntegrationRequestedEvent> integrationRequestedEvents = auditConsumer.getAllReceivedEvents(
                CloudIntegrationRequestedEvent.class
            );

            IntegrationContext integrationRequestEntity = integrationRequestedEvents.getFirst().getEntity();

            assertThat(integrationRequestEntity)
                .extracting(IntegrationContext::hasEphemeralVariables, IntegrationContext::getInBoundVariables)
                .containsExactly(true, Map.of());

            List<CloudVariableUpdatedEvent> variableUpdatedEvents = auditConsumer.getAllReceivedEvents(
                CloudVariableUpdatedEvent.class
            );

            assertThat(variableUpdatedEvents)
                .extracting(RuntimeEvent::getEntity)
                .extracting(VariableInstance::getName, VariableInstance::getValue)
                .contains(tuple("result", "fromConnector"));

            List<CloudIntegrationResultReceivedEvent> integrationResultEvents = auditConsumer.getAllReceivedEvents(
                CloudIntegrationResultReceivedEvent.class
            );

            IntegrationContext integrationResponseEntity = integrationResultEvents.getFirst().getEntity();

            assertThat(integrationResponseEntity)
                .extracting(
                    IntegrationContext::hasEphemeralVariables,
                    IntegrationContext::getInBoundVariables,
                    IntegrationContext::getOutBoundVariables
                )
                .containsExactly(true, Map.of(), Map.of());
        });
    }

    @Test
    void shouldNotRemoveOutBoundVarsFromIntegrationEventsForNonEphemeralVariables() {
        auditConsumer.clear();
        processInstanceRestTemplate.startProcess(
            ProcessPayloadBuilder.start().withProcessDefinitionKey("ProcessWithRestConnectorNonEphemeralVars").build()
        );
        await().untilAsserted(() -> {
            List<CloudIntegrationRequestedEvent> integrationRequestedEvents = auditConsumer.getAllReceivedEvents(
                CloudIntegrationRequestedEvent.class
            );

            IntegrationContext integrationRequestEntity = integrationRequestedEvents.getFirst().getEntity();

            assertThat(integrationRequestEntity)
                .extracting(IntegrationContext::hasEphemeralVariables, IntegrationContext::getInBoundVariables)
                .containsExactly(false, Map.of("restUrl", "https://jsonplaceholder.typicode.com/posts/1"));

            List<CloudIntegrationResultReceivedEvent> integrationResultEvents = auditConsumer.getAllReceivedEvents(
                CloudIntegrationResultReceivedEvent.class
            );
            assertThat(integrationResultEvents).isNotEmpty();

            IntegrationContext integrationResponseEntity = integrationResultEvents.getFirst().getEntity();

            assertThat(integrationResponseEntity)
                .extracting(
                    IntegrationContext::hasEphemeralVariables,
                    IntegrationContext::getInBoundVariables,
                    IntegrationContext::getOutBoundVariables
                )
                .containsExactly(false, Map.of(), Map.of("restResult", "fromConnector"));
        });
    }
}

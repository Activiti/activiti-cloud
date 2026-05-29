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
package org.activiti.cloud.starter.tests.services.audit;

import static org.activiti.api.process.model.events.ProcessRuntimeEvent.ProcessEvents.PROCESS_CREATED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.common.feature.FeatureToggle;
import org.activiti.cloud.common.feature.FeatureToggleHolder;
import org.activiti.cloud.common.feature.FeatureToggleHolderInitializer;
import org.activiti.cloud.services.events.listeners.MessageProducerCommandContextCloseListener;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.containers.RabbitMQContainerApplicationInitializer;
import org.activiti.cloud.services.test.containers.RabbitMQQueuesCleanupTestExecutionListener;
import org.activiti.cloud.starter.tests.helper.ProcessInstanceRestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestExecutionListeners.MergeMode;
import org.springframework.test.context.TestPropertySource;

/**
 * Verifies that with the {@code ACTIVITI_FEATURES_SPLIT_PROCESS_CREATED_EVENT_ENABLED=true}
 * env var (resolved to {@code activiti.features.split-process-created-event.enabled} via
 * Spring Boot relaxed binding), the runtime bundle publishes the root PROCESS_CREATED event
 * in a separate Spring Cloud Stream message ahead of the rest of the events accumulated in
 * the same command context.
 *
 * <p>The listener writes a single binding ({@code auditProducer} → destination {@code engineEvents}).
 * In production, both the audit-consumer and the query-consumer subscribe to that destination
 * with their own consumer group, so the standalone first message is what reaches the query
 * service and lets it materialise the process row immediately. Here the assertion rides on
 * {@link AuditConsumerStreamHandler}, which subscribes to the same {@code engineEvents}
 * destination — the consumer group differs but the byte stream produced by the listener is
 * identical to what the query consumer would receive.
 *
 * <p>Uses a real RabbitMQ container (instead of the in-memory test binder) to exercise the
 * binder's actual message boundaries: each {@code auditProducer.send(...)} must produce one
 * AMQP message, and the standalone PROCESS_CREATED must arrive before the bulk of the
 * remaining events.
 */
@ActiveProfiles(AuditProducerIT.AUDIT_PRODUCER_IT)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "spring.cloud.stream.default-binder=rabbit"
)
@TestPropertySource("classpath:application-test.properties")
@ContextConfiguration(
    classes = ServicesAuditITConfiguration.class,
    initializers = {
        RabbitMQContainerApplicationInitializer.class,
        KeycloakContainerApplicationInitializer.class,
        SplitProcessCreatedEventAuditIT.SplitProcessCreatedEnvVarInitializer.class,
    }
)
@AutoConfigureTestRestTemplate
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@TestExecutionListeners(
    value = RabbitMQQueuesCleanupTestExecutionListener.class,
    mergeMode = MergeMode.MERGE_WITH_DEFAULTS
)
class SplitProcessCreatedEventAuditIT {

    private static final String SIMPLE_PROCESS = "SimpleProcess";

    @Autowired
    private ProcessInstanceRestTemplate processInstanceRestTemplate;

    @Autowired
    private AuditConsumerStreamHandler streamHandler;

    @Autowired
    private FeatureToggle featureToggle;

    @Autowired
    private FeatureToggleHolderInitializer featureToggleHolderInitializer;

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:split-process-created-test");
    }

    @BeforeEach
    void setUp() {
        streamHandler.clear();
    }

    @Test
    void should_wire_feature_toggle_into_holder() {
        assertThat(featureToggleHolderInitializer).isNotNull();
        assertThat(
            featureToggle.isEnabled(MessageProducerCommandContextCloseListener.SPLIT_PROCESS_CREATED_EVENT_FEATURE)
        )
            .isTrue();
        assertThat(
            FeatureToggleHolder.isEnabled(MessageProducerCommandContextCloseListener.SPLIT_PROCESS_CREATED_EVENT_FEATURE)
        )
            .isTrue();
    }

    @Test
    void should_publish_root_process_created_as_standalone_message() {
        ResponseEntity<CloudProcessInstance> startResponse = processInstanceRestTemplate.startProcess(
            ProcessPayloadBuilder
                .start()
                .withProcessDefinitionKey(SIMPLE_PROCESS)
                .withBusinessKey("split-feature-business-key")
                .build()
        );
        String rootProcessInstanceId = startResponse.getBody().getId();

        await().untilAsserted(() -> {
            List<List<CloudRuntimeEvent<?, ?>>> batches = streamHandler.getReceivedBatches();

            assertThat(batches).isNotEmpty();
            List<CloudRuntimeEvent<?, ?>> firstBatch = batches.getFirst();
            assertThat(firstBatch).hasSize(1);
            assertThat(firstBatch.getFirst().getEventType()).isEqualTo(PROCESS_CREATED);
            assertThat(firstBatch.getFirst().getEntityId()).isEqualTo(rootProcessInstanceId);

            assertThat(batches.subList(1, batches.size()))
                .flatExtracting(events -> events)
                .filteredOn(event -> PROCESS_CREATED.equals(event.getEventType()))
                .extracting(CloudRuntimeEvent::getEntityId)
                .doesNotContain(rootProcessInstanceId);
        });
    }

    /**
     * Injects the {@code ACTIVITI_FEATURES_SPLIT_PROCESS_CREATED_EVENT_ENABLED} env var as a
     * {@link SystemEnvironmentPropertySource} so Spring Boot's relaxed binding maps it to
     * {@code activiti.features.split-process-created-event.enabled} — exercising the same
     * resolution path used in the deployed runtime bundle, instead of bypassing it with a
     * dotted property override.
     */
    public static class SplitProcessCreatedEnvVarInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            Map<String, Object> envVars = new HashMap<>();
            envVars.put("ACTIVITI_FEATURES_SPLIT_PROCESS_CREATED_EVENT_ENABLED", "true");
            applicationContext
                .getEnvironment()
                .getPropertySources()
                .addFirst(
                    new SystemEnvironmentPropertySource(
                        StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME + "Override",
                        envVars
                    )
                );
        }
    }
}

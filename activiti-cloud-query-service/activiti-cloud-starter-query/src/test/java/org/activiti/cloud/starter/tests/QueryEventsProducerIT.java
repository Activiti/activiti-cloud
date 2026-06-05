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
package org.activiti.cloud.starter.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCreatedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessStartedEventImpl;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.starters.test.MyProducer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "spring.cloud.stream.bindings.queryEventsProducer.destination=queryEvents"
)
@TestPropertySource("classpath:application-test.properties")
@ContextConfiguration(initializers = { KeycloakContainerApplicationInitializer.class })
@Import(TestChannelBinderConfiguration.class)
@DirtiesContext
public class QueryEventsProducerIT {

    private static final String QUERY_EVENTS_DESTINATION = "queryEvents";

    @Autowired
    private MyProducer producer;

    @Autowired
    private OutputDestination outputDestination;

    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @MockitoBean
    private BuildProperties buildProperties;

    @AfterEach
    public void tearDown() {
        outputDestination.clear(QUERY_EVENTS_DESTINATION);
        processInstanceRepository.deleteAll();
    }

    @Test
    public void shouldForwardEventsToQueryEventsDestinationAfterCommit() {
        //given
        ProcessInstanceImpl processInstance = new ProcessInstanceImpl();
        processInstance.setId("queryEventsProducerIT-pi");
        processInstance.setProcessDefinitionKey("process-key");

        CloudProcessCreatedEventImpl created = new CloudProcessCreatedEventImpl(processInstance);
        CloudProcessStartedEventImpl started = new CloudProcessStartedEventImpl(processInstance);

        //when
        producer.send(created, started);

        //then the projection commits and the listener forwards the events
        await()
            .atMost(Duration.ofSeconds(15))
            .untilAsserted(() -> assertThat(processInstanceRepository.findById(processInstance.getId())).isPresent());

        Message<byte[]> received = outputDestination.receive(
            Duration.ofSeconds(15).toMillis(),
            QUERY_EVENTS_DESTINATION
        );
        assertThat(received).isNotNull();
        assertThat(new String(received.getPayload()))
            .contains("PROCESS_CREATED")
            .contains("PROCESS_STARTED")
            .contains(processInstance.getId());
    }
}

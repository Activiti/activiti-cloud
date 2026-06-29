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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCreatedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessStartedEventImpl;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.starters.test.MyProducer;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

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

    @MockitoSpyBean
    private MessageChannel queryEventsProducer;

    @Autowired
    private QueueChannel queryEventsChannel;

    @AfterEach
    public void tearDown() {
        outputDestination.clear(QUERY_EVENTS_DESTINATION);
        while (queryEventsChannel.receive(0) != null) {
            // drain
        }
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
        await().untilAsserted(() ->
            assertThat(processInstanceRepository.findById(processInstance.getId())).isPresent()
        );

        await().untilAsserted(() -> verify(queryEventsProducer).send(any(Message.class)));
        await().untilAsserted(() -> assertThat(queryEventsChannel.getQueueSize()).isZero());

        Message<byte[]> received = outputDestination.receive(
            Duration.ofSeconds(10).toMillis(),
            QUERY_EVENTS_DESTINATION
        );
        assertThat(received).isNotNull();
        assertThat(new String(received.getPayload()))
            .contains("PROCESS_CREATED")
            .contains("PROCESS_STARTED")
            .contains(processInstance.getId());
    }

    @Test
    public void shouldRollbackWhenFailForwardEventsToQueryEventsDestination() {
        //given
        ProcessInstanceImpl processInstance = new ProcessInstanceImpl();
        processInstance.setId("queryEventsProducerIT-pi");
        processInstance.setProcessDefinitionKey("process-key");

        CloudProcessCreatedEventImpl created = new CloudProcessCreatedEventImpl(processInstance);
        CloudProcessStartedEventImpl started = new CloudProcessStartedEventImpl(processInstance);

        CountDownLatch countDownLatch = new CountDownLatch(1);

        doAnswer(arg -> {
            countDownLatch.countDown();

            throw new RuntimeException();
        })
            .when(queryEventsProducer)
            .send(any(Message.class));

        //when
        producer.send(created, started);

        //then the projection commits and the listener forwards the events
        await()
            .atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> assertThat(processInstanceRepository.findById(processInstance.getId())).isPresent());

        await().untilAsserted(() -> assertThat(countDownLatch.await(5, TimeUnit.SECONDS)).isTrue());

        assertThat(queryEventsChannel.getQueueSize()).isEqualTo(1);

        Message<?> message = queryEventsChannel.receive();

        assertThat(message.getPayload())
            .asInstanceOf(InstanceOfAssertFactories.list(CloudRuntimeEvent.class))
            .hasSize(2);
    }
}

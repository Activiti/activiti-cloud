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
package org.activiti.cloud.notifications.graphql.starter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.activiti.api.runtime.model.impl.BPMNSignalImpl;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNSignalReceivedEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNSignalReceivedEventImpl;
import org.activiti.cloud.notifications.graphql.GrapqhQLApplication;
import org.activiti.cloud.notifications.graphql.config.EngineEvents;
import org.activiti.cloud.notifications.graphql.config.EngineEventsConfiguration;
import org.activiti.cloud.services.notifications.graphql.events.model.EngineEvent;
import org.activiti.cloud.services.test.containers.RabbitMQContainerApplicationInitializer;
import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { GrapqhQLApplication.class })
@ContextConfiguration(
    classes = { EngineEventsConfiguration.class },
    initializers = { RabbitMQContainerApplicationInitializer.class }
)
class EngineEventsFluxPublisherIT {

    @MockitoBean
    private BuildProperties buildProperties;

    @Autowired
    private Flux<Message<List<EngineEvent>>> engineEventsFlux;

    @Autowired
    private EngineEvents producerChannel;

    private static final Logger LOGGER = LoggerFactory.getLogger(EngineEventsFluxPublisherIT.class.getName());

    private static final CloudBPMNSignalReceivedEvent event1 = new CloudBPMNSignalReceivedEventImpl(
        "id",
        new Date().getTime(),
        new BPMNSignalImpl("elementId"),
        "processDefinitionId",
        "processInstanceId"
    ) {
        {
            setAppName("default-app");
            setServiceName("rb-my-app");
            setServiceFullName("serviceFullName");
            setServiceType("runtime-bundle");
            setServiceVersion("");
            setProcessDefinitionId("processDefinitionId");
            setProcessDefinitionKey("processDefinitionKey");
            setProcessDefinitionVersion(1);
            setBusinessKey("businessKey");
        }
    };

    @Test
    void shouldSubscribeAndCancelSubscribersWhenNoSubscriberErrors() {
        final var firstClientCount = new AtomicInteger();
        final var secondClientCount = new AtomicInteger();
        final var thirdClientCount = new AtomicInteger();

        final var firstClient = engineEventsFlux.subscribe(o ->
            LOGGER.info("First client count #{}", firstClientCount.incrementAndGet())
        );
        final var secondClient = engineEventsFlux.subscribe(o ->
            LOGGER.info("Second client count #{}", secondClientCount.incrementAndGet())
        );

        StepVerifier
            .create(engineEventsFlux)
            .expectSubscription()
            .then(sendEngineEvent(event1))
            .expectNextCount(1)
            .thenAwait(Duration.ofMillis(100))
            .then(firstClient::dispose)
            .then(sendEngineEvent(event1))
            .expectNextCount(1)
            .then(() ->
                engineEventsFlux.subscribe(o ->
                    LOGGER.info("Third client count #{}", thirdClientCount.incrementAndGet())
                )
            )
            .thenAwait(Duration.ofMillis(100))
            .then(secondClient::dispose)
            .then(sendEngineEvent(event1))
            .expectNextCount(1)
            .thenAwait(Duration.ofMillis(100))
            .thenCancel()
            .log()
            .verify(Duration.ofSeconds(10));

        await()
            .untilAsserted(() -> {
                assertThat(firstClientCount).hasValue(1);
                assertThat(secondClientCount).hasValue(2);
                assertThat(thirdClientCount).hasValue(1);
            });
    }

    @Test
    void shouldNotCancelWhenSubscriberThrowsException() {
        final var secondClientCount = new AtomicInteger();

        engineEventsFlux
            .log()
            .subscribe(
                new BaseSubscriber<>() {
                    @Override
                    protected void hookOnNext(Message<List<EngineEvent>> value) {
                        throw new IllegalStateException("I'm failing");
                    }

                    @Override
                    public void dispose() {
                        super.dispose();
                    }
                }
            );

        final var secondClient = engineEventsFlux
            .log()
            .subscribe(o -> LOGGER.info("Second client count: {}", secondClientCount.incrementAndGet()));

        StepVerifier
            .create(engineEventsFlux)
            .then(sendEngineEvent(event1))
            .then(sendEngineEvent(event1))
            .expectNextCount(2)
            .thenAwait(Duration.ofSeconds(1))
            .thenCancel()
            .log()
            .verify(Duration.ofSeconds(10));

        secondClient.dispose();

        await()
            .untilAsserted(() -> {
                assertThat(secondClientCount).hasValue(2);
            });
    }

    @Test
    void shouldNotCancelWhenSlowSubscriberCancel() throws InterruptedException {
        final var countDownLatch = new CountDownLatch(1);
        final AtomicInteger firstCount = new AtomicInteger();
        final AtomicInteger secondCount = new AtomicInteger();

        engineEventsFlux
            .log()
            .subscribe(
                new BaseSubscriber<>() {
                    @Override
                    protected void hookOnSubscribe(Subscription subscription) {
                        LOGGER.warn("I'm subscribed {}", subscription);

                        subscription.request(1);
                    }

                    @Override
                    protected void hookOnNext(Message<List<EngineEvent>> value) {
                        LOGGER.info("Received message count: {}", firstCount.incrementAndGet());

                        if (firstCount.get() > 256) {
                            LOGGER.warn("I'm not feeling good: {}", value);

                            throw new IllegalStateException("Throwing up!!!");
                        } else {
                            request(1);
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    }

                    @Override
                    protected void hookOnError(Throwable throwable) {
                        LOGGER.error("Error: ", throwable);

                        cancel();

                        countDownLatch.countDown();
                    }

                    @Override
                    public void dispose() {
                        LOGGER.warn("I'm disposed");
                        super.dispose();
                    }

                    @Override
                    protected void hookOnCancel() {
                        LOGGER.warn("I'm cancelled");
                    }
                }
            );

        final var secondClient = engineEventsFlux
            .log()
            .subscribe(o -> {
                LOGGER.info("Second client count: {}", secondCount.incrementAndGet());

                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

        StepVerifier
            .create(engineEventsFlux)
            .then(() -> IntStream.range(0, 300).forEach(i -> sendEngineEvent(event1).run()))
            .expectNextCount(300)
            .thenAwait(Duration.ofSeconds(1))
            .thenCancel()
            .log()
            .verify(Duration.ofSeconds(10));

        assertThat(countDownLatch.await(5, TimeUnit.SECONDS)).isTrue();

        assertThat(firstCount).hasValue(257);
        assertThat(secondCount).hasValue(300);

        secondClient.dispose();

        LOGGER.info("All events have been delivered.");
    }

    @Test
    void shouldNotCancelConsumerWithErrorHandler() {
        final AtomicInteger firstCount = new AtomicInteger();
        final AtomicInteger secondCount = new AtomicInteger();

        final var firstClient = engineEventsFlux
            .doOnNext(o -> {
                throw new IllegalStateException("I'm failing");
            })
            .doOnError(e -> LOGGER.info("I've failed, and I am done {}", firstCount.incrementAndGet(), e))
            .onErrorComplete()
            .subscribe();

        final var secondClient = engineEventsFlux
            .log()
            .subscribe(o -> LOGGER.info("Second client : {}", secondCount.incrementAndGet()));

        StepVerifier
            .create(engineEventsFlux)
            .then(sendEngineEvent(event1))
            .expectNextCount(1)
            .then(sendEngineEvent(event1))
            .expectNextCount(1)
            .thenAwait(Duration.ofSeconds(1))
            .thenCancel()
            .log()
            .verify(Duration.ofSeconds(10));

        firstClient.dispose();
        secondClient.dispose();

        await()
            .untilAsserted(() -> {
                assertThat(firstCount).hasValue(1);
                assertThat(secondCount).hasValue(2);
            });
    }

    private Runnable sendEngineEvent(CloudRuntimeEvent... events) {
        return () ->
            producerChannel
                .output()
                .send(
                    MessageBuilder.withPayload(Arrays.array(events)).setHeader("routingKey", "eventProducer").build()
                );
    }
}

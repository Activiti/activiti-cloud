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
package org.activiti.cloud.qa.story;

import static org.activiti.cloud.qa.helpers.ProcessDefinitionRegistry.processDefinitionKeyMatcher;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.serenitybdd.core.Serenity;
import net.thucydides.core.annotations.Steps;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.builders.MessagePayloadBuilder;
import org.activiti.api.process.model.payloads.ReceiveMessagePayload;
import org.activiti.api.process.model.payloads.StartMessagePayload;
import org.activiti.cloud.acc.core.steps.notifications.NotificationsSteps;
import org.activiti.cloud.acc.core.steps.query.ProcessQuerySteps;
import org.activiti.cloud.acc.core.steps.runtime.ProcessRuntimeBundleSteps;
import org.activiti.cloud.acc.shared.model.AuthToken;
import org.activiti.cloud.acc.shared.rest.TokenHolder;
import org.activiti.cloud.qa.helpers.ProcessDefinitionRegistry;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.reactivestreams.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ReplayProcessor;
import reactor.test.StepVerifier;
import reactor.test.StepVerifier.Step;

public class ProcessInstanceNotifications {

    @Steps
    private ProcessRuntimeBundleSteps processRuntimeBundleSteps;

    @Steps
    private ProcessQuerySteps processQuerySteps;

    @Steps
    private NotificationsSteps notificationsSteps;

    @Autowired
    private ObjectMapper objectMapper = new ObjectMapper();

    private AtomicReference<ProcessInstance> processInstanceRef;
    private AtomicReference<Subscription> subscriptionRef;

    private Step<String> stepVerifier;

    @When("notifications: services are started")
    public void checkServicesStatus() {
        processRuntimeBundleSteps.checkServicesHealth();
        processQuerySteps.checkServicesHealth();
        notificationsSteps.checkServicesHealth();
    }

    @Given("notifications: session variable called $variableName with value set to $variableValue")
    public void setSessionVariableTo(String variableName, String variableValue) {
        Serenity.setSessionVariable(variableName).to(variableValue);
    }

    @Given("notifications: generated random value for session variable called $variableName")
    public void generateUniqueBusinessId(String variableName) {
        Serenity.setSessionVariable(variableName).to(UUID.randomUUID().toString());
    }

    @Given("notifications: session timeout of $timeoutSeconds seconds")
    public void setSessionTimeoutSeconds(long timeoutSeconds) {
        Serenity.setSessionVariable("sessionTimeoutSeconds").to(timeoutSeconds);
    }

    @Given("notifications: subscription timeout of $timeoutSeconds seconds")
    public void setSubscriptionTimeoutSeconds(long timeoutSeconds) {
        Serenity.setSessionVariable("subscriptionTimeoutSeconds").to(timeoutSeconds);
    }

    @When("notifications: the user subscribes to $eventTypesString notifications")
    public void subscribeToEventTypesNotifications(String eventTypesString) throws IOException, InterruptedException {
        String businessKey = sessionVariableCalled("businessKey", String.class).orElse("*");
        String processDefinitionKey = sessionVariableCalled("process", String.class)
            .map(ProcessDefinitionRegistry::processDefinitionKeyMatcher)
            .orElse("*");

        String[] eventTypes = eventTypesString.split(",");

        ReplayProcessor<String> subscription = subscribe(eventTypes, businessKey, processDefinitionKey);

        stepVerifier = StepVerifier.create(subscription).expectSubscription();
    }

    @When(
        "notifications: the user subscribes to $eventTypesString notifications with businessKey value from session variable called $variableName"
    )
    public void subscribeToEventTypesNotificationsWithBusinessKeySessionVariable(
        String eventTypesString,
        String variableName
    ) throws IOException, InterruptedException {
        String businessKey = sessionVariableCalled(variableName, String.class).orElse(null);
        String processDefinitionKey = sessionVariableCalled("process", String.class)
            .map(ProcessDefinitionRegistry::processDefinitionKeyMatcher)
            .orElse("*");

        String[] eventTypes = eventTypesString.split(",");

        ReplayProcessor<String> subscription = subscribe(eventTypes, businessKey, processDefinitionKey);

        stepVerifier = StepVerifier.create(subscription).expectSubscription();
    }

    @When("notifications: the user starts a process $processName")
    public void startProcess(String processName) throws IOException, InterruptedException {
        String processDefinitionKey = processDefinitionKeyMatcher(processName);
        String businessKey = sessionVariableCalled("businessKey", String.class).orElse("businessKey");

        processInstanceRef =
            new AtomicReference<>(processRuntimeBundleSteps.startProcess(processDefinitionKey, true, businessKey));
    }

    @When(
        "notifications: the user sends a start message named $messageName with businessKey value from session variable called $variableName"
    )
    public void sendStartMessage(String messageName, String variableName) throws IOException, InterruptedException {
        processInstanceRef = new AtomicReference<>();
        String businessKey = sessionVariableCalled(variableName, String.class).orElse(null);

        StartMessagePayload payload = MessagePayloadBuilder.start(messageName).withBusinessKey(businessKey).build();

        processInstanceRef.set(processRuntimeBundleSteps.message(payload));
    }

    @Then("notifications: verify subscription started")
    public void verifySubscriptionStarted() {
        assertThat(subscriptionRef.get()).as("should start the subscription").isNotNull();
    }

    @Then("notifications: verify process instance started response")
    public void verifyProcessInstanceStarted() {
        assertThat(processInstanceRef.get()).as("should receive process instance in the response").isNotNull();
    }

    @Then(
        "notifications: the user sends a message named $messageName with correlationKey value of session variable called $variableName"
    )
    public void sendMessage(String messageName, String variableName) throws Exception {
        String variableValue = Serenity.sessionVariableCalled(variableName);

        ReceiveMessagePayload payload = MessagePayloadBuilder
            .receive(messageName)
            .withCorrelationKey(variableValue)
            .build();

        processRuntimeBundleSteps.message(payload);
    }

    @Then("notifications: verify the status of the process is completed")
    public void verifyProcessCompleted() throws Exception {
        assertThat(processInstanceRef.get()).isNotNull();

        try {
            processQuerySteps.checkProcessInstanceStatus(
                processInstanceRef.get().getId(),
                ProcessInstance.ProcessInstanceStatus.COMPLETED
            );
        } catch (Error cause) {
            cancelSubscription();

            throw cause;
        }
    }

    @Then("notifications: the user completes the subscription")
    public void completeSubscription() {
        assertThat(subscriptionRef.get()).isNotNull();

        cancelSubscription();
    }

    @Then("notifications: verify all expected notifications are received")
    public void verifyAllNotificationsAreReceived() {
        long sessionTimeout = sessionTimeoutSeconds();

        stepVerifier.expectComplete().verify(Duration.ofSeconds(sessionTimeout));
    }

    @Then(
        "notifications: the payload with $eventTypes notifications is expected with process definition key value $processDefinitionKey"
    )
    public void expectPayloadWithEventTypesNotification(String eventTypes, String processDefinitionKey)
        throws Exception {
        String messagePayload = messagePayload(eventTypes, processDefinitionKey);

        stepVerifier.expectNext(messagePayload);
    }

    @Then("notifications: the payload with $eventTypes notifications is expected")
    public void expectPayloadWithEventTypesNotification(String eventTypes) throws Exception {
        String processDefinitionKey = processInstanceRef.get().getProcessDefinitionKey();

        String messagePayload = messagePayload(eventTypes, processDefinitionKey);

        stepVerifier.expectNext(messagePayload);
    }

    private void cancelSubscription() {
        // signal to stop receiving notifications
        subscriptionRef.get().cancel();
    }

    private Consumer<Subscription> countDownLatchAction(
        CountDownLatch countDownLatch,
        AtomicReference<Subscription> subscriptionRef,
        Duration duration,
        Runnable action
    ) {
        return subscription -> {
            subscriptionRef.set(subscription);

            Mono
                .just(subscription)
                .delaySubscription(duration)
                .doOnError(error -> subscriptionRef.get().cancel())
                .doOnSubscribe(it -> {
                    action.run();
                })
                .subscribe(it -> {
                    countDownLatch.countDown();
                });
        };
    }

    private Long sessionTimeoutSeconds() {
        return sessionVariableCalled("sessionTimeoutSeconds", Long.class).orElse(Long.valueOf(18));
    }

    private Long subscriptionTimeoutSeconds() {
        return sessionVariableCalled("subscriptionTimeoutSeconds", Long.class).orElse(Long.valueOf(6));
    }

    private <T> Optional<T> sessionVariableCalled(String key, Class<T> clazz) {
        return Optional.ofNullable(Serenity.sessionVariableCalled(key));
    }

    @SuppressWarnings("serial")
    private ObjectMap engineEvent(String eventType, String processDefinitionKey) {
        return new ObjectMap() {
            {
                put("serviceName", notificationsSteps.getRuntimeBundleServiceName());
                put("processDefinitionKey", processDefinitionKey);
                put("eventType", eventType);
            }
        };
    }

    @SuppressWarnings("serial")
    private ObjectMap objectMapPayload(ObjectMap[] engineEvents) {
        return new ObjectMap() {
            {
                put(
                    "payload",
                    new ObjectMap() {
                        {
                            put(
                                "data",
                                new ObjectMap() {
                                    {
                                        put("engineEvents", engineEvents);
                                    }
                                }
                            );
                        }
                    }
                );
                put("id", "1");
                put("type", "data");
            }
        };
    }

    private String messagePayload(String eventTypes, String processDefinitionKey) throws JsonProcessingException {
        ObjectMap[] engineEvents = Stream
            .of(eventTypes.split(","))
            .map(eventType -> engineEvent(eventType, processDefinitionKey))
            .toArray(ObjectMap[]::new);

        Map<String, Object> objectMapPayload = objectMapPayload(engineEvents);

        return objectMapper.writeValueAsString(objectMapPayload);
    }

    private ReplayProcessor<String> subscribe(String[] eventTypes, String businessKey, String processDefinitionKey)
        throws InterruptedException {
        String serviceName = notificationsSteps.getRuntimeBundleServiceName();
        long subscriptionTimeoutSeconds = subscriptionTimeoutSeconds();
        long sessionTimeoutSeconds = sessionTimeoutSeconds();

        subscriptionRef = new AtomicReference<>();

        CountDownLatch countDownLatch = new CountDownLatch(1);
        AuthToken authToken = TokenHolder.getAuthToken();

        // TODO: add processDefinitionKey when signal events are fixed
        String query =
            "subscription($serviceName: String!, $eventTypes: [EngineEventType!], $businessKey: String!, $processDefinitionKey: String!) {" +
            "  engineEvents(serviceName: [$serviceName], eventType: $eventTypes, businessKey: [$businessKey], processDefinitionKey: [$processDefinitionKey]) {" +
            "    serviceName " +
            "    processDefinitionKey " +
            "    eventType " +
            "  }" +
            "}";

        Map<String, Object> variables = new ObjectMap() {
            {
                put("serviceName", serviceName);
                put("eventTypes", eventTypes);
                put("businessKey", businessKey);
                put("processDefinitionKey", processDefinitionKey);
            }
        };

        Consumer<Subscription> action = countDownLatchAction(
            countDownLatch,
            subscriptionRef,
            Duration.ofSeconds(subscriptionTimeoutSeconds),
            () -> {}
        );

        ReplayProcessor<String> data = notificationsSteps.subscribe(
            authToken.getAccess_token(),
            query,
            variables,
            action
        );

        assertThat(countDownLatch.await(sessionTimeoutSeconds, TimeUnit.SECONDS))
            .as("should subscribe to notifications")
            .isTrue();

        return data;
    }

    @SuppressWarnings("serial")
    class ObjectMap extends LinkedHashMap<String, Object> {}
}

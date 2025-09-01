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

import static java.util.Map.entry;
import static org.activiti.cloud.qa.helpers.ProcessDefinitionRegistry.processDefinitionKeyMatcher;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import feign.FeignException;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import net.serenitybdd.core.Serenity;
import net.thucydides.core.annotations.Steps;
import net.thucydides.core.steps.StepEventBus;
import org.activiti.api.process.model.IntegrationContext;
import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.events.BPMNActivityEvent;
import org.activiti.api.process.model.events.IntegrationEvent;
import org.activiti.cloud.acc.core.steps.audit.AuditSteps;
import org.activiti.cloud.acc.core.steps.query.ProcessQuerySteps;
import org.activiti.cloud.acc.core.steps.query.admin.ProcessQueryAdminSteps;
import org.activiti.cloud.acc.core.steps.runtime.ProcessRuntimeBundleSteps;
import org.activiti.cloud.acc.core.steps.runtime.admin.ServiceTasksAdminSteps;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.CloudIntegrationContext;
import org.activiti.cloud.api.process.model.CloudServiceTask;
import org.activiti.cloud.api.process.model.events.CloudIntegrationErrorReceivedEvent;
import org.activiti.cloud.api.process.model.events.CloudIntegrationEvent;
import org.activiti.cloud.services.rest.api.ReplayServiceTaskRequest;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.springframework.hateoas.PagedModel;

public class ProcessInstanceServiceTasks {

    @Steps
    private ProcessRuntimeBundleSteps processRuntimeBundleSteps;

    @Steps
    private ProcessQuerySteps processQuerySteps;

    @Steps
    private ProcessQueryAdminSteps processQueryAdminSteps;

    @Steps
    private AuditSteps auditSteps;

    @Steps
    private ServiceTasksAdminSteps serviceTasksAdminSteps;

    @When("services are started")
    public void checkServicesStatus() {
        processRuntimeBundleSteps.checkServicesHealth();
        processQuerySteps.checkServicesHealth();
        auditSteps.checkServicesHealth();
    }

    @When("the user starts a process with service tasks called $processName")
    public void startProcess(String processName) throws IOException, InterruptedException {
        ProcessInstance processInstance = processRuntimeBundleSteps.startProcess(
            processDefinitionKeyMatcher(processName),
            false
        );
        Serenity.setSessionVariable("processInstanceId").to(processInstance.getId());
    }

    @Then("the user deletes the process with service tasks")
    public void deleteCurrentProcessInstance() throws Exception {
        String processId = Serenity.sessionVariableCalled("processInstanceId");
        processRuntimeBundleSteps.deleteProcessInstance(processId);
    }

    @Then("the user can get list of service tasks for process instance")
    public void verifyServiceTaskListFromProcessInstance() {
        String processId = Serenity.sessionVariableCalled("processInstanceId");

        await()
            .untilAsserted(() -> {
                PagedModel<CloudServiceTask> tasks = processQueryAdminSteps.getServiceTasks(processId);

                assertThat(tasks.getContent()).isNotEmpty().extracting("activityType").containsOnly("serviceTask");
            });
    }

    @Then("the user can get service task by id")
    public void verifyGetServiceTaskById() {
        String processId = Serenity.sessionVariableCalled("processInstanceId");

        await()
            .untilAsserted(() -> {
                PagedModel<CloudServiceTask> tasks = processQueryAdminSteps.getServiceTasks(processId);

                assertThat(tasks.getContent()).hasSize(1);

                String serviceTaskId = tasks.getContent().iterator().next().getId();

                CloudServiceTask serviceTask = processQueryAdminSteps.getServiceTaskById(serviceTaskId);

                assertThat(serviceTask)
                    .isNotNull()
                    .extracting(CloudServiceTask::getActivityType)
                    .isEqualTo("serviceTask");
            });
    }

    @Then("the user can get service task integration context by service task id")
    public void verifyServiceTaskIntegrationContextById() {
        String processId = Serenity.sessionVariableCalled("processInstanceId");

        await()
            .untilAsserted(() -> {
                PagedModel<CloudServiceTask> tasks = processQueryAdminSteps.getServiceTasks(processId);

                assertThat(tasks.getContent()).hasSize(1);

                String serviceTaskId = tasks.getContent().iterator().next().getId();

                assertThatHasIntegrationContext(serviceTaskId);
                CloudIntegrationContext serviceTask = processQueryAdminSteps.getCloudIntegrationContext(serviceTaskId);

                assertThat(serviceTask)
                    .isNotNull()
                    .extracting(CloudIntegrationContext::getClientType, CloudIntegrationContext::getStatus)
                    .containsOnly(
                        "ServiceTask",
                        CloudIntegrationContext.IntegrationContextStatus.INTEGRATION_RESULT_RECEIVED
                    );
            });
    }

    private void assertThatHasIntegrationContext(String serviceTaskId) {
        FeignException thrown = catchThrowableOfType(
            () -> processQueryAdminSteps.getCloudIntegrationContext(serviceTaskId),
            FeignException.class
        );
        if (thrown != null) {
            //It's important to clear step failures after an Exception, otherwise,
            //the step will be marked to be skipped and any subsequent call to
            //processQueryAdminSteps will return mocks instead of calling the real endpoint.
            //Without clearing step failures the await block become useless.
            StepEventBus.getEventBus().clearStepFailures();
        }
        assertThat(thrown).isNull();
    }

    @When("the user can get list of service tasks with status of $status")
    @Then("the user can get list of service tasks with status of $status")
    public void verifyGetServiceTaskByStatus(String status) {
        String processId = Serenity.sessionVariableCalled("processInstanceId");

        await()
            .untilAsserted(() -> {
                PagedModel<CloudServiceTask> tasks = processQueryAdminSteps.getServiceTasksByStatus(processId, status);
                assertThat(tasks.getContent())
                    .isNotEmpty()
                    .extracting(CloudServiceTask::getActivityType, CloudServiceTask::getStatus)
                    .containsOnly(tuple("serviceTask", CloudServiceTask.BPMNActivityStatus.valueOf(status)));
            });
    }

    @Then("the user can get list of service tasks for process key $processDefinitionKey and status $status")
    public void verifyGetServiceTaskByQuery(String processDefinitionKey, String status) {
        Map<String, String> queryMap = Map.ofEntries(
            entry("processDefinitionKey", processDefinitionKey),
            entry("status", status)
        );

        ProcessDefinition processDefinition = processQuerySteps
            .getProcessDefinitions()
            .getContent()
            .stream()
            .filter(i -> i.getKey().equals(processDefinitionKey))
            .findFirst()
            .orElseThrow();
        await()
            .untilAsserted(() -> {
                PagedModel<CloudServiceTask> tasks = processQueryAdminSteps.getServiceTasksByQuery(queryMap);
                assertThat(tasks.getContent())
                    .isNotEmpty()
                    .extracting(
                        CloudServiceTask::getProcessDefinitionId,
                        CloudServiceTask::getProcessDefinitionKey,
                        CloudServiceTask::getActivityType,
                        CloudServiceTask::getStatus
                    )
                    .containsOnly(
                        tuple(
                            processDefinition.getId(),
                            processDefinitionKey,
                            "serviceTask",
                            CloudServiceTask.BPMNActivityStatus.valueOf(status)
                        )
                    );
            });
    }

    @Then("the process with service tasks is completed")
    public void verifyProcessCompleted() throws Exception {
        String processId = Serenity.sessionVariableCalled("processInstanceId");
        processQuerySteps.checkProcessInstanceStatus(processId, ProcessInstance.ProcessInstanceStatus.COMPLETED);
    }

    @Then("integration context events are emitted for the process")
    public void verifyIntegrationContextEventsForProcess() throws Exception {
        String processId = Serenity.sessionVariableCalled("processInstanceId");
        ProcessInstance processInstance = processQuerySteps.getProcessInstance(processId);

        await()
            .untilAsserted(() -> {
                Collection<CloudRuntimeEvent> events = auditSteps.getEventsByProcessInstanceId(processId);

                assertThat(events)
                    .filteredOn(CloudIntegrationEvent.class::isInstance)
                    .isNotEmpty()
                    .extracting(
                        CloudRuntimeEvent::getEventType,
                        CloudRuntimeEvent::getProcessDefinitionId,
                        CloudRuntimeEvent::getProcessInstanceId,
                        CloudRuntimeEvent::getProcessDefinitionKey,
                        CloudRuntimeEvent::getBusinessKey,
                        event -> integrationContext(event).getProcessDefinitionId(),
                        event -> integrationContext(event).getProcessInstanceId()
                    )
                    .containsOnly(
                        integrationEvent(
                            IntegrationEvent.IntegrationEvents.INTEGRATION_RESULT_RECEIVED,
                            processInstance
                        ),
                        integrationEvent(IntegrationEvent.IntegrationEvents.INTEGRATION_REQUESTED, processInstance)
                    );
            });
    }

    @Then("all integration context events are emitted for the process")
    public void verifyAllIntegrationContextEventsForProcess() throws Exception {
        String processId = Serenity.sessionVariableCalled("processInstanceId");
        ProcessInstance processInstance = processQuerySteps.getProcessInstance(processId);

        await()
            .untilAsserted(() -> {
                Collection<CloudRuntimeEvent> events = auditSteps.getEventsByProcessInstanceId(processId);

                assertThat(events)
                    .filteredOn(CloudIntegrationEvent.class::isInstance)
                    .isNotEmpty()
                    .extracting(
                        CloudRuntimeEvent::getEventType,
                        CloudRuntimeEvent::getProcessDefinitionId,
                        CloudRuntimeEvent::getProcessInstanceId,
                        CloudRuntimeEvent::getProcessDefinitionKey,
                        CloudRuntimeEvent::getBusinessKey,
                        event -> integrationContext(event).getProcessDefinitionId(),
                        event -> integrationContext(event).getProcessInstanceId()
                    )
                    .containsOnly(
                        integrationEvent(
                            IntegrationEvent.IntegrationEvents.INTEGRATION_RESULT_RECEIVED,
                            processInstance
                        ),
                        integrationEvent(IntegrationEvent.IntegrationEvents.INTEGRATION_REQUESTED, processInstance),
                        integrationEvent(
                            IntegrationEvent.IntegrationEvents.INTEGRATION_ERROR_RECEIVED,
                            processInstance
                        ),
                        integrationEvent(IntegrationEvent.IntegrationEvents.INTEGRATION_REQUESTED, processInstance)
                    );
            });
    }

    @Then("integration context error events are emitted for the process")
    public void verifyIntegrationContextErrorEventsForProcess() throws Exception {
        String processId = Serenity.sessionVariableCalled("processInstanceId");
        ProcessInstance processInstance = processQuerySteps.getProcessInstance(processId);

        await()
            .untilAsserted(() -> {
                Collection<CloudRuntimeEvent> events = auditSteps.getEventsByProcessInstanceId(processId);

                assertThat(events)
                    .filteredOn(CloudIntegrationEvent.class::isInstance)
                    .isNotEmpty()
                    .extracting(
                        CloudRuntimeEvent::getEventType,
                        CloudRuntimeEvent::getProcessDefinitionId,
                        CloudRuntimeEvent::getProcessInstanceId,
                        CloudRuntimeEvent::getProcessDefinitionKey,
                        CloudRuntimeEvent::getBusinessKey,
                        event -> integrationContext(event).getProcessDefinitionId(),
                        event -> integrationContext(event).getProcessInstanceId()
                    )
                    .containsOnly(
                        integrationEvent(
                            IntegrationEvent.IntegrationEvents.INTEGRATION_ERROR_RECEIVED,
                            processInstance
                        ),
                        integrationEvent(IntegrationEvent.IntegrationEvents.INTEGRATION_REQUESTED, processInstance)
                    );
            });
    }

    private IntegrationContext integrationContext(CloudRuntimeEvent<?, ?> event) {
        return CloudIntegrationEvent.class.cast(event).getEntity();
    }

    @Then("the user can replay service task execution")
    public void verifyReplayServiceTaskExecution() throws Exception {
        String processId = Serenity.sessionVariableCalled("processInstanceId");

        await()
            .untilAsserted(() -> {
                Collection<CloudRuntimeEvent> events = auditSteps.getEventsByProcessInstanceId(processId);

                Optional<CloudIntegrationErrorReceivedEvent> event = events
                    .stream()
                    .filter(CloudIntegrationErrorReceivedEvent.class::isInstance)
                    .map(CloudIntegrationErrorReceivedEvent.class::cast)
                    .findFirst();

                assertThat(event).isPresent();

                IntegrationContext integrationContext = event.get().getEntity();
                String executionId = integrationContext.getExecutionId();
                String clientId = integrationContext.getClientId();

                serviceTasksAdminSteps.replayServiceTask(executionId, new ReplayServiceTaskRequest(clientId));
            });
    }

    private org.assertj.core.groups.Tuple integrationEvent(
        IntegrationEvent.IntegrationEvents type,
        ProcessInstance processInstance
    ) {
        return tuple(
            type,
            processInstance.getProcessDefinitionId(),
            processInstance.getId(),
            processInstance.getProcessDefinitionKey(),
            processInstance.getBusinessKey(),
            processInstance.getProcessDefinitionId(),
            processInstance.getId()
        );
    }

    @Then("the generated ACTIVITY_COMPLETED events for activity $elementId have the expected count of $count")
    public void verifyEventActivityCompleted(String elementId, Integer count) {
        String processId = Serenity.sessionVariableCalled("processInstanceId");

        await()
            .untilAsserted(() -> {
                Collection<CloudRuntimeEvent> generatedEvents = auditSteps
                    .getEventsByProcessInstanceId(processId)
                    .stream()
                    .filter(cloudRuntimeEvent ->
                        cloudRuntimeEvent.getEventType().equals(BPMNActivityEvent.ActivityEvents.ACTIVITY_COMPLETED)
                    )
                    .filter(cloudRuntimeEvent ->
                        BPMNActivityEvent.class.cast(cloudRuntimeEvent).getEntity().getElementId().equals(elementId)
                    )
                    .collect(Collectors.toList());

                assertThat(generatedEvents).hasSize(count);
            });
    }
}

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
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.activiti.api.process.model.IntegrationContext;
import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.events.IntegrationEvent;
import org.activiti.cloud.acc.core.steps.audit.AuditSteps;
import org.activiti.cloud.acc.core.steps.query.ProcessQuerySteps;
import org.activiti.cloud.acc.core.steps.query.admin.ProcessQueryAdminSteps;
import org.activiti.cloud.acc.core.steps.runtime.ProcessRuntimeBundleSteps;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.CloudIntegrationContext;
import org.activiti.cloud.api.process.model.CloudServiceTask;
import org.activiti.cloud.api.process.model.events.CloudIntegrationEvent;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.springframework.hateoas.PagedModel;

import net.serenitybdd.core.Serenity;
import net.thucydides.core.annotations.Steps;

public class ProcessInstanceServiceTasks {

    @Steps
    private ProcessRuntimeBundleSteps processRuntimeBundleSteps;

    @Steps
    private ProcessQuerySteps processQuerySteps;

    @Steps
    private ProcessQueryAdminSteps processQueryAdminSteps;

    @Steps
    private AuditSteps auditSteps;

    private ProcessInstance processInstance;

    @When("services are started")
    public void checkServicesStatus() {
        processRuntimeBundleSteps.checkServicesHealth();
        processQuerySteps.checkServicesHealth();
        auditSteps.checkServicesHealth();
    }

    @When("the user starts a process with service tasks called $processName")
    public void startProcess(String processName) throws IOException, InterruptedException {
        processInstance = processRuntimeBundleSteps.startProcess(processDefinitionKeyMatcher(processName),false);
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

        await().untilAsserted(() -> {
            PagedModel<CloudServiceTask> tasks = processQueryAdminSteps.getServiceTasks(processId);

            assertThat(tasks.getContent())
                            .isNotEmpty()
                            .extracting("activityType")
                            .containsOnly("serviceTask");
        });
    }

    @Then("the user can get service task by id")
    public void verifyGetServiceTaskById() {

        String processId = Serenity.sessionVariableCalled("processInstanceId");

        await().untilAsserted(() -> {
            PagedModel<CloudServiceTask> tasks = processQueryAdminSteps.getServiceTasks(processId);

            assertThat(tasks.getContent()).hasSize(1);

            String serviceTaskId = tasks.getContent()
                                        .iterator()
                                        .next()
                                        .getId();

            CloudServiceTask serviceTask = processQueryAdminSteps.getServiceTaskById(serviceTaskId);

            assertThat(serviceTask).isNotNull()
                                   .extracting(CloudServiceTask::getActivityType)
                                   .isEqualTo("serviceTask");
        });
    }

    @Then("the user can get service task integration context by service task id")
    public void verifyServiceTaskIntegrationContextById() {

        String processId = Serenity.sessionVariableCalled("processInstanceId");

        await().untilAsserted(() -> {
            PagedModel<CloudServiceTask> tasks = processQueryAdminSteps.getServiceTasks(processId);

            assertThat(tasks.getContent()).hasSize(1);

            String serviceTaskId = tasks.getContent()
                                        .iterator()
                                        .next()
                                        .getId();

            waitForIntegrationContext(serviceTaskId);
            CloudIntegrationContext serviceTask = processQueryAdminSteps.getCloudIntegrationContext(serviceTaskId);

            assertThat(serviceTask).isNotNull()
                                   .extracting(CloudIntegrationContext::getClientType, CloudIntegrationContext::getStatus)
                                   .containsOnly("ServiceTask", CloudIntegrationContext.IntegrationContextStatus.INTEGRATION_RESULT_RECEIVED);
        });
    }

    private void waitForIntegrationContext(String serviceTaskId) {
        final Throwable throwable = catchThrowable(
            () -> processQueryAdminSteps.getCloudIntegrationContext(serviceTaskId));
        assertThat(throwable).isNull();
    }

    @Then("the user can get list of service tasks with status of $status")
    public void verifyGetServiceTaskByStatus(String status) {
        String processId = Serenity.sessionVariableCalled("processInstanceId");

        await().untilAsserted(() -> {
            PagedModel<CloudServiceTask> tasks = processQueryAdminSteps.getServiceTasksByStatus(processId,
                                                                                                 status);
            assertThat(tasks.getContent()).isNotEmpty()
                                          .extracting(CloudServiceTask::getActivityType, CloudServiceTask::getStatus)
                                          .containsOnly(tuple("serviceTask", CloudServiceTask.BPMNActivityStatus.valueOf(status)));
        });
    }

    @Then("the user can get list of service tasks for process key $processDefinitionKey and status $status")
    public void verifyGetServiceTaskByQuery(String processDefinitionKey,
                                            String status) {
        Map<String, String> queryMap = Map.ofEntries(entry("processDefinitionKey", processDefinitionKey),
                                                     entry("status", status));

        ProcessDefinition processDefinition = processQuerySteps.getProcessDefinitions()
                                                               .getContent()
                                                               .stream()
                                                               .filter(i -> i.getKey().equals(processDefinitionKey))
                                                               .findFirst()
                                                               .orElseThrow();
        await().untilAsserted(() -> {
            PagedModel<CloudServiceTask> tasks = processQueryAdminSteps.getServiceTasksByQuery(queryMap);
            assertThat(tasks.getContent()).isNotEmpty()
                                          .extracting(CloudServiceTask::getProcessDefinitionId,
                                                      CloudServiceTask::getProcessDefinitionKey,
                                                      CloudServiceTask::getActivityType,
                                                      CloudServiceTask::getStatus)
                                          .containsOnly(tuple(processDefinition.getId(),
                                                              processDefinitionKey,
                                                              "serviceTask",
                                                              CloudServiceTask.BPMNActivityStatus.valueOf(status)));
        });
    }

    @Then("the process with service tasks is completed")
    public void verifyProcessCompleted() throws Exception {
        String processId = Serenity.sessionVariableCalled("processInstanceId");
        processQuerySteps.checkProcessInstanceStatus(processId,
                ProcessInstance.ProcessInstanceStatus.COMPLETED);
    }

    @Then("integration context events are emitted for the process")
    public void verifyIntegrationContextEventsForProcess() throws Exception {

        String processId = Serenity.sessionVariableCalled("processInstanceId");

        await().untilAsserted(() -> {
            Collection<CloudRuntimeEvent> events = auditSteps.getEventsByProcessInstanceId(processId);

            assertThat(events)
                    .filteredOn(CloudIntegrationEvent.class::isInstance)
                    .isNotEmpty()
                    .extracting(CloudRuntimeEvent::getEventType,
                                CloudRuntimeEvent::getProcessDefinitionId,
                                CloudRuntimeEvent::getProcessInstanceId,
                                CloudRuntimeEvent::getProcessDefinitionKey,
                                CloudRuntimeEvent::getBusinessKey,
                                event -> integrationContext(event).getProcessDefinitionId(),
                                event -> integrationContext(event).getProcessInstanceId()
                    )
                    .containsOnly(
                                     tuple(IntegrationEvent.IntegrationEvents.INTEGRATION_RESULT_RECEIVED,
                                           processInstance.getProcessDefinitionId(),
                                           processInstance.getId(),
                                           processInstance.getProcessDefinitionKey(),
                                           processInstance.getBusinessKey(),
                                           processInstance.getProcessDefinitionId(),
                                           processInstance.getId()
                                     ),
                                     tuple(IntegrationEvent.IntegrationEvents.INTEGRATION_REQUESTED,
                                           processInstance.getProcessDefinitionId(),
                                           processInstance.getId(),
                                           processInstance.getProcessDefinitionKey(),
                                           processInstance.getBusinessKey(),
                                           processInstance.getProcessDefinitionId(),
                                           processInstance.getId()
                                     ));
        });
    }

    @Then("integration context error events are emitted for the process")
    public void verifyIntegrationContextErrorEventsForProcess() throws Exception {

        String processId = Serenity.sessionVariableCalled("processInstanceId");

        await().untilAsserted(() -> {
            Collection<CloudRuntimeEvent> events = auditSteps.getEventsByProcessInstanceId(processId);

            assertThat(events)
                    .filteredOn(CloudIntegrationEvent.class::isInstance)
                    .isNotEmpty()
                    .extracting(CloudRuntimeEvent::getEventType,
                                CloudRuntimeEvent::getProcessDefinitionId,
                                CloudRuntimeEvent::getProcessInstanceId,
                                CloudRuntimeEvent::getProcessDefinitionKey,
                                CloudRuntimeEvent::getBusinessKey,
                                event -> integrationContext(event).getProcessDefinitionId(),
                                event -> integrationContext(event).getProcessInstanceId()
                    )
                    .containsOnly(
                                     tuple(IntegrationEvent.IntegrationEvents.INTEGRATION_ERROR_RECEIVED,
                                           processInstance.getProcessDefinitionId(),
                                           processInstance.getId(),
                                           processInstance.getProcessDefinitionKey(),
                                           processInstance.getBusinessKey(),
                                           processInstance.getProcessDefinitionId(),
                                           processInstance.getId()
                                     ),
                                     tuple(IntegrationEvent.IntegrationEvents.INTEGRATION_REQUESTED,
                                           processInstance.getProcessDefinitionId(),
                                           processInstance.getId(),
                                           processInstance.getProcessDefinitionKey(),
                                           processInstance.getBusinessKey(),
                                           processInstance.getProcessDefinitionId(),
                                           processInstance.getId()
                                     ));
        });
    }

    private IntegrationContext integrationContext(CloudRuntimeEvent<?,?> event) {
        return CloudIntegrationEvent.class.cast(event).getEntity();
    }

}

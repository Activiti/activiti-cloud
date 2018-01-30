/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.qa.steps;

import java.io.IOException;
import java.net.URISyntaxException;

import org.activiti.cloud.qa.model.AuthToken;
import org.activiti.cloud.qa.model.Event;
import org.activiti.cloud.qa.model.EventType;
import org.activiti.cloud.qa.model.EventsResponse;
import org.activiti.cloud.qa.model.ProcessInstanceResponse;
import org.activiti.cloud.qa.model.Task;
import org.activiti.cloud.qa.model.TaskAction;
import org.activiti.cloud.qa.model.TasksResponse;
import org.activiti.cloud.qa.service.AuthenticationService;
import org.activiti.cloud.qa.service.EventService;
import org.activiti.cloud.qa.service.ProcessInstanceService;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;

import static org.assertj.core.api.Assertions.*;

public class ProcessInstanceTasks {

    private AuthToken authToken;
    private ProcessInstanceResponse processInstanceResponse;
    private Task currentTask;

    @Given("the user is authenticated")
    public void runningProcess() {
        try {
            authToken = AuthenticationService.authenticate();
            System.out.println(authToken.getAccess_token());
        } catch (IOException e) {
            e.printStackTrace();
        }
        assertThat(authToken).isNotNull();
        assertThat(authToken.getAccess_token()).isNotNull();
    }

    @When("the user starts a random process")
    public void startProcess() {
        try {
            processInstanceResponse = ProcessInstanceService.startProcess(authToken);
        } catch (IOException e) {
            e.printStackTrace();
        }
        assertThat(processInstanceResponse).isNotNull();

        TasksResponse tasksResponse = null;
        try {
            tasksResponse = ProcessInstanceService.getTaskByProcessInstanceId(processInstanceResponse.getId(),
                                                                              authToken);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        assertThat(tasksResponse).isNotNull();
        assertThat(tasksResponse.getEmbedded()).isNotNull();
        assertThat(tasksResponse.getEmbedded().getTasks()).isNotNull();
        currentTask = tasksResponse.getEmbedded().getTasks().get(0);
        assertThat(currentTask).isNotNull();
    }

    @When("the user claims a task")
    public void claimTask() {
        try {
            ProcessInstanceService.changeTaskStatus(currentTask.getId(),
                                                    "hruser",
                                                    TaskAction.CLAIM,
                                                    authToken);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @When("the user completes the task")
    public void completeTask() {
        try {
            ProcessInstanceService.changeTaskStatus(currentTask.getId(),
                                                    null,
                                                    TaskAction.COMPLETE,
                                                    authToken);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Then("the status of the process is changed to completed")
    public void verifyProcessStatus() {
        EventsResponse eventsResponse = null;
        try {
            eventsResponse = EventService.getEventsByProcessInstanceIdAndEventType(processInstanceResponse.getId(),
                                                                                   EventType.TASK_COMPLETED,
                                                                                   authToken);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        assertThat(eventsResponse).isNotNull();
        assertThat(eventsResponse.getEmbeddedEvents()).isNotNull();
        assertThat(eventsResponse.getEmbeddedEvents()).isNotNull();
        Event resultingEvent = eventsResponse.getEmbeddedEvents().getEvents().get(0);
        assertThat(resultingEvent).isNotNull();
        assertThat(resultingEvent.getProcessInstanceId()).isEqualTo(processInstanceResponse.getId());
        assertThat(resultingEvent.getEventType()).isEqualTo(EventType.TASK_COMPLETED);
        assertThat(resultingEvent.getTask().getId()).isEqualTo(currentTask.getId());
    }
}

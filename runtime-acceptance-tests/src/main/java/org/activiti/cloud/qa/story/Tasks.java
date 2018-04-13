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

package org.activiti.cloud.qa.story;

import net.thucydides.core.annotations.Steps;
import org.activiti.cloud.qa.model.QueryStatus;
import org.activiti.cloud.qa.model.Task;
import org.activiti.cloud.qa.steps.AuditSteps;
import org.activiti.cloud.qa.steps.QuerySteps;
import org.activiti.cloud.qa.steps.RuntimeBundleSteps;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;

import static org.assertj.core.api.Assertions.*;

public class Tasks {

    @Steps
    private RuntimeBundleSteps runtimeBundleSteps;

    @Steps
    private AuditSteps auditSteps;

    @Steps
    private QuerySteps querySteps;

    /**
     * standalone task
     */
    private Task newTask;

    @When("the user creates a standalone task")
    public void createTask() throws Exception {
        newTask = runtimeBundleSteps.createNewTask();
        assertThat(newTask).isNotNull();
    }

    @Then("the task is created and the status is assigned")
    public void taskIsCreatedAndAssigned() {
        final Task assignedTask = runtimeBundleSteps.getTaskById(newTask.getId());
        assertThat(assignedTask).isNotNull();
        assertThat(assignedTask.getStatus()).isNotEmpty().isEqualToIgnoringCase(QueryStatus.ASSIGNED.toString());

        auditSteps.checkTaskCreatedAndAssignedEvents(assignedTask.getId());
        querySteps.checkTaskStatus(assignedTask.getId(),
                                   QueryStatus.ASSIGNED);
    }
}

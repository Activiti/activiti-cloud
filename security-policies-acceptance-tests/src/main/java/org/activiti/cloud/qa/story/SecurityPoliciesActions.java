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

import feign.FeignException;
import net.thucydides.core.annotations.Steps;
import org.activiti.cloud.acc.core.steps.audit.AuditSteps;
import org.activiti.cloud.acc.core.steps.audit.admin.AuditAdminSteps;
import org.activiti.cloud.acc.core.steps.query.ProcessQuerySteps;
import org.activiti.cloud.acc.core.steps.query.TaskQuerySteps;
import org.activiti.cloud.acc.core.steps.query.admin.ProcessQueryAdminSteps;
import org.activiti.cloud.acc.core.steps.runtime.ProcessRuntimeBundleSteps;
import org.activiti.cloud.acc.core.steps.runtime.TaskRuntimeBundleSteps;
import org.activiti.cloud.acc.core.steps.runtime.admin.ProcessRuntimeAdminSteps;
import org.jbehave.core.annotations.Then;

import static org.activiti.cloud.acc.core.helper.Filters.checkEvents;
import static org.activiti.cloud.acc.core.helper.Filters.checkProcessInstances;
import static org.assertj.core.api.Assertions.assertThat;
import static org.activiti.cloud.qa.helpers.ProcessDefinitionRegistry.processDefinitionKeys;


public class SecurityPoliciesActions {

    @Steps
    private ProcessRuntimeBundleSteps processRuntimeBundleSteps;
    @Steps
    private TaskRuntimeBundleSteps taskRuntimeBundleSteps;
    @Steps
    private ProcessRuntimeAdminSteps processRuntimeAdminSteps;

    @Steps
    private ProcessQuerySteps processQuerySteps;
    @Steps
    private TaskQuerySteps taskQuerySteps;
    @Steps
    private ProcessQueryAdminSteps processQueryAdminSteps;

    @Steps
    private AuditSteps auditSteps;
    @Steps
    private AuditAdminSteps auditAdminSteps;

    @Then("the user cannot start the process with variables")
    public void startProcess() throws Exception {
        try {
            processRuntimeBundleSteps.startProcess(processDefinitionKeys.get("PROCESS_INSTANCE_WITH_VARIABLES"));
        }catch (FeignException exception){
            assertThat(exception.getMessage()).contains("Unable to find process definition for the given id:'ProcessWithVariables'");
        }
    }

    @Then("the user can get simple process instances")
    public void checkIfSimpleProcessInstancesArePresent(){
        assertThat(checkProcessInstances(processRuntimeBundleSteps.getAllProcessInstances(), processDefinitionKeys.get("SIMPLE_PROCESS_INSTANCE"))).isNotEmpty();
    }

    @Then("the user can get process with variables instances")
    public void checkIfProcessWithVariablesArePresent(){
        assertThat(checkProcessInstances(processRuntimeBundleSteps.getAllProcessInstances(), processDefinitionKeys.get("PROCESS_INSTANCE_WITH_VARIABLES"))).isNotEmpty();
    }

    @Then("the user can query simple process instances")
    public void checkIfSimpleProcessInstancesArePresentQuery(){
        assertThat(checkProcessInstances(processQuerySteps.getAllProcessInstances(), processDefinitionKeys.get("SIMPLE_PROCESS_INSTANCE"))).isNotEmpty();
    }

    @Then("the user can get events for simple process instances")
    public void checkIfEventsFromSimpleProcessesArePresent(){
        assertThat(checkEvents(auditSteps.getAllEvents(),processDefinitionKeys.get("SIMPLE_PROCESS_INSTANCE"))).isNotEmpty();
    }

    @Then("the user can get events for process with variables instances")
    public void checkIfEventsFromProcessesWithVariablesArePresent(){
        assertThat(checkEvents(auditSteps.getAllEvents(),processDefinitionKeys.get("PROCESS_INSTANCE_WITH_VARIABLES"))).isNotEmpty();
    }

    @Then("the user cannot get events for process with variables instances")
    public void checkIfEventsFromProcessesWithVariablesAreNotPresent(){
        assertThat(checkEvents(auditSteps.getAllEvents(),processDefinitionKeys.get("PROCESS_INSTANCE_WITH_VARIABLES"))).isEmpty();
    }

    @Then("the user cannot get process with variables instances")
    public void checkIfProcessWithVariablesAreNotPresent(){
        assertThat(checkProcessInstances(processRuntimeBundleSteps.getAllProcessInstances(), processDefinitionKeys.get("PROCESS_INSTANCE_WITH_VARIABLES"))).isEmpty();
    }

    @Then("the user cannot query process with variables instances")
    public void checkIfProcessWithVariablesAreNotPresentQuery(){
        assertThat(checkProcessInstances(processQuerySteps.getAllProcessInstances(),processDefinitionKeys.get("PROCESS_INSTANCE_WITH_VARIABLES"))).isEmpty();
    }

    @Then("the user can query process with variables instances")
    public void checkIfProcessWithVariablesArePresentQuery(){
        assertThat(checkProcessInstances(processQuerySteps.getAllProcessInstances(),processDefinitionKeys.get("PROCESS_INSTANCE_WITH_VARIABLES"))).isNotEmpty();
    }

    @Then("the user can get tasks")
    public void checkIfTaskArePresent(){
        assertThat(taskRuntimeBundleSteps.getAllTasks().getContent()).isNotNull();
    }

    @Then("the user can query tasks")
    public void checkIfTaskArePresentQuery(){
        assertThat(taskQuerySteps.getAllTasks().getContent()).isNotNull();
    }

}

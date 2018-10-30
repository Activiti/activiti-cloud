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
import org.activiti.cloud.qa.steps.AuditSteps;
import org.activiti.cloud.qa.steps.QuerySteps;
import org.activiti.cloud.qa.steps.RuntimeBundleSteps;
import org.jbehave.core.annotations.Then;

import static org.activiti.cloud.qa.helper.Filters.checkEvents;
import static org.activiti.cloud.qa.helper.Filters.checkProcessInstances;
import static org.activiti.cloud.qa.steps.RuntimeBundleSteps.PROCESS_INSTANCE_WITH_VARIABLES_DEFINITION_KEY;
import static org.activiti.cloud.qa.steps.RuntimeBundleSteps.SIMPLE_PROCESS_INSTANCE_DEFINITION_KEY;
import static org.assertj.core.api.Assertions.assertThat;


public class SecurityPoliciesActions {

    @Steps
    private AuditSteps auditSteps;

    @Steps
    private QuerySteps querySteps;

    @Steps
    private RuntimeBundleSteps runtimeBundleSteps;

    @Then("the user cannot start the process with variables")
    public void startProcess() throws Exception {
        try {
            runtimeBundleSteps.startProcess(PROCESS_INSTANCE_WITH_VARIABLES_DEFINITION_KEY);
        }catch (FeignException exception){
            assertThat(exception.getMessage()).contains("Unable to find process definition for the given id:'ProcessWithVariables'");
        }
    }

    @Then("the user can get simple process instances")
    public void checkIfSimpleProcessInstancesArePresent(){
        assertThat(checkProcessInstances(runtimeBundleSteps.getAllProcessInstances(), SIMPLE_PROCESS_INSTANCE_DEFINITION_KEY)).isNotEmpty();
    }

    @Then("the user can get process with variables instances")
    public void checkIfProcessWithVariablesArePresent(){
        assertThat(checkProcessInstances(runtimeBundleSteps.getAllProcessInstances(), PROCESS_INSTANCE_WITH_VARIABLES_DEFINITION_KEY)).isNotEmpty();
    }

    @Then("the user can query simple process instances")
    public void checkIfSimpleProcessInstancesArePresentQuery(){
        assertThat(checkProcessInstances(querySteps.getAllProcessInstances(), SIMPLE_PROCESS_INSTANCE_DEFINITION_KEY)).isNotEmpty();
    }

    @Then("the user can get events for simple process instances")
    public void checkIfEventsFromSimpleProcessesArePresent(){
        assertThat(checkEvents(auditSteps.getAllEvents(),SIMPLE_PROCESS_INSTANCE_DEFINITION_KEY)).isNotEmpty();
    }

    @Then("the user can get events for process with variables instances")
    public void checkIfEventsFromProcessesWithVariablesArePresent(){
        assertThat(checkEvents(auditSteps.getAllEvents(),PROCESS_INSTANCE_WITH_VARIABLES_DEFINITION_KEY)).isNotEmpty();
    }

    @Then("the user cannot get events for process with variables instances")
    public void checkIfEventsFromProcessesWithVariablesAreNotPresent(){
        assertThat(checkEvents(auditSteps.getAllEvents(),PROCESS_INSTANCE_WITH_VARIABLES_DEFINITION_KEY)).isEmpty();
    }

    @Then("the user cannot get process with variables instances")
    public void checkIfProcessWithVariablesAreNotPresent(){
        assertThat(checkProcessInstances(runtimeBundleSteps.getAllProcessInstances(), PROCESS_INSTANCE_WITH_VARIABLES_DEFINITION_KEY)).isEmpty();
    }

    @Then("the user cannot query process with variables instances")
    public void checkIfProcessWithVariablesAreNotPresentQuery(){
        assertThat(checkProcessInstances(querySteps.getAllProcessInstances(),PROCESS_INSTANCE_WITH_VARIABLES_DEFINITION_KEY)).isEmpty();
    }

    @Then("the user can query process with variables instances")
    public void checkIfProcessWithVariablesArePresentQuery(){
        assertThat(checkProcessInstances(querySteps.getAllProcessInstances(),PROCESS_INSTANCE_WITH_VARIABLES_DEFINITION_KEY)).isNotEmpty();
    }

    @Then("the user can get tasks")
    public void checkIfTaskArePresent(){
        assertThat(runtimeBundleSteps.getAllTasks().getContent()).isNotNull();
    }

    @Then("the user can query tasks")
    public void checkIfTaskArePresentQuery(){
        assertThat(querySteps.getAllTasks().getContent()).isNotNull();
    }

}

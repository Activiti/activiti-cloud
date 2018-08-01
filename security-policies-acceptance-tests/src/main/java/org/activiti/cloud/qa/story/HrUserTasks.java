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
import org.activiti.runtime.api.event.CloudRuntimeEvent;
import org.activiti.runtime.api.model.CloudProcessInstance;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.springframework.hateoas.PagedResources;
import java.util.Collection;
import java.util.Collections;
import static org.activiti.cloud.qa.steps.RuntimeBundleSteps.PROCESS_INSTANCE_WITH_VARIABLES_DEFINITION_KEY;
import static org.assertj.core.api.Assertions.assertThat;

public class HrUserTasks {

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
            assertThat(exception.getMessage()).contains("Operation not permitted for ProcessWithVariables");
        }
    }

    @Then("the user can get simple process instances")
    public void checkIfSimpleProcessInstancesArePresent(){
        assertThat(checkProcessInstances(runtimeBundleSteps.getAllProcessInstances(), "SimpleProcess")).isNotEmpty();
    }

    @Then("the user can query simple process instances")
    public void checkIfSimpleProcessInstancesArePresentQuery(){
        assertThat(checkProcessInstances(querySteps.getAllProcessInstances(), "SimpleProcess")).isNotEmpty();
    }

    @Then("the user can get events for simple process instances")
    public void checkIfEventsArePresent(){
        assertThat(checkEvents(auditSteps.getAllEvents(),"SimpleProcess")).isNotEmpty();
    }

    @Then("the user cannot get events for process with variables instances")
    public void checkIfEventsAreNotPresent(){
        assertThat(checkEvents(auditSteps.getAllEvents(),"ProcessWithVariables" )).isEmpty();
    }

    @Then("the user cannot get process with variables instances")
    public void checkIfProcessWithVariablesAreNotPresent(){
        assertThat(checkProcessInstances(runtimeBundleSteps.getAllProcessInstances(), "ProcessWithVariables")).isEmpty();
    }

    @Then("the user cannot query process with variables instances")
    public void checkIfProcessWithVariablesAreNotPresentQuery(){
        assertThat(checkProcessInstances(querySteps.getAllProcessInstances(),"ProcessWithVariables")).isEmpty();
    }

    @Then("the user can get tasks")
    public void checkIfTaskArePresent(){
        assertThat(runtimeBundleSteps.getAllTasks().getContent()).isNotNull();
    }

    @Then("the user can query tasks")
    public void checkIfTaskArePresentQuery(){
        assertThat(querySteps.getAllTasks().getContent()).isNotNull();
    }

    private Collection<CloudProcessInstance> checkProcessInstances(PagedResources<CloudProcessInstance> resource, String processKey){
        Collection<CloudProcessInstance> rawCollection = resource.getContent();
        Collection<CloudProcessInstance> filteredCollection = Collections.emptyList();
        for(CloudProcessInstance e : rawCollection){
            if(e.getProcessDefinitionKey().equals(processKey)){
                filteredCollection.add(e);
            }
        }
        return filteredCollection;
    }

    private Collection<CloudRuntimeEvent> checkEvents(PagedResources<CloudRuntimeEvent> resource, String processKey){
        Collection<CloudRuntimeEvent> rawCollection = resource.getContent();
        Collection<CloudRuntimeEvent> filteredCollection = Collections.emptyList();
        for(CloudRuntimeEvent e : rawCollection){
            Object element = e.getEntity();
            if( element instanceof CloudProcessInstance){
                if((((CloudProcessInstance) element).getProcessDefinitionKey().equals(processKey))) {
                    filteredCollection.add(e);
                }
            }
        }
        return filteredCollection;
    }
    
    @Given("the number is $argument")
    public void printing(String argument){
        System.out.println(argument);
    }
















}

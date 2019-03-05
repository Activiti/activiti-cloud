/*
 * Copyright 2019 Alfresco, Inc. and/or its affiliates.
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
import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.cloud.acc.core.steps.query.ProcessQuerySteps;
import org.jbehave.core.annotations.Then;

import static org.assertj.core.api.Assertions.assertThat;

public class ProcessDefinitionActions {

    @Steps
    private ProcessQuerySteps processQuerySteps;

    @Then("the user can get the process model for process with key $processDefinitionKey by passing its id")
    public void getProcessModel(String processDefinitionKey) {
        ProcessDefinition matchingProcessDefinition = processQuerySteps
                .getProcessDefinitions()
                .getContent()
                .stream()
                .filter(processDefinition -> processDefinition.getKey().equals(processDefinitionKey))
                .findFirst()
                .orElse(null);

        assertThat(matchingProcessDefinition)
                .as("No process definition found matching key " + processDefinitionKey)
                .isNotNull();

        String processModel = processQuerySteps.getProcessModel(matchingProcessDefinition.getId());
        assertThat(processModel).isNotEmpty();
        assertThat(processModel).contains("bpmn2:process id=\"" + processDefinitionKey +"\"");
    }

}

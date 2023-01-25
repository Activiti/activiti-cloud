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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Comparator;
import net.thucydides.core.annotations.Steps;
import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.cloud.acc.core.steps.query.ProcessQuerySteps;
import org.activiti.cloud.acc.core.steps.runtime.ProcessRuntimeBundleSteps;
import org.jbehave.core.annotations.Then;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.ResourceUtils;
import org.xmlunit.assertj3.XmlAssert;

public class ProcessDefinitionActions {

    public static final String TEST_OUTPUT_RESULT_PATH = "classpath:results/";

    @Steps
    private ProcessQuerySteps processQuerySteps;

    @Steps
    private ProcessRuntimeBundleSteps processRuntimeBundleSteps;

    @Then("the user can get the process model for process with key $processDefinitionKey by passing its id")
    public void getProcessModel(String processDefinitionKey) {
        ProcessDefinition matchingProcessDefinition = getProcessDefinition(processDefinitionKey);

        String processModel = processQuerySteps.getProcessModel(matchingProcessDefinition.getId());
        assertThat(processModel).isNotEmpty();
        assertThat(processModel).contains("bpmn2:process id=\"" + processDefinitionKey + "\"");
    }

    @Then("the process diagram image for process with key $processDefinitionKey is the same as $resultFileName file")
    public void getProcessDiagram(String processDefinitionKey, String resultFileName) throws FileNotFoundException {
        ProcessDefinition matchingProcessDefinition = getProcessDefinition(processDefinitionKey);

        String processDiagram = processRuntimeBundleSteps.getProcessDiagramByKey(matchingProcessDefinition.getId());
        File expectedResultFile = ResourceUtils.getFile(TEST_OUTPUT_RESULT_PATH + resultFileName);

        XmlAssert
            .assertThat(processDiagram)
            .and(expectedResultFile)
            .ignoreWhitespace()
            .withNodeFilter(node -> !"path".equals(node.getNodeName()))
            .withAttributeFilter(attr -> !"style".equals(attr.getName()))
            .areIdentical();
    }

    @NotNull
    private ProcessDefinition getProcessDefinition(String processDefinitionKey) {
        ProcessDefinition matchingProcessDefinition = processQuerySteps
            .getProcessDefinitions()
            .getContent()
            .stream()
            .filter(processDefinition -> processDefinition.getKey().equals(processDefinitionKey))
            .max(Comparator.comparing(processDefinition -> Integer.valueOf(processDefinition.getAppVersion())))
            .orElse(null);

        assertThat(matchingProcessDefinition)
            .as("No process definition found matching key " + processDefinitionKey)
            .isNotNull();
        return matchingProcessDefinition;
    }
}

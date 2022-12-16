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

import net.thucydides.core.annotations.Steps;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.cloud.acc.core.steps.query.ProcessQuerySteps;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.qa.steps.MultipleRuntimeBundleSteps;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;

public class ProcessInstanceTasks {

    @Steps
    private MultipleRuntimeBundleSteps runtimeBundleSteps;

    @Steps
    private ProcessQuerySteps processQuerySteps;

    private CloudProcessInstance processInstanceCatchSignal;
    private CloudProcessInstance processInstanceThrowSignal;

    @When(
        "the user starts signal catch process on primary runtime and starts signal throw process on secondary runtime"
    )
    public void startSignalCatchThrowProcessInstance() {
        processInstanceCatchSignal = runtimeBundleSteps.startProcess("SignalCatchEventProcess", true);
        assertThat(processInstanceCatchSignal).isNotNull();
        processInstanceThrowSignal = runtimeBundleSteps.startProcess("SignalThrowEventProcess", false);
        assertThat(processInstanceThrowSignal).isNotNull();
    }

    @Then("a signal was received and the signal catch and throw processes were completed")
    public void sheckSignalCatchThrowProcessInstances() throws Exception {
        processQuerySteps.checkProcessInstanceStatus(
            processInstanceCatchSignal.getId(),
            ProcessInstance.ProcessInstanceStatus.COMPLETED
        );
        processQuerySteps.checkProcessInstanceStatus(
            processInstanceThrowSignal.getId(),
            ProcessInstance.ProcessInstanceStatus.COMPLETED
        );
    }
}

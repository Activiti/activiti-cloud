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
import java.util.Collection;
import java.util.Map;

import net.thucydides.core.annotations.Step;
import org.activiti.cloud.qa.config.RuntimeTestsConfigurationProperties;
import org.activiti.cloud.qa.model.ProcessInstance;
import org.activiti.cloud.qa.model.Task;
import org.activiti.cloud.qa.rest.RuntimeFeignConfiguration;
import org.activiti.cloud.qa.rest.feign.EnableFeignContext;
import org.activiti.cloud.qa.service.RuntimeBundleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.*;

/**
 * Runtime bundle steps
 */
@EnableFeignContext
@ContextConfiguration(classes = {RuntimeTestsConfigurationProperties.class, RuntimeFeignConfiguration.class})
public class RuntimeBundleSteps {

    public static final String DEFAULT_PROCCES_INSTANCE_COMMAND_TYPE = "StartProcessInstanceCmd";

    public static final String DEFAULT_PROCCES_INSTANCE_KEY = "ProcessWithVariables";

    @Autowired
    private RuntimeBundleService runtimeBundleService;

    @Step
    public Map<String, Object> health() {
        return runtimeBundleService.health();
    }

    @Step
    public ProcessInstance startProcess() throws IOException {

        ProcessInstance processInstance = new ProcessInstance();
        processInstance.setCommandType(DEFAULT_PROCCES_INSTANCE_COMMAND_TYPE);
        processInstance.setProcessDefinitionKey(DEFAULT_PROCCES_INSTANCE_KEY);

        return runtimeBundleService
                .startProcess(processInstance);
    }

    @Step
    public Collection<Task> getTaskByProcessInstanceId(String processInstanceId) throws Exception {

        return runtimeBundleService
                .getProcessInstanceTasks(processInstanceId).getContent();
    }

    @Step
    public void assignTaskToUser(String id,
                                 String user) throws IOException {

        runtimeBundleService
                .assignTaskToUser(id,
                                  user);
    }

    @Step
    public void completeTask(String id) throws IOException {

        runtimeBundleService
                .completeTask(id);
    }

    @Step
    public void deleteProcessInstance(String id) throws IOException {
        runtimeBundleService.deleteProcess(id);
    }

    @Step
    public void checkProcessInstanceNotFound(String processInstanceId) {
        assertThatExceptionOfType(Exception.class).isThrownBy(
                () -> runtimeBundleService.getProcessInstance(processInstanceId)
        ).withMessageContaining("Unable to find process definition for the given id:");
    }

    @Step
    public void waitForMessagesToBeConsumed() throws InterruptedException {
        Thread.sleep(200);
    }
}

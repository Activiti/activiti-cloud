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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import net.thucydides.core.annotations.Step;
import org.activiti.cloud.qa.model.ProcessInstance;
import org.activiti.cloud.qa.model.Task;
import org.activiti.cloud.qa.model.commands.CreateTaskCmd;
import org.activiti.cloud.qa.rest.RuntimeDirtyContextHandler;
import org.activiti.cloud.qa.rest.feign.EnableRuntimeFeignContext;
import org.activiti.cloud.qa.service.RuntimeBundleDiagramService;
import org.activiti.cloud.qa.service.RuntimeBundleService;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resources;

import static org.assertj.core.api.Assertions.*;

/**
 * Runtime bundle steps
 */
@EnableRuntimeFeignContext
public class RuntimeBundleSteps {

    public static final String DEFAULT_PROCESS_INSTANCE_COMMAND_TYPE = "StartProcessInstanceCmd";

    public static final String DEFAULT_PROCESS_INSTANCE_KEY = "ProcessWithVariables";

    @Autowired
    private RuntimeDirtyContextHandler dirtyContextHandler;

    @Autowired
    private RuntimeBundleService runtimeBundleService;

    @Autowired
    private RuntimeBundleDiagramService runtimeBundleDiagramService;

    @Step
    public void checkServicesHealth() {
        assertThat(runtimeBundleService.isServiceUp()).isTrue();
    }

    @Step
    public Map<String, Object> health() {
        return runtimeBundleService.health();
    }

    @Step
    public ProcessInstance startProcess() {
        return this.startProcess(DEFAULT_PROCESS_INSTANCE_KEY);
    }

    @Step
    public ProcessInstance startProcess(String process) {

        ProcessInstance processInstance = new ProcessInstance();
        processInstance.setCommandType(DEFAULT_PROCESS_INSTANCE_COMMAND_TYPE);
        processInstance.setProcessDefinitionKey(process);

        return dirtyContextHandler.dirty(runtimeBundleService.startProcess(processInstance));
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
        Thread.sleep(220);
    }

    @Step
    public Task createNewTask() {
        return dirtyContextHandler.dirty(
                runtimeBundleService.createNewTask(new CreateTaskCmd("new-task",
                                                                     "task-description",
                                                                     "CreateTaskCmd")));
    }

    public Task createSubtask(String parentTaskId) {
        return runtimeBundleService.createSubtask(parentTaskId,
                                                  new CreateTaskCmd("subtask",
                                                                    "subtask-description",
                                                                    "CreateTaskCmd"));
    }

    public Resources getSubtasks(String parentTaskId) {
        return runtimeBundleService.getSubtasks(parentTaskId);
    }

    @Step
    public Task getTaskById(String id) {
        return runtimeBundleService.getTaskById(id);
    }

    @Step
    public String openProcessInstanceDiagram(String id) {
        return runtimeBundleDiagramService.getProcessDiagram(id);
    }

    @Step
    public void checkProcessInstanceDiagram(String diagram) throws Exception {
        assertThat(diagram).isNotEmpty();
        assertThat(svgToPng(diagram.getBytes())).isNotEmpty();
    }

    @Step
    public void checkProcessInstanceNoDiagram(String diagram) {
        assertThat(diagram).isEmpty();
    }

    @Step
    public void suspendProcessInstance(String processInstanceId) {
        runtimeBundleService.suspendProcess(processInstanceId);
    }

    @Step
    public void activateProcessInstance(String processInstanceId) {
        runtimeBundleService.activateProcess(processInstanceId);
    }

    private byte[] svgToPng(byte[] streamBytes)
            throws TranscoderException, IOException {
        try (ByteArrayInputStream input = new ByteArrayInputStream(streamBytes);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            new PNGTranscoder().transcode(new TranscoderInput(input),
                                          new TranscoderOutput(output));
            output.flush();
            return output.toByteArray();
        }
    }

    @Step
    public void deleteTask(String taskId) {
        runtimeBundleService.deleteTask(taskId);
    }

    @Step
    public void checkTaskNotFound(String taskId) {
        assertThatExceptionOfType(Exception.class).isThrownBy(
                () -> runtimeBundleService.getTaskById(taskId)
        ).withMessageContaining("Unable to find task");
    }
}

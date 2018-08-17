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
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.task.model.builders.TaskPayloadBuilder;
import org.activiti.api.task.model.payloads.CreateTaskPayload;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.api.task.model.CloudTask;
import org.activiti.cloud.qa.rest.RuntimeDirtyContextHandler;
import org.activiti.cloud.qa.rest.feign.EnableRuntimeFeignContext;
import org.activiti.cloud.qa.service.RuntimeBundleDiagramService;
import org.activiti.cloud.qa.service.RuntimeBundleService;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Runtime bundle steps
 */
@EnableRuntimeFeignContext
public class RuntimeBundleSteps {

    public static final String DEFAULT_PROCESS_INSTANCE_COMMAND_TYPE = "START_PROCESS";

    public static final String SIMPLE_PROCESS_INSTANCE_DEFINITION_KEY = "SimpleProcess";

    public static final String CONNECTOR_PROCESS_INSTANCE_DEFINITION_KEY = "ConnectorProcess";

    public static final String PROCESS_INSTANCE_WITH_VARIABLES_DEFINITION_KEY = "ProcessWithVariables";

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
    public CloudProcessInstance startProcess() {
        return this.startProcess(SIMPLE_PROCESS_INSTANCE_DEFINITION_KEY);
    }

    @Step
    public CloudProcessInstance startProcess(String process) {

        return dirtyContextHandler.dirty(runtimeBundleService.startProcess(ProcessPayloadBuilder
                                                                                   .start()
                                                                                   .withProcessDefinitionKey(process)
                                                                                   .build()));
    }

    @Step
    public Collection<CloudTask> getTaskByProcessInstanceId(String processInstanceId) {
        return runtimeBundleService
                .getProcessInstanceTasks(processInstanceId).getContent();
    }

    @Step
    public void assignTaskToUser(String id,
                                 String user) {

        runtimeBundleService
                .assignTaskToUser(id,
                                  user);
    }

    @Step
    public void completeTask(String id) {

        runtimeBundleService
                .completeTask(id);
    }

    @Step
    public void deleteProcessInstance(String id) {
        runtimeBundleService.deleteProcess(id);
    }

    @Step
    public void checkProcessInstanceNotFound(String processInstanceId) {
        assertThatExceptionOfType(Exception.class).isThrownBy(
                () -> runtimeBundleService.getProcessInstance(processInstanceId)
        ).withMessageContaining("Unable to find process instance for the given id:");
    }

    @Step
    public CloudTask createNewTask() {

        CreateTaskPayload createTask = TaskPayloadBuilder
                .create()
                .withName("new-task")
                .withDescription("task-description")
                .withAssignee("testuser")
                .build();
        return dirtyContextHandler.dirty(
                runtimeBundleService.createNewTask(createTask));
    }

    public CloudTask createSubtask(String parentTaskId) {
        CreateTaskPayload subTask = TaskPayloadBuilder
                .create()
                .withName("subtask")
                .withDescription("subtask-description")
                .withAssignee("testuser")
                .build();
        return runtimeBundleService.createSubtask(parentTaskId,
                                                  subTask);
    }

    public Resources<CloudTask> getSubtasks(String parentTaskId) {
        return runtimeBundleService.getSubtasks(parentTaskId);
    }

    @Step
    public CloudTask getTaskById(String id) {
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

    @Step
    public PagedResources<CloudTask> getAllTasks(){
        return runtimeBundleService.getAllTasks();
    }

    @Step
    public PagedResources<CloudProcessInstance> getAllProcessInstances(){
        return runtimeBundleService.getAllProcessInstances();
    }

    @Step
    public PagedResources<CloudProcessInstance> getAllProcessInstancesAdmin(){
        return runtimeBundleService.getAllProcessInstancesAdmin();
    }
}

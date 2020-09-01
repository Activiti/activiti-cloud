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
package org.activiti.cloud.acc.core.steps.runtime;

import static org.activiti.cloud.acc.core.assertions.RestErrorAssert.assertThatRestNotFoundErrorIsThrownBy;
import static org.activiti.cloud.acc.core.helper.SvgToPng.svgToPng;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.model.builders.StartProcessPayloadBuilder;
import org.activiti.api.process.model.payloads.ReceiveMessagePayload;
import org.activiti.api.process.model.payloads.StartMessagePayload;
import org.activiti.api.process.model.payloads.StartProcessPayload;
import org.activiti.api.runtime.model.impl.ProcessVariableValue;
import org.activiti.cloud.acc.core.rest.RuntimeDirtyContextHandler;
import org.activiti.cloud.acc.core.rest.feign.EnableRuntimeFeignContext;
import org.activiti.cloud.acc.core.services.runtime.ProcessRuntimeService;
import org.activiti.cloud.acc.core.services.runtime.diagram.ProcessRuntimeDiagramService;
import org.activiti.cloud.acc.shared.service.BaseService;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.api.task.model.CloudTask;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.hateoas.PagedModel;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.thucydides.core.annotations.Step;

@EnableRuntimeFeignContext
public class ProcessRuntimeBundleSteps {

    @Autowired
    private RuntimeDirtyContextHandler dirtyContextHandler;

    @Autowired
    private ProcessRuntimeService processRuntimeService;

    @Autowired
    private ProcessRuntimeDiagramService processRuntimeDiagramService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    @Qualifier("runtimeBundleBaseService")
    private BaseService baseService;

    @Step
    public void checkServicesHealth() {
        assertThat(baseService.isServiceUp()).isTrue();
    }

    @Step
    public CloudProcessInstance startProcess(StartProcessPayload payload) {
        return dirtyContextHandler.dirty(processRuntimeService.startProcess(payload));
    }

    @Step
    public CloudProcessInstance startProcess(String process, boolean variables, String businessKey) throws IOException {
        StartProcessPayloadBuilder payload = ProcessPayloadBuilder
                .start()
                .withProcessDefinitionKey(process)
                .withName("processInstanceName")
                .withBusinessKey(businessKey);


        if(variables){
            payload.withVariable("test_variable_name", "test-variable-value");
            payload.withVariable("test_bigdecimal_variable_name", wrap(BigDecimal.valueOf(1234567890L, 2)));
            payload.withVariable("test_date_variable_name", wrap(Date.from(Instant.EPOCH)));
            payload.withVariable("test_long_variable_name", wrap(1234567890L));
            payload.withVariable("test_int_variable_name", 7);
            payload.withVariable("test_bool_variable_name", true);
            payload.withVariable("test_json_variable_name",objectMapper.readTree("{ \"test-json-variable-element1\":\"test-json-variable-value1\"}"));
            payload.withVariable("test_long_json_variable_name",objectMapper.readTree("{ \"verylongjson\":\""+ StringUtils.repeat("a", 4000)+"\"}"));
        }

        return startProcess(payload.build());
    }

    protected Map<String, String> wrap(BigDecimal value) {
        return ProcessVariableValue.builder()
                                   .type("bigdecimal")
                                   .value(value.toString())
                                   .build()
                                   .toMap();
    }

    protected Map<String, String> wrap(Long value) {
        return ProcessVariableValue.builder()
                                   .type("long")
                                   .value(value.toString())
                                   .build()
                                   .toMap();
    }

    protected Map<String, String> wrap(Date value) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS'Z'");

        return ProcessVariableValue.builder()
                                   .type("date")
                                   .value(formatter.format(value))
                                   .build()
                                   .toMap();
    }

    @Step
    public CloudProcessInstance startProcess(String process, boolean variables) throws IOException {
        return startProcess(process, variables, "businessKey");
    }

    @Step
    public CloudProcessInstance startProcess(String process) {
        return startProcess(ProcessPayloadBuilder.start()
                                                 .withProcessDefinitionKey(process)
                                                 .withName("process-instance-name")
                                                 .build());
    }

    @Step
    public CloudProcessInstance startProcessWithVariables(String process, Map<String,Object> variables) {
        return startProcess(ProcessPayloadBuilder.start()
                                                 .withProcessDefinitionKey(process)
                                                 .withVariables(variables)
                                                 .build());
    }

    @Step
    public void deleteProcessInstance(String id) {
        processRuntimeService.deleteProcess(id);
    }

    @Step
    public void checkProcessInstanceNotFound(String processInstanceId) {
        assertThatRestNotFoundErrorIsThrownBy(
                () -> processRuntimeService.getProcessInstance(processInstanceId)
        ).withMessageContaining("Unable to find process instance for the given id:'" + processInstanceId + "'");
    }

    @Step
    public String openProcessInstanceDiagram(String id) {
        return processRuntimeDiagramService.getProcessInstanceModel(id);
    }

    @Step
    public void checkProcessInstanceDiagram(String diagram) throws Exception {
        assertThat(diagram).isNotEmpty();
        assertThat(svgToPng(diagram.getBytes())).isNotEmpty();
    }

    @Step
    public void checkProcessInstanceNoDiagram(String diagram) {
        assertThat(diagram).isNullOrEmpty();
    }

    @Step
    public void suspendProcessInstance(String processInstanceId) {
        processRuntimeService.suspendProcess(processInstanceId);
    }

    @Step
    public void resumeProcessInstance(String processInstanceId) {
        processRuntimeService.resumeProcess(processInstanceId);
    }

    @Step
    public PagedModel<CloudProcessInstance> getAllProcessInstances(){
        return processRuntimeService.getAllProcessInstances();
    }

    @Step
    public CloudProcessInstance getProcessInstanceById(String id){
        return processRuntimeService.getProcessInstance(id);
    }

    @Step
    public PagedModel<CloudProcessInstance> getSubProcesses(String parentId){
        return processRuntimeService.getSubProcesses(parentId);
    }

    @Step
    public PagedModel<ProcessDefinition> getProcessDefinitions(){
        return processRuntimeService.getProcessDefinitions();
    }

    @Step
    public ProcessDefinition getProcessDefinitionByKey(String key){
        return processRuntimeService.getProcessDefinitionByKey(key);
    }

    @Step
    public Collection<CloudTask> getTaskByProcessInstanceId(String processInstanceId) {
        return processRuntimeService
                .getProcessInstanceTasks(processInstanceId).getContent();
    }

    @Step
    public CloudProcessInstance startProcessWithProcessInstanceName(String process,
                                                                    String processName) {
        return dirtyContextHandler.dirty(processRuntimeService.startProcess(ProcessPayloadBuilder
                .start()
                .withName(processName)
                .withProcessDefinitionKey(process)
                .build()));
    }

    @Step
    public void checkProcessInstanceName(String processInstanceId,
                                         String processInstanceName) {
        assertThat(processRuntimeService.getProcessInstance(processInstanceId).getName()).isEqualTo(processInstanceName);
    }


    @Step
    public CloudProcessInstance setProcessName(String processInstanceId, String processInstanceName){
        return processRuntimeService.updateProcess(
                processInstanceId,
                ProcessPayloadBuilder.update().withName(processInstanceName).build());

    }

    @Step
    public CloudProcessInstance message(StartMessagePayload payload) throws IOException {
        return dirtyContextHandler.dirty(processRuntimeService.message(payload));
    }

    @Step
    public void message(ReceiveMessagePayload payload) throws IOException {
        processRuntimeService.message(payload);
    }

}

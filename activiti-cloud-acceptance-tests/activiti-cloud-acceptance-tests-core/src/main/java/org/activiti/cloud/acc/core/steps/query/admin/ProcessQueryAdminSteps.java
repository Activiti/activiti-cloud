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
package org.activiti.cloud.acc.core.steps.query.admin;

import static org.activiti.cloud.acc.core.helper.SvgToPng.svgToPng;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import net.thucydides.core.annotations.Step;
import org.activiti.cloud.acc.core.rest.feign.EnableRuntimeFeignContext;
import org.activiti.cloud.acc.core.services.query.admin.ProcessModelQueryAdminService;
import org.activiti.cloud.acc.core.services.query.admin.ProcessQueryAdminDiagramService;
import org.activiti.cloud.acc.core.services.query.admin.ProcessQueryAdminService;
import org.activiti.cloud.acc.shared.service.BaseService;
import org.activiti.cloud.api.process.model.CloudIntegrationContext;
import org.activiti.cloud.api.process.model.CloudProcessDefinition;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.api.process.model.CloudServiceTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;

@EnableRuntimeFeignContext
public class ProcessQueryAdminSteps {

    @Autowired
    private ProcessQueryAdminService processQueryAdminService;

    @Autowired
    private ProcessModelQueryAdminService processModelQueryAdminService;

    @Autowired
    private ProcessQueryAdminDiagramService processQueryAdminDiagramService;

    @Autowired
    @Qualifier("queryBaseService")
    private BaseService baseService;

    @Step
    public void checkServicesHealth() {
        assertThat(baseService.isServiceUp()).isTrue();
    }

    @Step
    public PagedModel<CloudProcessInstance> getAllProcessInstancesAdmin() {
        return processQueryAdminService.getProcessInstances();
    }

    @Step
    public PagedModel<CloudProcessDefinition> getAllProcessDefinitions() {
        return processQueryAdminService.getProcessDefinitions();
    }

    @Step
    public String getProcessModel(String processDefinitionId) {
        return processModelQueryAdminService.getProcessModel(processDefinitionId);
    }

    @Step
    public PagedModel<CloudProcessDefinition> getProcessDefinitions() {
        return processQueryAdminService.getProcessDefinitions();
    }

    @Step
    public CollectionModel<EntityModel<CloudProcessInstance>> deleteProcessInstances() {
        return processQueryAdminService.deleteProcessInstances();
    }

    @Step
    public String getProcessInstanceDiagram(String id) {
        return processQueryAdminDiagramService.getProcessInstanceDiagram(id);
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
    public PagedModel<CloudServiceTask> getServiceTasks() {
        return processQueryAdminService.getServiceTasks();
    }

    @Step
    public PagedModel<CloudServiceTask> getServiceTasksByQuery(Map<String, String> queryMap) {
        return processQueryAdminService.getServiceTasks(queryMap);
    }

    @Step
    public CloudServiceTask getServiceTaskById(String serviceTaskId) {
        return processQueryAdminService.getServiceTaskById(serviceTaskId);
    }

    @Step
    public PagedModel<CloudServiceTask> getServiceTasks(String processInstanceId) {
        return processQueryAdminService.getServiceTasks(processInstanceId);
    }

    @Step
    public PagedModel<CloudServiceTask> getServiceTasksByStatus(String processInstanceId, String status) {
        return processQueryAdminService.getServiceTasksByStatus(processInstanceId, status);
    }

    @Step
    public CloudIntegrationContext getCloudIntegrationContext(String serviceTaskId) {
        return processQueryAdminService.getCloudIntegrationContext(serviceTaskId);
    }

    @Step
    public PagedModel<CloudProcessInstance> getProcessInstancesByProcessDefinitionKey(String processDefinitionKey) {
        return this.processQueryAdminService.getProcessInstancesByProcessDefinitionKey(processDefinitionKey);
    }
}

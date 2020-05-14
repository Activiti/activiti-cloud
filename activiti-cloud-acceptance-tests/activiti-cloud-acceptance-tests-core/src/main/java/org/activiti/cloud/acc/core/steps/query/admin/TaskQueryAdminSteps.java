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

import static org.assertj.core.api.Assertions.assertThat;

import net.thucydides.core.annotations.Step;
import org.activiti.cloud.acc.core.rest.feign.EnableRuntimeFeignContext;
import org.activiti.cloud.acc.core.services.query.admin.TaskQueryAdminService;
import org.activiti.cloud.api.task.model.CloudTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;

@EnableRuntimeFeignContext
public class TaskQueryAdminSteps {

    @Autowired
    private TaskQueryAdminService taskQueryAdminService;

    @Step
    public void checkServicesHealth() {
        assertThat(taskQueryAdminService.isServiceUp()).isTrue();
    }

 
    @Step
    public PagedResources<CloudTask> getAllTasks(){
        return taskQueryAdminService.getTasks();
    }

    @Step
    public CloudTask getTaskById(String id){
        return taskQueryAdminService.getTask(id);
    }

    
    @Step
    public PagedResources<CloudTask> getRootTasksByProcessInstance(String processInstanceId){
        return taskQueryAdminService.getRootTasksByProcessInstance(processInstanceId);
    }
    
    @Step
    public PagedResources<CloudTask> getStandaloneTasks(){
        return taskQueryAdminService.getStandaloneTasks();
    }

    @Step
    public PagedResources<CloudTask> getNonStandaloneTasks(){
        return taskQueryAdminService.getNonStandaloneTasks();
    }

    @Step
    public Resources<Resource<CloudTask>> deleteTasks(){
        return taskQueryAdminService.deleteTasks();
    }
    
}

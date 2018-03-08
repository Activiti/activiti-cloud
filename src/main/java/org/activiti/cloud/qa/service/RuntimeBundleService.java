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

package org.activiti.cloud.qa.service;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.activiti.cloud.qa.model.ProcessInstance;
import org.activiti.cloud.qa.model.Task;
import org.springframework.hateoas.PagedResources;

/**
 * Runtime Bundle service
 */
public interface RuntimeBundleService {

    @RequestLine("POST /v1/process-instances")
    @Headers("Content-Type: application/json")
    ProcessInstance startProcess(ProcessInstance processInstance);

    @RequestLine("GET /v1/process-instances/{id}/tasks")
    @Headers({
            "Content-Type: application/json",
            "Accept: application/hal+json;charset=UTF-8"
    })
    PagedResources<Task> getProcessInstanceTasks(@Param("id") String id);

    @RequestLine("POST /v1/tasks/{id}/claim")
    @Headers("Content-Type: application/x-www-form-urlencoded")
    void assignTaskToUser(@Param("id") String id,
                          @Param("assignee") String user);

    @RequestLine("POST /v1/tasks/{id}/complete")
    void completeTask(@Param("id") String id);
}

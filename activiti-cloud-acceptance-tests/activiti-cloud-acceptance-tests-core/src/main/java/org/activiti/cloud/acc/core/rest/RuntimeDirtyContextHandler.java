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
package org.activiti.cloud.acc.core.rest;

import org.activiti.cloud.acc.core.config.RuntimeTestsConfigurationProperties;
import org.activiti.cloud.acc.shared.rest.DirtyContextHandler;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.api.task.model.CloudTask;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * DirtyContextHandler for runtime bundle
 */
public class RuntimeDirtyContextHandler {

    private final String TASKS_PATH = "/v1/tasks/";
    private final String PROCESS_INSTANCES_PATH = "/v1/process-instances/";

    @Autowired
    private DirtyContextHandler dirtyContextHandler;

    @Autowired
    private RuntimeTestsConfigurationProperties configurationProperties;

    /**
     * Set a process instance as dirty.
     * @param processInstance the process instance
     * @return the dirty process instance
     */
    public CloudProcessInstance dirty(final CloudProcessInstance processInstance) {
        dirtyContextHandler.dirty(
            configurationProperties.getRuntimeBundleUrl() + PROCESS_INSTANCES_PATH + processInstance.getId()
        );
        return processInstance;
    }

    /**
     * Set a task as dirty.
     * @param task the task
     * @return the dirty task
     */
    public CloudTask dirty(final CloudTask task) {
        dirtyContextHandler.dirty(configurationProperties.getRuntimeBundleUrl() + TASKS_PATH + task.getId());
        return task;
    }
}

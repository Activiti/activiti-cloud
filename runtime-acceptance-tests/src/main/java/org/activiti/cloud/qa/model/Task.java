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

package org.activiti.cloud.qa.model;

public class Task extends ActivitiEntityMetadata {

    private String id;

    private String name;

    private String processDefinitionId;

    private String processInstanceId;

    private TaskStatus status;

    private String parentTaskId;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public String getParentTaskId() {
        return parentTaskId;
    }
}

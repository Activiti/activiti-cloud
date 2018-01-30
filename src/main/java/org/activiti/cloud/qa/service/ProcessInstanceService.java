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

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.activiti.cloud.qa.Config;
import org.activiti.cloud.qa.client.ConnectorHelper;
import org.activiti.cloud.qa.model.AuthToken;
import org.activiti.cloud.qa.model.ProcessInstanceRequest;
import org.activiti.cloud.qa.model.ProcessInstanceResponse;
import org.activiti.cloud.qa.model.TaskAction;
import org.activiti.cloud.qa.model.TasksResponse;
import org.activiti.cloud.qa.serialization.Serializer;

public class ProcessInstanceService {

    public static ProcessInstanceResponse startProcess(AuthToken authToken) throws IOException {

        ProcessInstanceRequest processInstanceRequest = new ProcessInstanceRequest();
        processInstanceRequest.setCommandType("StartProcessInstanceCmd");
        processInstanceRequest.setProcessDefinitionKey("ProcessWithVariables");

        return Serializer.toProcessInstance(ConnectorHelper.postJson(Config.getInstance().getProperties().getProperty("rb.processinstance.url"), processInstanceRequest, authToken));


    }

    public static TasksResponse getTaskByProcessInstanceId(String processInstanceId,
                                                           AuthToken authToken) throws URISyntaxException, IOException {

        return Serializer.toTaskResponse(ConnectorHelper.get(Config.getInstance().getProperties().getProperty("rb.processinstance.url") + "/" + processInstanceId + "/tasks", null, authToken));
    }

    public static void changeTaskStatus(String id,
                                        String assignee,
                                        TaskAction taskAction,
                                        AuthToken authToken) throws IOException {

        Map<String,String> form = new HashMap<String, String>();
        if(taskAction.equals(TaskAction.CLAIM)){
            form.put("assignee", assignee);
        }

        Serializer.toAuthToken(ConnectorHelper.postForm(Config.getInstance().getProperties().getProperty("rb.task.url") + "/" + id + "/" + taskAction.getType(), form, authToken));
    }
}

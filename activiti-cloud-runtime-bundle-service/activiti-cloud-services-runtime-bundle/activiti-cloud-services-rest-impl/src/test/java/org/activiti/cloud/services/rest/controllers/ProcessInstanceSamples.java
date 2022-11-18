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
package org.activiti.cloud.services.rest.controllers;

import java.util.Date;
import java.util.UUID;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;

public class ProcessInstanceSamples {

    public static ProcessInstance defaultProcessInstance() {
        ProcessInstanceImpl processInstance = new ProcessInstanceImpl();
        processInstance.setId(UUID.randomUUID().toString());
        processInstance.setName("My process instance");
        processInstance.setProcessDefinitionId(UUID.randomUUID().toString());
        processInstance.setInitiator("user");
        processInstance.setStartDate(new Date());
        processInstance.setBusinessKey("my business key");
        processInstance.setStatus(ProcessInstance.ProcessInstanceStatus.RUNNING);
        processInstance.setProcessDefinitionKey("my-proc-def");

        return processInstance;
    }
}

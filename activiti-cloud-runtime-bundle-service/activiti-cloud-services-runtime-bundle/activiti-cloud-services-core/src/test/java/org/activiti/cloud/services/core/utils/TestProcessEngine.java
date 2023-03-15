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
package org.activiti.cloud.services.core.utils;

import java.io.IOException;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.runtime.ProcessInstance;

/**
 * Wrapper over ProcessEngine for testing purposes.
 * It helps to unify the calls to process engine services from tests.
 */
public class TestProcessEngine {

    private final ProcessEngine processEngine;

    public TestProcessEngine(final ProcessEngine processEngine) {
        this.processEngine = processEngine;
    }

    public Deployment deploy(final String processDefinitionKey) throws IOException {
        return processEngine
            .getRepositoryService()
            .createDeployment()
            .addClasspathResource(processDefinitionKey + ".bpmn20.xml")
            .deploy();
    }

    public ProcessInstance startProcessInstanceByKey(String processDefinitionKey) {
        return processEngine.getRuntimeService().startProcessInstanceByKey(processDefinitionKey);
    }

    public BpmnModel getBpmnModel(String processDefinitionId) {
        return processEngine.getRepositoryService().getBpmnModel(processDefinitionId);
    }
}

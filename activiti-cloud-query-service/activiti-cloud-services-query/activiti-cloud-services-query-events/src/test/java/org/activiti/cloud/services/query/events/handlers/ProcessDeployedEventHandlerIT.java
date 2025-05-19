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

package org.activiti.cloud.services.query.events.handlers;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import org.activiti.api.runtime.model.impl.ProcessDefinitionImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessDeployedEventImpl;
import org.activiti.cloud.services.query.model.ProcessDefinitionEntity;
import org.activiti.cloud.services.query.model.ProcessModelEntity;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@TestPropertySource("classpath:application-test.properties")
@SpringBootTest
@EnableAutoConfiguration
@Transactional
class ProcessDeployedEventHandlerIT {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ProcessDeployedEventHandler handler;

    //to be fixed in code
    @Disabled
    @Test
    void shouldPersistProcessDefinitionAndModelInDatabase() {
        // given
        String processId = "test-process-id";
        ProcessDefinitionImpl processDefinition = new ProcessDefinitionImpl();
        processDefinition.setId(processId);
        processDefinition.setName("Test Process");
        processDefinition.setKey("TEST-PROCESS");
        processDefinition.setVersion(1);

        CloudProcessDeployedEventImpl event = new CloudProcessDeployedEventImpl(processDefinition);
        event.setAppName("test-app");
        event.setAppVersion("1.0");
        event.setServiceName("test-service");
        event.setServiceFullName("test-service-full");
        event.setServiceVersion("1.0");
        event.setServiceType("default");
        event.setProcessModelContent("<?xml version=\"1.0\" ?><bpmn2:definitions />");

        // when
        handler.handle(event);
        entityManager.flush();
        entityManager.clear();

        // then
        ProcessDefinitionEntity storedProcessDef = entityManager.find(ProcessDefinitionEntity.class, processId);
        assertThat(storedProcessDef).isNotNull();
        assertThat(storedProcessDef.getId()).isEqualTo(processId);
        assertThat(storedProcessDef.getName()).isEqualTo("Test Process");
        assertThat(storedProcessDef.getKey()).isEqualTo("TEST-PROCESS");
        assertThat(storedProcessDef.getVersion()).isEqualTo(1);
        assertThat(storedProcessDef.getServiceName()).isEqualTo("test-service");
        assertThat(storedProcessDef.getAppName()).isEqualTo("test-app");

        ProcessModelEntity storedProcessModel = entityManager.find(ProcessModelEntity.class, processId);
        assertThat(storedProcessModel).isNotNull();
        assertThat(storedProcessModel.getProcessDefinition().getId()).isEqualTo(processId);
        assertThat(storedProcessModel.getProcessModelContent())
            .isEqualTo("<?xml version=\"1.0\" ?><bpmn2:definitions />");
    }
}

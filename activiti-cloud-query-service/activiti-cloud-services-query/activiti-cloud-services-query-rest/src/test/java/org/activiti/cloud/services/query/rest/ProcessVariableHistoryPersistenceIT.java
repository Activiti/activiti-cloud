/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.services.query.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.runtime.model.impl.VariableInstanceImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableCreatedEventImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableDeletedEventImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableUpdatedEventImpl;
import org.activiti.cloud.services.query.app.repository.ProcessVariableHistoryRepository;
import org.activiti.cloud.services.query.events.handlers.VariableCreatedEventHandler;
import org.activiti.cloud.services.query.events.handlers.VariableDeletedEventHandler;
import org.activiti.cloud.services.query.events.handlers.VariableUpdatedEventHandler;
import org.activiti.cloud.services.query.model.ProcessVariableHistoryEntity;
import org.activiti.cloud.services.query.util.QueryTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(
    properties = { "spring.main.banner-mode=off", "spring.jpa.properties.hibernate.enable_lazy_load_no_trans=false" }
)
@Testcontainers
@TestPropertySource("classpath:application-test.properties")
class ProcessVariableHistoryPersistenceIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine")
        .waitingFor(Wait.forListeningPort());

    @Autowired
    private VariableCreatedEventHandler variableCreatedEventHandler;

    @Autowired
    private VariableUpdatedEventHandler variableUpdatedEventHandler;

    @Autowired
    private VariableDeletedEventHandler variableDeletedEventHandler;

    @Autowired
    private ProcessVariableHistoryRepository historyRepository;

    @Autowired
    private QueryTestUtils queryTestUtils;

    @Test
    @Transactional
    void should_persistHistoryEntries_when_createUpdateDeleteLifecycle() {
        // given - a running process instance
        String processInstanceId = "proc-it-001";
        queryTestUtils
            .buildProcessInstance()
            .withId(processInstanceId)
            .withStatus(ProcessInstance.ProcessInstanceStatus.RUNNING)
            .buildAndSave();

        long baseTimestamp = System.currentTimeMillis();

        // when - variable is created
        VariableInstanceImpl<String> createVar = new VariableInstanceImpl<>(
            "myVar",
            "string",
            "initial",
            processInstanceId,
            null
        );
        CloudVariableCreatedEventImpl createEvent = new CloudVariableCreatedEventImpl("e1", baseTimestamp, createVar);
        variableCreatedEventHandler.handle(createEvent);

        // and then updated twice
        VariableInstanceImpl<String> updateVar1 = new VariableInstanceImpl<>(
            "myVar",
            "string",
            "second",
            processInstanceId,
            null
        );
        CloudVariableUpdatedEventImpl<String> updateEvent1 = new CloudVariableUpdatedEventImpl<>(
            "e2",
            baseTimestamp + 1000,
            updateVar1,
            "initial"
        );
        variableUpdatedEventHandler.handle(updateEvent1);

        VariableInstanceImpl<String> updateVar2 = new VariableInstanceImpl<>(
            "myVar",
            "string",
            "third",
            processInstanceId,
            null
        );
        CloudVariableUpdatedEventImpl<String> updateEvent2 = new CloudVariableUpdatedEventImpl<>(
            "e3",
            baseTimestamp + 2000,
            updateVar2,
            "second"
        );
        variableUpdatedEventHandler.handle(updateEvent2);

        // and then deleted
        VariableInstanceImpl<String> deleteVar = new VariableInstanceImpl<>(
            "myVar",
            "string",
            null,
            processInstanceId,
            null
        );
        CloudVariableDeletedEventImpl deleteEvent = new CloudVariableDeletedEventImpl(
            "e4",
            baseTimestamp + 3000,
            deleteVar
        );
        variableDeletedEventHandler.handle(deleteEvent);

        // then - history has 4 entries in order
        List<ProcessVariableHistoryEntity> history = historyRepository.findByProcessInstanceIdAndVariableNameOrderByCreateTimeAscSequenceNumberAsc(
            processInstanceId,
            "myVar"
        );

        assertThat(history).hasSize(4);

        ProcessVariableHistoryEntity entry0 = history.getFirst();
        assertThat(entry0.getVariableName()).isEqualTo("myVar");
        assertThat(entry0.getType()).isEqualTo("string");
        assertThat((String) entry0.getValue()).isEqualTo("initial");
        assertThat(entry0.isDeleted()).isFalse();
        assertThat(entry0.getCreateTime().getTime()).isEqualTo(baseTimestamp);

        ProcessVariableHistoryEntity entry1 = history.get(1);
        assertThat((String) entry1.getValue()).isEqualTo("second");
        assertThat(entry1.isDeleted()).isFalse();
        assertThat(entry1.getCreateTime().getTime()).isEqualTo(baseTimestamp + 1000);

        ProcessVariableHistoryEntity entry2 = history.get(2);
        assertThat((String) entry2.getValue()).isEqualTo("third");
        assertThat(entry2.isDeleted()).isFalse();
        assertThat(entry2.getCreateTime().getTime()).isEqualTo(baseTimestamp + 2000);

        ProcessVariableHistoryEntity entry3 = history.get(3);
        assertThat((Object) entry3.getValue()).isNull();
        assertThat(entry3.isDeleted()).isTrue();
        assertThat(entry3.getCreateTime().getTime()).isEqualTo(baseTimestamp + 3000);
    }
}

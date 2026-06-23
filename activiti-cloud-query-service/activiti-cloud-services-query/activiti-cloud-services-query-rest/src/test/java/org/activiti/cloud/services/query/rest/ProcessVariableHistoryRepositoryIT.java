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

import java.util.Date;
import java.util.UUID;
import org.activiti.QueryRestTestApplication;
import org.activiti.cloud.services.query.app.repository.ProcessVariableHistoryRepository;
import org.activiti.cloud.services.query.model.ProcessVariableHistoryEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(
    classes = QueryRestTestApplication.class,
    properties = { "spring.main.banner-mode=off", "spring.jpa.properties.hibernate.enable_lazy_load_no_trans=false" }
)
@TestPropertySource("classpath:application-test.properties")
@Testcontainers
class ProcessVariableHistoryRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine").waitingFor(
        Wait.forListeningPort()
    );

    @Autowired
    private ProcessVariableHistoryRepository historyRepository;

    @AfterEach
    void cleanUp() {
        historyRepository.deleteAll();
    }

    @Test
    void should_deleteRecordsOlderThanCutoff_andLeaveNewerOnes() {
        var processInstanceId = UUID.randomUUID().toString();
        historyRepository.save(buildHistoryEntity(processInstanceId, new Date(1000)));
        historyRepository.save(buildHistoryEntity(processInstanceId, new Date(2000)));
        historyRepository.save(buildHistoryEntity(processInstanceId, new Date(3000)));

        var deleted = historyRepository.deleteByRecordCreateTimeBefore(new Date(3000));

        assertThat(deleted).isEqualTo(2);
        assertThat(historyRepository.findAll())
            .hasSize(1)
            .first()
            .extracting(e -> e.getRecordCreateTime().getTime())
            .isEqualTo(3000L);
    }

    @Test
    void should_deleteNoRecords_when_allRecordsAreAfterCutoff() {
        var processInstanceId = UUID.randomUUID().toString();
        historyRepository.save(buildHistoryEntity(processInstanceId, new Date(5000)));
        historyRepository.save(buildHistoryEntity(processInstanceId, new Date(6000)));

        var deleted = historyRepository.deleteByRecordCreateTimeBefore(new Date(1000));

        assertThat(deleted).isZero();
        assertThat(historyRepository.count()).isEqualTo(2);
    }

    @Test
    void should_deleteAllRecords_when_allRecordsAreBeforeCutoff() {
        var processInstanceId = UUID.randomUUID().toString();
        historyRepository.save(buildHistoryEntity(processInstanceId, new Date(1000)));
        historyRepository.save(buildHistoryEntity(processInstanceId, new Date(2000)));

        var deleted = historyRepository.deleteByRecordCreateTimeBefore(new Date(9999));

        assertThat(deleted).isEqualTo(2);
        assertThat(historyRepository.count()).isZero();
    }

    private ProcessVariableHistoryEntity buildHistoryEntity(String processInstanceId, Date recordCreateTime) {
        var entity = new ProcessVariableHistoryEntity();
        entity.setProcessInstanceId(processInstanceId);
        entity.setVariableName("var");
        entity.setType(String.class.getName());
        entity.setValue("value");
        entity.setDeleted(false);
        entity.setEventTime(new Date(0L));
        entity.setRecordCreateTime(recordCreateTime);
        entity.setSequenceNumber(1);
        return entity;
    }
}

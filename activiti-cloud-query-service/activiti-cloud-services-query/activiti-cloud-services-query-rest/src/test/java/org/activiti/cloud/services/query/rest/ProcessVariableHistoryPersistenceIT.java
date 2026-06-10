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
import org.activiti.cloud.services.query.model.ProcessVariableHistoryEntity;
import org.activiti.cloud.services.query.util.QueryTestUtils;
import org.activiti.cloud.services.test.TestProducerAutoConfiguration;
import org.activiti.cloud.starters.test.MyProducer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(
    properties = { "spring.main.banner-mode=off", "spring.jpa.properties.hibernate.enable_lazy_load_no_trans=false" }
)
@TestPropertySource("classpath:application-test.properties")
@Import({ TestChannelBinderConfiguration.class, TestProducerAutoConfiguration.class })
class ProcessVariableHistoryPersistenceIT {

    @Autowired
    private MyProducer producer;

    @Autowired
    private ProcessVariableHistoryRepository historyRepository;

    @Autowired
    private QueryTestUtils queryTestUtils;

    @AfterEach
    void tearDown() {
        historyRepository.deleteAll();
    }

    @Test
    void should_persistHistoryEntries_when_createUpdateDeleteLifecycle() {
        // given - a running process instance
        String processInstanceId = "proc-it-001";
        queryTestUtils
            .buildProcessInstance()
            .withId(processInstanceId)
            .withStatus(ProcessInstance.ProcessInstanceStatus.RUNNING)
            .buildAndSave();

        long baseTimestamp = System.currentTimeMillis();

        // when - variable is created (each event is a separate message as in production)
        producer.send(
            new CloudVariableCreatedEventImpl(
                "e1",
                baseTimestamp,
                new VariableInstanceImpl<>("myVar", "string", "initial", processInstanceId, null)
            )
        );

        producer.send(
            new CloudVariableUpdatedEventImpl<>(
                "e2",
                baseTimestamp + 1000,
                new VariableInstanceImpl<>("myVar", "string", "second", processInstanceId, null),
                "initial"
            )
        );

        producer.send(
            new CloudVariableUpdatedEventImpl<>(
                "e3",
                baseTimestamp + 2000,
                new VariableInstanceImpl<>("myVar", "string", "third", processInstanceId, null),
                "second"
            )
        );

        producer.send(
            new CloudVariableDeletedEventImpl(
                "e4",
                baseTimestamp + 3000,
                new VariableInstanceImpl<>("myVar", "string", null, processInstanceId, null)
            )
        );

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
        assertThat(entry0.getMessageId()).isNotNull();
        assertThat(entry0.getSequenceNumber()).isEqualTo(0);

        ProcessVariableHistoryEntity entry1 = history.get(1);
        assertThat((String) entry1.getValue()).isEqualTo("second");
        assertThat(entry1.isDeleted()).isFalse();
        assertThat(entry1.getCreateTime().getTime()).isEqualTo(baseTimestamp + 1000);
        assertThat(entry1.getMessageId()).isNotNull();
        assertThat(entry1.getSequenceNumber()).isEqualTo(0);

        ProcessVariableHistoryEntity entry2 = history.get(2);
        assertThat((String) entry2.getValue()).isEqualTo("third");
        assertThat(entry2.isDeleted()).isFalse();
        assertThat(entry2.getCreateTime().getTime()).isEqualTo(baseTimestamp + 2000);
        assertThat(entry2.getMessageId()).isNotNull();
        assertThat(entry2.getSequenceNumber()).isEqualTo(0);

        ProcessVariableHistoryEntity entry3 = history.get(3);
        assertThat((Object) entry3.getValue()).isNull();
        assertThat(entry3.isDeleted()).isTrue();
        assertThat(entry3.getCreateTime().getTime()).isEqualTo(baseTimestamp + 3000);
        assertThat(entry3.getMessageId()).isNotNull();
        assertThat(entry3.getSequenceNumber()).isEqualTo(0);

        // each send() generates a distinct messageId UUID
        assertThat(history)
            .extracting(ProcessVariableHistoryEntity::getMessageId)
            .doesNotContainNull()
            .doesNotHaveDuplicates();
    }
}
